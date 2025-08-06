package io.github.natswarchuan.vmc.core.query.helper;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.lazy.*;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.http.HttpStatus;

/**
 * Ánh xạ kết quả truy vấn thô từ cơ sở dữ liệu thành các đồ thị đối tượng thực thể hoặc DTO.
 *
 * <p>Lớp này đóng vai trò trung tâm trong việc chuyển đổi dữ liệu. Nó nhận một danh sách các hàng
 * dữ liệu phẳng (thường là kết quả của một truy vấn có JOIN) và tái cấu trúc chúng thành một danh
 * sách các đối tượng thực thể có cấu trúc lồng nhau. Nó xử lý các logic phức tạp như:
 *
 * <ul>
 *   <li>Tạo và quản lý các thực thể trong một "session cache" để tránh trùng lặp đối tượng.
 *   <li>Liên kết các thực thể với nhau dựa trên thông tin về mối quan hệ (eager loading).
 *   <li>Thiết lập các proxy cho việc tải lười (lazy loading) các mối quan hệ.
 *   <li>Chuyển đổi các thực thể đã được xây dựng thành các đối tượng truyền dữ liệu (DTO).
 * </ul>
 *
 * @author NatswarChuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryResultMapper {

  private final Class<? extends Model> modelClass;
  private final String fromAlias;
  private final List<String> withRelations;

  /**
   * Khởi tạo một instance mới của QueryResultMapper.
   *
   * @param modelClass Lớp thực thể gốc mà bộ ánh xạ này sẽ làm việc.
   * @param fromAlias Bí danh của bảng gốc trong câu lệnh SQL.
   * @param withRelations Danh sách các mối quan hệ cần được tải ngay lập tức (eager loading).
   */
  public QueryResultMapper(
      Class<? extends Model> modelClass, String fromAlias, List<String> withRelations) {
    this.modelClass = modelClass;
    this.fromAlias = fromAlias;
    this.withRelations = withRelations;
  }

  /**
   * Xử lý một danh sách kết quả phẳng và xây dựng lại đồ thị đối tượng.
   *
   * <p>Đây là phương thức chính của lớp. Nó duyệt qua từng hàng kết quả, tạo hoặc lấy các thực thể
   * từ một cache tạm thời (session cache) để tránh tạo đối tượng trùng lặp cho cùng một bản ghi, và
   * sau đó liên kết chúng lại với nhau dựa trên các mệnh đề JOIN đã được định nghĩa.
   *
   * @param flatResults Một {@code List<Map<String, Object>>} đại diện cho kết quả thô từ cơ sở dữ
   *     liệu.
   * @param joinClauses Danh sách các mệnh đề JOIN đã được sử dụng trong truy vấn, chứa thông tin về
   *     các mối quan hệ.
   * @param <T> Kiểu của thực thể gốc.
   * @return Một danh sách các thực thể gốc đã được xây dựng hoàn chỉnh, với các mối quan hệ đã được
   *     liên kết.
   */
  public <T extends Model> List<T> processFlatResults(
      List<Map<String, Object>> flatResults, List<JoinClause> joinClauses) {
    if (flatResults.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Class<?>, Map<Object, Model>> sessionCache = new HashMap<>();

    Map<Object, T> mainModelsMap = new LinkedHashMap<>();

    for (Map<String, Object> row : flatResults) {
      T mainModel = (T) getOrCreateEntity(modelClass, fromAlias, row, sessionCache);
      if (mainModel == null) {
        continue;
      }
      mainModelsMap.put(mainModel.getPrimaryKey(), mainModel);

      for (JoinClause join : joinClauses) {
        if (join.getRelatedClass() == null) continue;

        Model relatedModel =
            getOrCreateEntity(join.getRelatedClass(), join.getAlias(), row, sessionCache);
        if (relatedModel == null) continue;

        linkEntities(mainModel, relatedModel, join.getRelationName());
      }
    }
    return new ArrayList<>(mainModelsMap.values());
  }

  /**
   * Lấy hoặc tạo một thực thể từ cache trong phiên xử lý.
   *
   * <p>Phương thức này kiểm tra xem một thực thể với khóa chính cụ thể đã tồn tại trong cache chưa.
   * Nếu có, nó sẽ trả về thực thể đó. Nếu không, nó sẽ tạo một thực thể mới bằng cách gọi {@link
   * #mapRowToModel(Class, Map, String)}, đưa vào cache, và sau đó trả về.
   *
   * @param entityClass Lớp của thực thể cần lấy hoặc tạo.
   * @param alias Bí danh của bảng tương ứng trong câu lệnh SQL.
   * @param row Dữ liệu của một hàng từ cơ sở dữ liệu.
   * @param sessionCache Cache của phiên xử lý để lưu trữ các thực thể đã tạo.
   * @return Thực thể đã tồn tại hoặc vừa được tạo. Trả về {@code null} nếu không có khóa chính.
   */
  private Model getOrCreateEntity(
      Class<? extends Model> entityClass,
      String alias,
      Map<String, Object> row,
      Map<Class<?>, Map<Object, Model>> sessionCache) {
    EntityMetadata metadata = MetadataCache.getMetadata(entityClass);
    String pkAlias = alias + "_" + metadata.getPrimaryKeyColumnName();
    Object pkValue = row.get(pkAlias);

    if (pkValue == null) {
      return null;
    }

    sessionCache.computeIfAbsent(entityClass, k -> new HashMap<>());
    Map<Object, Model> entityCache = sessionCache.get(entityClass);

    return entityCache.computeIfAbsent(
        pkValue,
        k -> {
          Model newInstance = mapRowToModel(entityClass, row, alias);
          if (newInstance != null) {
            newInstance.setPrimaryKey(pkValue);
          }
          return newInstance;
        });
  }

  /**
   * Liên kết một thực thể quan hệ (related) vào thực thể sở hữu (owner).
   *
   * @param owner Thực thể sở hữu mối quan hệ.
   * @param related Thực thể liên quan cần được liên kết vào.
   * @param relationName Tên của trường (field) đại diện cho mối quan hệ trên thực thể sở hữu.
   * @throws VMCException nếu không thể truy cập hoặc gán giá trị cho trường quan hệ.
   */
  private void linkEntities(Model owner, Model related, String relationName) {
    try {
      Field relationField = findField(owner.getClass(), relationName);
      relationField.setAccessible(true);
      RelationMetadata relMeta =
          MetadataCache.getMetadata(owner.getClass()).getRelations().get(relationName);

      if (relMeta.isCollection()) {
        Collection<Model> collection = (Collection<Model>) relationField.get(owner);

        Set<Object> pksInCollection =
            collection.stream().map(Model::getPrimaryKey).collect(Collectors.toSet());
        if (!pksInCollection.contains(related.getPrimaryKey())) {
          collection.add(related);
          setBackReference(owner, related, relationName);
        }
      } else {
        if (relationField.get(owner) == null) {
          relationField.set(owner, related);
          setBackReference(owner, related, relationName);
        }
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not link entities for relation: " + relationName,
          e);
    }
  }

  /**
   * Thiết lập tham chiếu ngược từ thực thể liên quan (related) trở lại thực thể sở hữu (owner).
   *
   * <p>Đối với các mối quan hệ hai chiều (bidirectional), sau khi liên kết từ A đến B, cần phải
   * thiết lập liên kết ngược từ B trở lại A để đảm bảo tính nhất quán của đồ thị đối tượng.
   *
   * @param owner Thực thể sở hữu.
   * @param related Thực thể liên quan.
   * @param ownerRelationName Tên của trường quan hệ ở phía owner đã được liên kết.
   */
  private void setBackReference(Model owner, Model related, String ownerRelationName) {
    try {
      EntityMetadata relatedMeta = MetadataCache.getMetadata(related.getClass());

      for (RelationMetadata backRelMeta : relatedMeta.getRelations().values()) {
        if (ownerRelationName.equals(backRelMeta.getMappedBy())) {
          Field backRefField = findField(related.getClass(), backRelMeta.getFieldName());
          backRefField.setAccessible(true);

          if (backRelMeta.isCollection()) {
            Collection<Model> collection = (Collection<Model>) backRefField.get(related);
            if (collection != null) {
              Set<Object> pksInCollection =
                  collection.stream().map(Model::getPrimaryKey).collect(Collectors.toSet());
              if (!pksInCollection.contains(owner.getPrimaryKey())) {
                collection.add(owner);
              }
            }
          } else {
            if (backRefField.get(related) == null) {
              backRefField.set(related, owner);
            }
          }
          return;
        }
      }
    } catch (Exception e) {

    }
  }

  /**
   * Ánh xạ một hàng kết quả (dưới dạng Map) thành một instance của một lớp Model.
   *
   * @param modelClass Lớp cụ thể của model cần tạo.
   * @param row Dữ liệu của một hàng từ cơ sở dữ liệu.
   * @param alias Bí danh (alias) của bảng trong câu lệnh SQL, dùng để trích xuất đúng cột.
   * @return Một instance của model đã được điền dữ liệu, hoặc {@code null} nếu hàng không chứa dữ
   *     liệu cho model này.
   */
  public Model mapRowToModel(
      Class<? extends Model> modelClass, Map<String, Object> row, String alias) {
    try {
      Map<String, Object> modelAttributes = new HashMap<>();
      boolean hasData = false;

      String prefix = (alias != null) ? alias + "_" : "";
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        if (alias == null || entry.getKey().startsWith(prefix)) {
          String columnName =
              (alias != null) ? entry.getKey().substring(prefix.length()) : entry.getKey();
          modelAttributes.put(columnName, entry.getValue());
          if (entry.getValue() != null) {
            hasData = true;
          }
        }
      }

      if (!hasData) return null;

      Model modelInstance = modelClass.getDeclaredConstructor().newInstance();
      modelInstance.setAttributes(modelAttributes);
      setupRelations(modelInstance);
      return modelInstance;
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not instantiate and map model: " + modelClass.getSimpleName(),
          e);
    }
  }

  /**
   * Thiết lập các trường quan hệ cho một instance thực thể đã được tạo.
   *
   * <p>Phương thức này quyết định xem một mối quan hệ nên được tải ngay lập tức (eager) hay tải
   * lười (lazy) dựa trên danh sách {@code withRelations} đã được cung cấp.
   *
   * @param modelInstance Instance của thực thể cần thiết lập quan hệ.
   */
  private void setupRelations(Model modelInstance) {
    EntityMetadata metadata = MetadataCache.getMetadata(modelInstance.getClass());
    for (RelationMetadata relMeta : metadata.getRelations().values()) {
      try {
        Field field = findField(modelInstance.getClass(), relMeta.getFieldName());
        field.setAccessible(true);
        boolean isEager = this.withRelations.contains(relMeta.getFieldName());

        if (relMeta.isCollection()) {
          if (isEager) {
            if (List.class.isAssignableFrom(field.getType())) {
              field.set(modelInstance, new ArrayList<>());
            } else if (Set.class.isAssignableFrom(field.getType())) {
              field.set(modelInstance, new HashSet<>());
            }
          } else {
            LazyLoader<?> loader = createLoader(modelInstance, relMeta);
            if (List.class.isAssignableFrom(field.getType())) {
              field.set(modelInstance, new LazyLoadingList<>(loader));
            } else if (Set.class.isAssignableFrom(field.getType())) {
              field.set(modelInstance, new LazyLoadingSet<>(loader));
            }
          }
        } else {
          if (!isEager) {
            Object fkValue = null;
            String pkColumn = null;
            String excludedRelationName = null;
            EntityMetadata targetMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());

            if (relMeta.isOwningSideOfAssociation()) {
              fkValue = modelInstance.getAttribute(relMeta.getJoinColumnName());
              pkColumn = targetMeta.getPrimaryKeyColumnName();
              excludedRelationName =
                  targetMeta.getRelations().values().stream()
                      .filter(r -> relMeta.getFieldName().equals(r.getMappedBy()))
                      .map(RelationMetadata::getFieldName)
                      .findFirst()
                      .orElse(null);
            } else if (relMeta.isInverseSide()) {
              Object pkValue = modelInstance.getAttribute(metadata.getPrimaryKeyColumnName());
              if (pkValue != null) {
                RelationMetadata owningRel = targetMeta.getRelations().get(relMeta.getMappedBy());
                if (owningRel != null && owningRel.getJoinColumnName() != null) {
                  pkColumn = owningRel.getJoinColumnName();
                  fkValue = pkValue;
                  excludedRelationName = relMeta.getMappedBy();
                }
              }
            }

            if (fkValue != null) {
              Object proxy =
                  createLazyProxy(
                      relMeta.getTargetEntity(), pkColumn, fkValue, excludedRelationName);
              field.set(modelInstance, proxy);
            }
          }
        }
      } catch (Exception e) {
        throw new VMCException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Could not set up relation for " + relMeta.getFieldName(),
            e);
      }
    }
  }

  /**
   * Tạo một đối tượng proxy CGLIB cho các mối quan hệ ToOne để thực hiện tải lười.
   *
   * @param targetClass Lớp của thực thể liên quan cần tạo proxy.
   * @param pkColumn Tên cột dùng để truy vấn (thường là khóa chính hoặc khóa ngoại).
   * @param pkValue Giá trị dùng để truy vấn.
   * @param excludedRelationName Tên của mối quan hệ ngược lại cần loại trừ để tránh đệ quy vô hạn.
   * @return Một đối tượng proxy của lớp đích.
   */
  private Object createLazyProxy(
      Class<?> targetClass, String pkColumn, Object pkValue, String excludedRelationName) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(targetClass);
    enhancer.setCallback(
        new LazyLoadInterceptor(
            (Class<? extends Model>) targetClass, pkColumn, pkValue, excludedRelationName));
    return enhancer.create();
  }

  /**
   * Tạo một {@link LazyLoader} phù hợp cho một mối quan hệ dạng collection.
   *
   * @param modelInstance Thực thể chứa mối quan hệ.
   * @param relMeta Metadata của mối quan hệ.
   * @return Một instance của {@link OneToManyLoader} hoặc {@link ManyToManyLoader}.
   * @throws VMCException nếu loại quan hệ không được hỗ trợ.
   */
  private LazyLoader<?> createLoader(Model modelInstance, RelationMetadata relMeta) {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY)
      return new OneToManyLoader(modelInstance, relMeta);
    if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY)
      return new ManyToManyLoader(modelInstance, relMeta);
    throw new VMCException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Cannot create loader for relation type: " + relMeta.getType());
  }

  /**
   * Ánh xạ một danh sách các thực thể sang một danh sách các DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param entities Danh sách các thực thể cần ánh xạ.
   * @param dtoClass Lớp của DTO đích.
   * @return Một danh sách các DTO đã được điền dữ liệu.
   */
  public <D> List<D> mapEntitiesToDtos(List<? extends Model> entities, Class<D> dtoClass) {
    return entities.stream()
        .map(entity -> mapEntityToDto(entity, dtoClass))
        .collect(Collectors.toList());
  }

  /**
   * Ánh xạ một thực thể đơn lẻ sang một DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param entity Thực thể cần ánh xạ.
   * @param dtoClass Lớp của DTO đích.
   * @return Một instance DTO đã được điền dữ liệu, hoặc {@code null} nếu entity đầu vào là null.
   * @throws VMCException nếu lớp DTO không triển khai {@link BaseDto} hoặc có lỗi khi ánh xạ.
   */
  private <D> D mapEntityToDto(Model entity, Class<D> dtoClass) {
    if (entity == null) {
      return null;
    }
    try {
      D dtoInstance = dtoClass.getDeclaredConstructor().newInstance();
      if (dtoInstance instanceof BaseDto) {
        return (D) ((BaseDto) dtoInstance).toDto(entity);
      } else {
        throw new VMCException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "DTO class " + dtoClass.getSimpleName() + " must implement BaseDto.");
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not map entity "
              + entity.getClass().getSimpleName()
              + " to DTO "
              + dtoClass.getSimpleName(),
          e);
    }
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó bằng reflection.
   *
   * @param clazz Lớp bắt đầu tìm kiếm.
   * @param fieldName Tên của trường cần tìm.
   * @return Đối tượng {@code Field} nếu tìm thấy.
   * @throws NoSuchFieldException nếu không tìm thấy trường.
   */
  private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
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
   * Xây dựng một cấu trúc cây từ một danh sách phẳng các thực thể tự tham chiếu.
   *
   * <p>Phương thức này được sử dụng để xử lý các mối quan hệ đệ quy, ví dụ như một cây danh mục,
   * trong đó mỗi mục có thể có một mục cha và nhiều mục con.
   *
   * @param <T> Kiểu của thực thể.
   * @param flatList Danh sách phẳng chứa tất cả các nút trong cây.
   * @param childRelation Metadata của mối quan hệ "con" (ví dụ: trường {@code List<Category>
   *     children}).
   * @return Một danh sách chỉ chứa các nút gốc của cây, với các nút con đã được lồng vào đúng vị
   *     trí.
   * @throws VMCException nếu có lỗi xảy ra trong quá trình xây dựng cây.
   */
  public <T extends Model> List<T> buildTree(List<T> flatList, RelationMetadata childRelation) {
    if (flatList == null || flatList.isEmpty()) {
      return Collections.emptyList();
    }

    try {
      EntityMetadata metadata = MetadataCache.getMetadata(modelClass);
      String pkName = metadata.getPrimaryKeyFieldName();

      RelationMetadata parentRelation =
          metadata.getRelations().values().stream()
              .filter(r -> childRelation.getMappedBy().equals(r.getFieldName()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new VMCException(
                          HttpStatus.INTERNAL_SERVER_ERROR,
                          "Recursive relationship misconfigured."));

      Field childField = findField(modelClass, childRelation.getFieldName());
      childField.setAccessible(true);
      Field pkField = findField(modelClass, pkName);
      pkField.setAccessible(true);

      Map<Object, T> map = new HashMap<>();
      for (T item : flatList) {
        map.put(pkField.get(item), item);
      }

      List<T> rootNodes = new ArrayList<>();
      for (T item : flatList) {
        Object parentId = item.getAttribute(parentRelation.getJoinColumnName());
        if (parentId == null) {
          rootNodes.add(item);
        } else {
          T parentFromMap = map.get(parentId);
          if (parentFromMap != null) {

            ((List<T>) childField.get(parentFromMap)).add(item);
          } else {

            rootNodes.add(item);
          }
        }
      }
      return rootNodes;

    } catch (Exception e) {
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build entity tree", e);
    }
  }
}
