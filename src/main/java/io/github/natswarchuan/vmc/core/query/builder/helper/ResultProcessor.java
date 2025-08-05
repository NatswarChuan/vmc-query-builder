package io.github.natswarchuan.vmc.core.query.builder.helper;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.lazy.*;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.builder.AbstractQueryBuilder;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.http.HttpStatus;

/**
 * Xử lý kết quả thô từ DB, ánh xạ thành các đối tượng Entity và DTO.
 *
 * @param <T> Kiểu của thực thể gốc.
 */
public class ResultProcessor<T extends Model> {

  private final AbstractQueryBuilder<?> builder;
  private final Class<T> modelClass;

  @SuppressWarnings("unchecked")
  public ResultProcessor(AbstractQueryBuilder<?> builder) {
    this.builder = builder;
    this.modelClass = (Class<T>) builder.getModelClass();
  }

  /**
   * Xử lý kết quả phẳng từ cơ sở dữ liệu và xây dựng lại đồ thị đối tượng.
   *
   * @param flatResults Danh sách kết quả thô từ DB.
   * @return Một danh sách các thực thể gốc đã được xây dựng hoàn chỉnh.
   */
  public List<T> process(List<Map<String, Object>> flatResults) {
    Map<Object, T> mainModelsMap = new LinkedHashMap<>();
    EntityMetadata mainMetadata = MetadataCache.getMetadata(modelClass);
    String mainPrimaryKeyAlias = builder.getFromAlias() + "_" + mainMetadata.getPrimaryKeyColumnName();

    for (Map<String, Object> row : flatResults) {
      Object mainModelPkValue = row.get(mainPrimaryKeyAlias);

      if (mainModelPkValue == null) {
        T item = mapRowToModel(modelClass, row, null);
        if (item != null) {
          mainModelsMap.put(row.hashCode(), item); // Dùng hashcode làm key dự phòng
        }
        continue;
      }

      T mainModel = mainModelsMap.computeIfAbsent(mainModelPkValue, pk -> mapRowToModel(modelClass, row, builder.getFromAlias()));

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
   * Liên kết một model quan hệ vào model chính.
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
      } else if (join.getRelationType() == RelationMetadata.RelationType.ONE_TO_MANY || join.getRelationType() == RelationMetadata.RelationType.MANY_TO_MANY) {
        Collection<Model> collection = (Collection<Model>) relationField.get(mainModel);
        EntityMetadata relatedMetadata = MetadataCache.getMetadata(join.getRelatedClass());
        String relatedPkName = relatedMetadata.getPrimaryKeyColumnName();
        Object relatedPkValue = relatedModel.getAttribute(relatedPkName);

        if (relatedPkValue != null) {
          boolean alreadyExists = collection.stream().anyMatch(item -> {
            Object itemPk = item.getAttribute(relatedPkName);
            return Objects.equals(relatedPkValue, itemPk);
          });
          if (!alreadyExists) {
            collection.add(relatedModel);
          }
        }
      }
    } catch (Exception e) {
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not set relation from JOIN: " + join.getRelationName(), e);
    }
  }

  /**
   * Ánh xạ một hàng kết quả (dưới dạng Map) thành một instance của một lớp Model.
   */
  private <M extends Model> M mapRowToModel(Class<M> specificModelClass, Map<String, Object> row, String alias) {
    try {
      Map<String, Object> modelAttributes = new HashMap<>();
      boolean hasData = false;

      String prefix = (alias == null) ? "" : alias + "_";
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        if (alias == null || entry.getKey().startsWith(prefix)) {
          String columnName = (alias == null) ? entry.getKey() : entry.getKey().substring(prefix.length());
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
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not instantiate and map model: " + specificModelClass.getSimpleName(), e);
    }
  }

  /**
   * Thiết lập các mối quan hệ cho một instance thực thể đã được tạo.
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
        throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not set up relation for " + relMeta.getFieldName(), e);
      }
    }
  }

  private void setupEagerRelation(Model modelInstance, Field field, RelationMetadata relMeta) throws IllegalAccessException {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY || relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      if (List.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new ArrayList<>());
      } else if (Set.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new HashSet<>());
      }
    }
  }

  private void setupLazyRelation(Model modelInstance, Field field, RelationMetadata relMeta, EntityMetadata ownerMetadata) throws IllegalAccessException {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY || relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      LazyLoader<?> loader = createCollectionLoader(modelInstance, relMeta);
      if (List.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new LazyLoadingList<>(loader));
      } else if (Set.class.isAssignableFrom(field.getType())) {
        field.set(modelInstance, new LazyLoadingSet<>(loader));
      }
    } else { // ToOne
      Object proxy = createProxyForToOneRelation(modelInstance, relMeta, ownerMetadata);
      if (proxy != null) {
        field.set(modelInstance, proxy);
      }
    }
  }

  /**
   * Tạo proxy cho các quan hệ ToOne (MANY_TO_ONE, ONE_TO_ONE) để tải lười.
   */
  @SuppressWarnings("unchecked")
  private Object createProxyForToOneRelation(Model modelInstance, RelationMetadata relMeta, EntityMetadata ownerMetadata) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(relMeta.getTargetEntity());

    Object foreignKeyValue = null;
    String targetColumn = null;

    if (relMeta.getJoinColumnName() != null) { // Owner of the relationship (has foreign key)
      foreignKeyValue = modelInstance.getAttribute(relMeta.getJoinColumnName());
      EntityMetadata targetMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
      targetColumn = targetMeta.getPrimaryKeyColumnName();
    } else { // Not owner, mappedBy is set
      EntityMetadata inverseMetadata = MetadataCache.getMetadata(relMeta.getTargetEntity());
      RelationMetadata inverseRelation = inverseMetadata.getRelations().get(relMeta.getMappedBy());
      targetColumn = inverseRelation.getJoinColumnName();
      foreignKeyValue = modelInstance.getAttribute(ownerMetadata.getPrimaryKeyColumnName());
    }

    if (foreignKeyValue != null) {
      enhancer.setCallback(new LazyLoadInterceptor((Class<? extends Model>) relMeta.getTargetEntity(), targetColumn, foreignKeyValue));
      return enhancer.create();
    }
    return null;
  }

  /**
   * Tạo một LazyLoader phù hợp cho một mối quan hệ collection.
   */
  private LazyLoader<?> createCollectionLoader(Model modelInstance, RelationMetadata relMeta) {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY) {
      return new OneToManyLoader(modelInstance, relMeta);
    }
    if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
      return new ManyToManyLoader(modelInstance, relMeta);
    }
    throw new VMCException(HttpStatus.BAD_REQUEST, "Cannot create loader for relation type: " + relMeta.getType());
  }

  /**
   * Ánh xạ một danh sách các thực thể sang một danh sách các DTO.
   */
  public <D> List<D> mapEntitiesToDtos(List<T> entities, Class<D> dtoClass) {
    return entities.stream().map(entity -> mapEntityToDto(entity, dtoClass)).collect(Collectors.toList());
  }

  /**
   * Xử lý kết quả thô và ánh xạ trực tiếp sang DTO.
   * Phương thức này gộp các bước xử lý để đảm bảo an toàn kiểu.
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
        throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Lớp DTO " + dtoClass.getSimpleName() + " phải triển khai BaseDto.");
      }
    } catch (Exception e) {
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể ánh xạ entity " + entity.getClass().getSimpleName() + " sang DTO " + dtoClass.getSimpleName(), e);
    }
  }

  /**
   * Tìm một trường trong một lớp hoặc các lớp cha của nó bằng reflection.
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
    throw new VMCException(HttpStatus.BAD_REQUEST, "Field '" + fieldName + "' not found in class " + clazz.getName());
  }
}
