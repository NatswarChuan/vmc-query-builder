package io.github.natswarchuan.vmc.core.persistence.handler;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Xử lý logic xóa theo tầng (cascade remove) và dọn dẹp các mối quan hệ khi một thực thể bị xóa.
 *
 * <p>Lớp này chịu trách nhiệm thực hiện các hành động cần thiết trên các thực thể liên quan (ví dụ:
 * xóa bản ghi con, cập nhật khóa ngoại thành NULL, xóa các bản ghi trong bảng trung gian) trước khi
 * thực thể chính bị xóa khỏi cơ sở dữ liệu.
 *
 * @author NatswarChuan
 */
public class CascadeRemoveHandler {

  private GenericQueryExecutorMapper queryExecutor;

  /** Khởi tạo một instance mới của CascadeRemoveHandler. */
  public CascadeRemoveHandler() {}

  /**
   * Lấy instance của {@code GenericQueryExecutorMapper} một cách lười biếng (lazy).
   *
   * <p>Phương thức này đảm bảo rằng bean chỉ được lấy từ Spring context khi cần thiết lần đầu tiên.
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
   * Xử lý tất cả các hành động cascade cho một thực thể sắp bị xóa.
   *
   * <p>Phương thức này duyệt qua tất cả các mối quan hệ của thực thể và gọi các phương thức xử lý
   * tương ứng dựa trên loại quan hệ (One-to-Many, One-to-One, Many-to-Many).
   *
   * @param model Thực thể sắp bị xóa.
   * @throws Exception nếu có lỗi xảy ra trong quá trình xử lý.
   */
  public void handleCascades(Model model, RemoveOptions options) throws Exception {
    EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));

    Set<String> relationsToCascade = options.getRelationsToCascade();

    for (String relationName : relationsToCascade) {
      RelationMetadata relMeta = metadata.getRelations().get(relationName);
      if (relMeta == null) continue;

      switch (relMeta.getType()) {
        case ONE_TO_MANY:
          handleOneToManyOnRemove(model, relMeta, metadata);
          break;
        case ONE_TO_ONE:
          if (relMeta.isInverseSide()) {
            handleOneToOneInverseOnRemove(model, relMeta, metadata);
          }
          break;
        case MANY_TO_MANY:
          if (relMeta.isOwningSide()) {
            handleManyToManyOnRemove(model, relMeta, metadata);
          }
          break;
        default:
          break;
      }
    }
  }

  /**
   * Xử lý logic xóa cho mối quan hệ One-to-Many.
   *
   * <p>Nếu mối quan hệ được đánh dấu là {@code orphanRemoval}, các thực thể con sẽ bị xóa. Ngược
   * lại, khóa ngoại của chúng sẽ được cập nhật thành {@code NULL}.
   *
   * @param owner Thực thể cha (phía "one").
   * @param relMeta Metadata của mối quan hệ.
   * @param ownerMeta Metadata của thực thể cha.
   * @throws Exception nếu có lỗi xảy ra.
   */
  private void handleOneToManyOnRemove(
      Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta) throws Exception {
    EntityMetadata childMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
    RelationMetadata inverseRelMeta = childMeta.getRelations().get(relMeta.getMappedBy());
    String fkColumn = inverseRelMeta.getJoinColumnName();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);

    String selectChildrenSql =
        String.format(
            "SELECT %s FROM %s WHERE %s = #{params.ownerId}",
            childMeta.getPrimaryKeyColumnName(), childMeta.getTableName(), fkColumn);
    List<Map<String, Object>> children =
        getQueryExecutor().execute(selectChildrenSql, Map.of("ownerId", ownerId));

    if (!children.isEmpty()) {
      List<Object> childIds =
          children.stream().map(c -> c.values().iterator().next()).collect(Collectors.toList());
      String deleteChildrenSql =
          String.format(
              "DELETE FROM %s WHERE %s IN (%s)",
              childMeta.getTableName(),
              childMeta.getPrimaryKeyColumnName(),
              childIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
      getQueryExecutor().delete(deleteChildrenSql, Collections.emptyMap());
    }
  }

  /**
   * Xử lý logic xóa cho phía nghịch đảo (inverse side) của mối quan hệ One-to-One.
   *
   * @param owner Thực thể sắp bị xóa.
   * @param relMeta Metadata của mối quan hệ.
   * @param ownerMeta Metadata của thực thể sắp bị xóa.
   * @throws Exception nếu có lỗi xảy ra.
   */
  private void handleOneToOneInverseOnRemove(
      Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta) throws Exception {
    EntityMetadata relatedMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
    RelationMetadata inverseRelMeta = relatedMeta.getRelations().get(relMeta.getMappedBy());
    String fkColumn = inverseRelMeta.getJoinColumnName();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);

    String deleteSql =
        String.format(
            "DELETE FROM %s WHERE %s = #{params.ownerId}", relatedMeta.getTableName(), fkColumn);
    getQueryExecutor().delete(deleteSql, Map.of("ownerId", ownerId));
  }

  /**
   * Xử lý logic xóa cho mối quan hệ Many-to-Many.
   *
   * <p>Phương thức này sẽ xóa tất cả các bản ghi liên quan đến thực thể đang bị xóa khỏi bảng trung
   * gian (join table).
   *
   * @param owner Thực thể sắp bị xóa.
   * @param relMeta Metadata của mối quan hệ.
   * @param ownerMeta Metadata của thực thể sắp bị xóa.
   * @throws Exception nếu có lỗi xảy ra.
   */
  private void handleManyToManyOnRemove(
      Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta) throws Exception {
    JoinTableMetadata joinTable = relMeta.getJoinTable();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);
    String deleteSql =
        String.format(
            "DELETE FROM %s WHERE %s = #{params.ownerId}",
            joinTable.getTableName(), joinTable.getJoinColumn());
    getQueryExecutor().delete(deleteSql, Map.of("ownerId", ownerId));
  }

  /**
   * Lấy giá trị khóa chính của một thực thể bằng reflection.
   *
   * @param model Thực thể cần lấy khóa chính.
   * @param metadata Metadata của thực thể.
   * @return Giá trị của khóa chính.
   * @throws Exception nếu có lỗi khi truy cập trường.
   */
  private Object getPrimaryKeyValue(Model model, EntityMetadata metadata) throws Exception {
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
