package io.github.natswarchuan.vmc.core.query.builder.helper;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.lazy.*;
import io.github.natswarchuan.vmc.core.query.builder.AbstractQueryBuilder;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.http.HttpStatus;

/**
 * Xử lý kết quả thô từ cơ sở dữ liệu và ánh xạ chúng thành các đồ thị đối tượng thực thể hoặc DTO.
 *
 * <p>Lớp này chịu trách nhiệm biến đổi một danh sách phẳng các hàng dữ liệu (thường là kết quả của
 * một truy vấn JOIN) thành một danh sách các đối tượng thực thể đã được cấu trúc, với các mối quan
 * hệ (cả eager và lazy) được thiết lập đúng cách.
 *
 * @param <T> Kiểu của thực thể gốc mà bộ xử lý này làm việc.
 * @author NatswarChuan
 */
public class ResultProcessor<T extends Model> {

  private final AbstractQueryBuilder<?> builder;
  private final Class<T> modelClass;

  /**
   * Khởi tạo một instance mới của ResultProcessor.
   *
   * @param builder Instance của query builder chứa trạng thái của truy vấn (ví dụ: các mối quan hệ
   *     cần eager load).
   */
  @SuppressWarnings("unchecked")
  public ResultProcessor(AbstractQueryBuilder<?> builder) {
    this.builder = builder;
    this.modelClass = (Class<T>) builder.getModelClass();
  }

  /**
   * Xử lý một danh sách kết quả phẳng từ cơ sở dữ liệu và xây dựng lại đồ thị đối tượng.
   *
   * <p>Phương thức này duyệt qua từng hàng kết quả, tạo hoặc lấy các thực thể từ một cache tạm thời
   * (session cache) để tránh tạo đối tượng trùng lặp, và sau đó liên kết chúng lại với nhau dựa
   * trên các mệnh đề JOIN đã được định nghĩa.
   *
   * @param flatResults Một {@code List<Map<String, Object>>} đại diện cho kết quả thô từ cơ sở dữ
   *     liệu.
   * @return Một danh sách các thực thể gốc đã được xây dựng hoàn chỉnh, với các mối quan hệ đã được
   *     liên kết.
   */
  public List<T> process(List<Map<String, Object>> flatResults) {
    Map<Object, T> mainModelsMap = new LinkedHashMap<>();
    EntityMetadata mainMetadata = MetadataCache.getMetadata(modelClass);
    String mainPrimaryKeyAlias =
        builder.getFromAlias() + "_" + mainMetadata.getPrimaryKeyColumnName();

    for (Map<String, Object> row : flatResults) {
      Object mainModelPkValue = row.get(mainPrimaryKeyAlias);

      if (mainModelPkValue == null) {
        T item = mapRowToModel(modelClass, row, null);
        if (item != null) {
          mainModelsMap.put(row.hashCode(), item);
        }
        continue;
      }

      T mainModel =
          mainModelsMap.computeIfAbsent(
              mainModelPkValue, pk -> mapRowToModel(modelClass, row, builder.getFromAlias()));

      for (JoinClause join : builder.getJoinClauses()) {
        if (join.getRelatedClass() == null) continue;
        Model relatedModel = mapRowToModel(join.getRelatedClass(), row, join.getAlias());
        if (relatedModel == null) continue;

        linkRelatedModel(mainModel, relatedModel, join);
      }
    }
    return new ArrayList<>(mainModelsMap.values());
  }

  /**
   * Liên kết một thực thể quan hệ (related model) vào thực thể chính (main model).
   *
   * @param mainModel Thực thể chính.
   * @param relatedModel Thực thể liên quan cần được liên kết vào.
   * @param join Mệnh đề JOIN chứa thông tin về mối quan hệ.
   */
  @SuppressWarnings("unchecked")
  private void linkRelatedModel(T mainModel, Model relatedModel, JoinClause join) {
    try {
      Field relationField = findField(mainModel.getClass(), join.getRelationName());
      relationField.setAccessible(true);

      if (join.getRelationType() == RelationMetadata.RelationType.ONE_TO_ONE) {
        if (relationField.get(mainModel) == null) {
          relationField.set(mainModel, relatedModel);
        }
      } else if (join.getRelationType() == RelationMetadata.RelationType.ONE_TO_MANY
          || join.getRelationType() == RelationMetadata.RelationType.MANY_TO_MANY) {
        Collection<Model> collection = (Collection<Model>) relationField.get(mainModel);
        EntityMetadata relatedMetadata = MetadataCache.getMetadata(join.getRelatedClass());
        String relatedPkName = relatedMetadata.getPrimaryKeyColumnName();
        Object relatedPkValue = relatedModel.getAttribute(relatedPkName);

        if (relatedPkValue != null) {
          boolean alreadyExists =
              collection.stream()
                  .anyMatch(
                      item -> {
                        Object itemPk = item.getAttribute(relatedPkName);
                        return Objects.equals(relatedPkValue, itemPk);
                      });
          if (!alreadyExists) {
            collection.add(relatedModel);
          }
        }
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not set relation from JOIN: " + join.getRelationName(),
          e);
    }
  }

