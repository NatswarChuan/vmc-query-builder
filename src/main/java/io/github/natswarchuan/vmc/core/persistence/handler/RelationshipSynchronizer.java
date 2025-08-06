package io.github.natswarchuan.vmc.core.persistence.handler;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Đồng bộ hóa trạng thái của các mối quan hệ (OneToMany, ManyToMany) giữa trạng thái trong bộ nhớ
 * và cơ sở dữ liệu.
 *
 * <p>Lớp này so sánh collection các thực thể liên quan trong đối tượng Java (trạng thái "mong
 * muốn") với các liên kết hiện có trong cơ sở dữ liệu (trạng thái "hiện tại"). Sau đó, nó thực hiện
 * các thao tác INSERT, UPDATE, hoặc DELETE cần thiết để làm cho trạng thái trong cơ sở dữ liệu khớp
 * với trạng thái trong bộ nhớ.
 *
 * @author NatswarChuan
 */
public class RelationshipSynchronizer {

  private GenericQueryExecutorMapper queryExecutor;
  private final VMCPersistenceManager persistenceManager;

  /**
   * Khởi tạo một instance mới của RelationshipSynchronizer.
   *
   * @param persistenceManager Trình quản lý persistence để xử lý việc lưu các thực thể liên quan
   *     một cách đệ quy.
   */
  public RelationshipSynchronizer(VMCPersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  /**
   * Lấy instance của {@code GenericQueryExecutorMapper} một cách lười biếng (lazy).
   *
   * @return instance của {@code GenericQueryExecutorMapper}.
   */
  private GenericQueryExecutorMapper getQueryExecutor() {
    if (this.queryExecutor == null) {
      this.queryExecutor = BeanUtil.getBean(GenericQueryExecutorMapper.class);
    }
    return this.queryExecutor;
  }

  /**
   * Đồng bộ hóa một mối quan hệ One-to-Many.
   *
   * <p>Phương thức này so sánh collection con "mong muốn" với các bản ghi con hiện có trong DB. Nó
   * sẽ:
   *
   * <ul>
   *   <li>Lưu tất cả các thực thể con trong collection "mong muốn".
   *   <li>Xác định các thực thể con cần được hủy liên kết (disassociated).
   *   <li>Khóa ngoại của các thực thể bị hủy liên kết sẽ được cập nhật thành NULL.
   * </ul>
   *
   * @param owner Thực thể cha (phía "one").
   * @param relMeta Metadata của mối quan hệ.
   * @param desiredCollection Collection các thực thể con "mong muốn".
   * @param options Các tùy chọn lưu, được truyền xuống cho các thao tác lưu đệ quy.
   * @param processedEntities Một map để theo dõi các thực thể đã được xử lý để tránh vòng lặp vô
   *     hạn.
   */
  public void synchronizeOneToMany(
      Model owner,
      RelationMetadata relMeta,
      Collection<?> desiredCollection,
      SaveOptions options,
      Map<Model, Model> processedEntities) {
    try {
      if (desiredCollection == null) {
        desiredCollection = Collections.emptyList();
      }

      Object ownerId = getPrimaryKeyValue(owner);
      if (ownerId == null || (ownerId instanceof Number && ((Number) ownerId).longValue() == 0)) {

        for (Object item : desiredCollection) {
          Model child = (Model) item;
          setInverseSide(child, owner, relMeta);
          persistenceManager.saveGraph(child, options, processedEntities);
        }
        return;
      }

      EntityMetadata childMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
      RelationMetadata inverseRelMeta = childMeta.getRelations().get(relMeta.getMappedBy());
      String fkColumn = inverseRelMeta.getJoinColumnName();

      String selectSql =
          String.format(
              "SELECT %s FROM %s WHERE %s = #{params.ownerId}",
              childMeta.getPrimaryKeyColumnName(), childMeta.getTableName(), fkColumn);
      List<Map<String, Object>> currentLinks =
          getQueryExecutor().execute(selectSql, Map.of("ownerId", ownerId));
      Set<Object> currentChildIds =
          currentLinks.stream()
              .map(row -> row.values().iterator().next())
              .collect(Collectors.toSet());

      Set<Object> desiredChildIds = new HashSet<>();
      for (Object item : desiredCollection) {
        Model child = (Model) item;
        setInverseSide(child, owner, relMeta);
        persistenceManager.saveGraph(child, options, processedEntities);
        desiredChildIds.add(getPrimaryKeyValue(child));
      }

      Set<Object> idsToDisassociate = new HashSet<>(currentChildIds);
      idsToDisassociate.removeAll(desiredChildIds);

      if (!idsToDisassociate.isEmpty()) {
        String updateSql =
            String.format(
                "UPDATE %s SET %s = NULL WHERE %s IN (%s)",
                childMeta.getTableName(),
                fkColumn,
                childMeta.getPrimaryKeyColumnName(),
                getInClauseValues(idsToDisassociate));
        getQueryExecutor().update(updateSql, Collections.emptyMap());
      }
    } catch (Exception e) {

      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to synchronize One-to-Many relationship for " + relMeta.getFieldName(),
          e);
    }
  }

  /**
   * Đồng bộ hóa một mối quan hệ Many-to-Many.
   *
   * <p>Phương thức này quản lý các bản ghi trong bảng trung gian (join table). Nó sẽ:
   *
   * <ul>
   *   <li>Lưu tất cả các thực thể trong collection "mong muốn".
   *   <li>So sánh các liên kết hiện tại trong bảng trung gian với các liên kết "mong muốn".
   *   <li>Xóa các liên kết không còn tồn tại.
   *   <li>Thêm các liên kết mới.
   * </ul>
   *
   * @param owner Thực thể sở hữu.
   * @param relMeta Metadata của mối quan hệ.
   * @param relatedCollection Collection các thực thể liên quan "mong muốn".
   * @param options Các tùy chọn lưu.
   * @param processedEntities Một map để theo dõi các thực thể đã được xử lý.
   */
  public void synchronizeManyToMany(
      Model owner,
      RelationMetadata relMeta,
      Collection<?> relatedCollection,
      SaveOptions options,
      Map<Model, Model> processedEntities) {
    try {
      if (relatedCollection == null) {
        relatedCollection = Collections.emptySet();
      }

      for (Object item : relatedCollection) {
        persistenceManager.saveGraph((Model) item, options, processedEntities);
      }

      JoinTableMetadata joinTable = relMeta.getJoinTable();
      Object ownerId = getPrimaryKeyValue(owner);

      String selectSql =
          String.format(
              "SELECT %s FROM %s WHERE %s = #{params.ownerId}",
              joinTable.getInverseJoinColumn(),
              joinTable.getTableName(),
              joinTable.getJoinColumn());
      List<Map<String, Object>> currentLinks =
          getQueryExecutor().execute(selectSql, Map.of("ownerId", ownerId));
      Set<Object> currentRelatedIds =
          currentLinks.stream()
              .map(row -> row.values().iterator().next())
              .collect(Collectors.toSet());

      Set<Object> desiredRelatedIds = new HashSet<>();
      for (Object item : relatedCollection) {
        desiredRelatedIds.add(getPrimaryKeyValue((Model) item));
      }

      Set<Object> idsToAdd = new HashSet<>(desiredRelatedIds);
      idsToAdd.removeAll(currentRelatedIds);

      Set<Object> idsToRemove = new HashSet<>(currentRelatedIds);
      idsToRemove.removeAll(desiredRelatedIds);

      if (!idsToRemove.isEmpty()) {
        String deleteSql =
            String.format(
                "DELETE FROM %s WHERE %s = #{params.ownerId} AND %s IN (%s)",
                joinTable.getTableName(),
                joinTable.getJoinColumn(),
                joinTable.getInverseJoinColumn(),
                getInClauseValues(idsToRemove));
        getQueryExecutor().delete(deleteSql, Map.of("ownerId", ownerId));
      }

      if (!idsToAdd.isEmpty()) {

        StringBuilder valuesClause = new StringBuilder();
        Map<String, Object> batchParams = new HashMap<>();
        batchParams.put("ownerId", ownerId);
        int i = 0;
        for (Object childId : idsToAdd) {
          if (i > 0) {
            valuesClause.append(", ");
          }
          String paramName = "childId" + i;
          valuesClause.append("(:ownerId, :").append(paramName).append(")");
          batchParams.put(paramName, childId);
          i++;
        }

        String insertSql =
            String.format(
                "INSERT INTO %s (%s, %s) VALUES %s",
                joinTable.getTableName(),
                joinTable.getJoinColumn(),
                joinTable.getInverseJoinColumn(),
                valuesClause.toString());

        String myBatisSql = insertSql.replaceAll(":(\\w+)", "#{params.$1}");

        getQueryExecutor().insert(myBatisSql, batchParams);
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to synchronize Many-to-Many relationship for " + relMeta.getFieldName(),
          e);
    }
  }

  /**
   * Chuyển đổi một {@code Set} các ID thành một chuỗi để sử dụng trong mệnh đề SQL IN.
   *
   * @param ids Tập hợp các ID.
   * @return Một chuỗi được định dạng, ví dụ: "1,2,3" hoặc "'a','b','c'".
   */
  private String getInClauseValues(Set<Object> ids) {
    return ids.stream()
        .map(
            id -> {
              if (id instanceof Number) {
                return id.toString();
              } else {
                return "'" + id.toString().replace("'", "''") + "'";
              }
            })
        .collect(Collectors.joining(","));
  }

  /**
   * Thiết lập tham chiếu ngược từ thực thể con đến thực thể cha.
   *
   * @param child Thực thể con.
   * @param parent Thực thể cha.
   * @param parentRelMeta Metadata của mối quan hệ từ phía cha.
   * @throws Exception nếu có lỗi reflection.
   */
  private void setInverseSide(Model child, Model parent, RelationMetadata parentRelMeta)
      throws Exception {
    String mappedByField = parentRelMeta.getMappedBy();
    if (mappedByField != null && !mappedByField.isEmpty()) {
      Field inverseField = findField(child.getClass(), mappedByField);
      inverseField.setAccessible(true);
      inverseField.set(child, parent);
    }
  }

  /**
   * Lấy giá trị khóa chính của một thực thể.
   *
   * @param model Thực thể cần lấy khóa chính.
   * @return Giá trị của khóa chính.
   * @throws Exception nếu có lỗi reflection.
   */
  private Object getPrimaryKeyValue(Model model) throws Exception {
    if (model == null) {
      return null;
    }
    EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));
    Field pkField = findField(model.getClass(), metadata.getPrimaryKeyFieldName());
    pkField.setAccessible(true);
    return pkField.get(model);
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó.
   *
   * @param clazz Lớp bắt đầu tìm kiếm.
   * @param fieldName Tên của trường cần tìm.
   * @return Đối tượng {@code Field} nếu tìm thấy.
   * @throws VMCException nếu không tìm thấy trường.
   */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new VMCException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }

  /**
   * Lấy lớp thực sự của một đối tượng có thể là proxy của CGLIB.
   *
   * @param clazz Lớp cần kiểm tra.
   * @return Lớp cha nếu là proxy, ngược lại trả về chính nó.
   */
  private Class<?> getUnproxiedClass(Class<?> clazz) {
    if (clazz.getName().contains("$$EnhancerByCGLIB$$")) {
      return clazz.getSuperclass();
    }
    return clazz;
  }
}
