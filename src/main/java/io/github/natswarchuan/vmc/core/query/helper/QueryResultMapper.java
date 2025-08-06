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
 * @author NatswarChuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryResultMapper {

  private final Class<? extends Model> modelClass;
  private final String fromAlias;
  private final List<String> withRelations;

  public QueryResultMapper(
      Class<? extends Model> modelClass, String fromAlias, List<String> withRelations) {
    this.modelClass = modelClass;
    this.fromAlias = fromAlias;
    this.withRelations = withRelations;
  }

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

  /** Lấy một thực thể từ cache hoặc tạo mới nếu chưa có. */
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

  /** Liên kết hai thực thể và tự động thiết lập tham chiếu ngược. */
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
            if (relMeta.isOwningSideOfAssociation()) {
              Object fkValue = modelInstance.getAttribute(relMeta.getJoinColumnName());
              if (fkValue != null) {
                EntityMetadata targetMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
                Object proxy =
                    createLazyProxy(
                        relMeta.getTargetEntity(), targetMeta.getPrimaryKeyColumnName(), fkValue);
                field.set(modelInstance, proxy);
              }
            } else if (relMeta.isInverseSide()) {
              EntityMetadata ownerMeta = MetadataCache.getMetadata(modelInstance.getClass());
              Object pkValue = modelInstance.getAttribute(ownerMeta.getPrimaryKeyColumnName());
              if (pkValue != null) {
                EntityMetadata targetMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
                RelationMetadata owningRel = targetMeta.getRelations().get(relMeta.getMappedBy());
                if (owningRel != null && owningRel.getJoinColumnName() != null) {
                  String fkColumnOnTarget = owningRel.getJoinColumnName();
                  Object proxy =
                      createLazyProxy(relMeta.getTargetEntity(), fkColumnOnTarget, pkValue);
                  field.set(modelInstance, proxy);
                }
              }
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

  private Object createLazyProxy(Class<?> targetClass, String pkColumn, Object pkValue) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(targetClass);
    enhancer.setCallback(
        new LazyLoadInterceptor((Class<? extends Model>) targetClass, pkColumn, pkValue));
    return enhancer.create();
  }

  private LazyLoader<?> createLoader(Model modelInstance, RelationMetadata relMeta) {
    if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY)
      return new OneToManyLoader(modelInstance, relMeta);
    if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY)
      return new ManyToManyLoader(modelInstance, relMeta);
    throw new VMCException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Cannot create loader for relation type: " + relMeta.getType());
  }

  public <D> List<D> mapEntitiesToDtos(List<? extends Model> entities, Class<D> dtoClass) {
    return entities.stream()
        .map(entity -> mapEntityToDto(entity, dtoClass))
        .collect(Collectors.toList());
  }

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