  /**
   * Ánh xạ một hàng kết quả (dưới dạng Map) thành một instance của một lớp Model.
   *
   * @param <M> Kiểu của Model.
   * @param specificModelClass Lớp cụ thể của model cần tạo.
   * @param row Dữ liệu của một hàng từ cơ sở dữ liệu.
   * @param alias Bí danh (alias) của bảng trong câu lệnh SQL, dùng để trích xuất đúng cột.
   * @return Một instance của model đã được điền dữ liệu, hoặc {@code null} nếu hàng không chứa dữ
   *     liệu cho model này.
   */
  private <M extends Model> M mapRowToModel(
      Class<M> specificModelClass, Map<String, Object> row, String alias) {
    try {
      Map<String, Object> modelAttributes = new HashMap<>();
      boolean hasData = false;

      String prefix = (alias == null) ? "" : alias + "_";
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        if (alias == null || entry.getKey().startsWith(prefix)) {
          String columnName =
              (alias == null) ? entry.getKey() : entry.getKey().substring(prefix.length());
          modelAttributes.put(columnName, entry.getValue());
          if (entry.getValue() != null) {
            hasData = true;
          }
        }
      }

      if (!hasData) return null;

      M modelInstance = specificModelClass.getDeclaredConstructor().newInstance();
      modelInstance.setAttributes(modelAttributes);
      setupRelations(modelInstance);
      return modelInstance;
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not instantiate and map model: " + specificModelClass.getSimpleName(),
          e);
    }
  }

  /**
   * Thiết lập các trường quan hệ cho một instance thực thể đã được tạo.
   *
   * <p>Phương thức này quyết định xem một mối quan hệ nên được tải ngay lập tức (eager) hay tải
   * lười (lazy) dựa trên cấu hình của query builder.
   *
   * @param modelInstance Instance của thực thể cần thiết lập quan hệ.
   */
  private void setupRelations(Model modelInstance) {
    EntityMetadata metadata = MetadataCache.getMetadata(modelInstance.getClass());
    for (RelationMetadata relMeta : metadata.getRelations().values()) {
      try {
        Field field = findField(modelInstance.getClass(), relMeta.getFieldName());
        field.setAccessible(true);
        boolean isEager = builder.getWithRelations().contains(relMeta.getFieldName());

        if (isEager) {
          setupEagerRelation(modelInstance, field, relMeta);
        } else {
          setupLazyRelation(modelInstance, field, relMeta, metadata);
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
   * Thiết lập một mối quan hệ để tải ngay lập tức (eager loading).
   *
   * <p>Đối với các mối quan hệ dạng collection (ToMany), phương thức này khởi tạo một collection
   * rỗng (ArrayList hoặc HashSet) để các thực thể liên quan có thể được thêm vào sau đó trong quá
   * trình xử lý kết quả.
   *
   * @param modelInstance Thực thể chứa mối quan hệ.
   * @param field Trường (field) đại diện cho mối quan hệ.
   * @param relMeta Metadata của mối quan hệ.
   * @throws IllegalAccessException nếu không thể truy cập trường.
   */
  private void setupEagerRelation(Model modelInstance, Field field, RelationMetadata relMeta)
      throws IllegalAccessException {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY
        || relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      if (List.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new ArrayList<>());
      } else if (Set.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new HashSet<>());
      }
    }
  }

  /**
   * Thiết lập một mối quan hệ để tải lười (lazy loading).
   *
   * <p>Đối với các mối quan hệ dạng collection, nó sẽ gán một proxy collection (LazyLoadingList
   * hoặc LazyLoadingSet). Đối với các mối quan hệ ToOne, nó sẽ tạo một proxy CGLIB.
   *
   * @param modelInstance Thực thể chứa mối quan hệ.
   * @param field Trường (field) đại diện cho mối quan hệ.
   * @param relMeta Metadata của mối quan hệ.
   * @param ownerMetadata Metadata của thực thể chứa mối quan hệ.
   * @throws IllegalAccessException nếu không thể truy cập trường.
   */
  private void setupLazyRelation(
      Model modelInstance, Field field, RelationMetadata relMeta, EntityMetadata ownerMetadata)
      throws IllegalAccessException {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY
        || relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      LazyLoader<?> loader = createCollectionLoader(modelInstance, relMeta);
      if (List.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new LazyLoadingList<>(loader));
      } else if (Set.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new LazyLoadingSet<>(loader));
      }
    } else {
      Object proxy = createProxyForToOneRelation(modelInstance, relMeta, ownerMetadata);
      if (proxy != null) {
        field.set(modelInstance, proxy);
      }
    }
  }

  /**
   * Tạo một đối tượng proxy CGLIB cho các mối quan hệ ToOne (MANY_TO_ONE, ONE_TO_ONE) để thực hiện
   * tải lười.
   *
   * @param modelInstance Thực thể chứa mối quan hệ.
   * @param relMeta Metadata của mối quan hệ.
   * @param ownerMetadata Metadata của thực thể chứa mối quan hệ.
   * @return Một đối tượng proxy, hoặc {@code null} nếu không thể tạo (ví dụ: khóa ngoại là null).
   */
  @SuppressWarnings("unchecked")
  private Object createProxyForToOneRelation(
      Model modelInstance, RelationMetadata relMeta, EntityMetadata ownerMetadata) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(relMeta.getTargetEntity());

    Object foreignKeyValue = null;
    String targetColumn = null;

    if (relMeta.getJoinColumnName() != null) {
      foreignKeyValue = modelInstance.getAttribute(relMeta.getJoinColumnName());
      EntityMetadata targetMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
      targetColumn = targetMeta.getPrimaryKeyColumnName();
    } else {
      EntityMetadata inverseMetadata = MetadataCache.getMetadata(relMeta.getTargetEntity());
      RelationMetadata inverseRelation = inverseMetadata.getRelations().get(relMeta.getMappedBy());
      targetColumn = inverseRelation.getJoinColumnName();
      foreignKeyValue = modelInstance.getAttribute(ownerMetadata.getPrimaryKeyColumnName());
    }

    if (foreignKeyValue != null) {
      enhancer.setCallback(
          new LazyLoadInterceptor(
              (Class<? extends Model>) relMeta.getTargetEntity(), targetColumn, foreignKeyValue));
      return enhancer.create();
    }
    return null;
  }

  /**
   * Tạo một {@link LazyLoader} phù hợp cho một mối quan hệ dạng collection.
   *
   * @param modelInstance Thực thể chứa mối quan hệ.
   * @param relMeta Metadata của mối quan hệ.
   * @return Một instance của {@link OneToManyLoader} hoặc {@link ManyToManyLoader}.
   */
  private LazyLoader<?> createCollectionLoader(Model modelInstance, RelationMetadata relMeta) {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY) {
      return new OneToManyLoader(modelInstance, relMeta);
    }
    if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      return new ManyToManyLoader(modelInstance, relMeta);
    }
    throw new VMCException(
        HttpStatus.BAD_REQUEST, "Cannot create loader for relation type: " + relMeta.getType());
  }

  /**
   * Ánh xạ một danh sách các thực thể sang một danh sách các DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param entities Danh sách các thực thể cần ánh xạ.
   * @param dtoClass Lớp của DTO đích.
   * @return Một danh sách các DTO đã được điền dữ liệu.
   */
  public <D> List<D> mapEntitiesToDtos(List<T> entities, Class<D> dtoClass) {
    return entities.stream()
        .map(entity -> mapEntityToDto(entity, dtoClass))
        .collect(Collectors.toList());
  }

  /**
   * Xử lý kết quả thô và ánh xạ trực tiếp sang DTO.
   *
   * <p>Phương thức này gộp các bước xử lý để đảm bảo an toàn kiểu.
   *
   * @param flatResults Danh sách kết quả thô từ DB.
   * @param dtoClass Lớp DTO đích.
   * @param <D> Kiểu của DTO.
   * @return Một danh sách các DTO đã được điền dữ liệu.
   */
  public <D> List<D> processAndMapToDtos(List<Map<String, Object>> flatResults, Class<D> dtoClass) {
    if (flatResults == null || flatResults.isEmpty()) {
      return Collections.emptyList();
    }
    List<T> entities = this.process(flatResults);
    return this.mapEntitiesToDtos(entities, dtoClass);
  }

  /**
   * Ánh xạ một thực thể đơn lẻ sang một DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param entity Thực thể cần ánh xạ.
   * @param dtoClass Lớp của DTO đích.
   * @return Một instance DTO đã được điền dữ liệu, hoặc {@code null} nếu entity đầu vào là null.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private <D> D mapEntityToDto(T entity, Class<D> dtoClass) {
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
            "Lớp DTO " + dtoClass.getSimpleName() + " phải triển khai BaseDto.");
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Không thể ánh xạ entity "
              + entity.getClass().getSimpleName()
              + " sang DTO "
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
   */
  private static Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new VMCException(
        HttpStatus.BAD_REQUEST, "Field '" + fieldName + "' not found in class " + clazz.getName());
  }
}
