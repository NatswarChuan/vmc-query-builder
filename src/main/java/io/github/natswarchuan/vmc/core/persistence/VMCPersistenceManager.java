package io.github.natswarchuan.vmc.core.persistence;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.handler.CascadeRemoveHandler;
import io.github.natswarchuan.vmc.core.persistence.handler.CrudExecutor;
import io.github.natswarchuan.vmc.core.persistence.handler.RelationshipSynchronizer;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý các thao tác bền bỉ (persistence) cho các thực thể (entity). Lớp này điều phối các
 * handler chuyên biệt để thực hiện việc lưu, xóa, và đồng bộ hóa.
 *
 * @author NatswarChuan
 */
@Component
public class VMCPersistenceManager {

  private final GenericQueryExecutorMapper queryExecutor;
  private final CrudExecutor crudExecutor;
  private final RelationshipSynchronizer relationshipSynchronizer;
  private final CascadeRemoveHandler cascadeRemoveHandler;

  public VMCPersistenceManager(GenericQueryExecutorMapper queryExecutor) {
    this.queryExecutor = queryExecutor;
    this.crudExecutor = new CrudExecutor();
    this.relationshipSynchronizer = new RelationshipSynchronizer(this);
    this.cascadeRemoveHandler = new CascadeRemoveHandler();
  }

  @Transactional
  public void save(Model model, SaveOptions options) {
    saveGraph(model, options, new IdentityHashMap<>());
  }

  @Transactional
  public void saveAll(Iterable<? extends Model> models, SaveOptions options) {
    if (models == null) return;
    IdentityHashMap<Model, Model> processed = new IdentityHashMap<>();
    for (Model model : models) {
      saveGraph(model, options, processed);
    }
  }

  @Transactional
  public <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto, SaveOptions options) {
    if (dto == null) return null;
    E entity = dto.toEntity();
    save(entity, options);
    return (D) dto.toDto(entity);
  }

  @Transactional
  public <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions options) {
    if (dtos == null) return Collections.emptyList();
    List<Model> entitiesToSave = new ArrayList<>();
    List<D> originalDtos = new ArrayList<>();
    dtos.forEach(
        dto -> {
          originalDtos.add(dto);
          entitiesToSave.add(dto.toEntity());
        });

    saveAll(entitiesToSave, options);

    List<D> savedDtos = new ArrayList<>();
    for (int i = 0; i < entitiesToSave.size(); i++) {
      savedDtos.add((D) originalDtos.get(i).toDto(entitiesToSave.get(i)));
    }
    return savedDtos;
  }

  @Transactional
  public void remove(Model model) {
    if (model == null) return;
    try {
      EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));
      Object pkValue = getPrimaryKeyValue(model, metadata);
      if (pkValue == null) return;

      cascadeRemoveHandler.handleCascades(model);

      String sql =
          String.format(
              "DELETE FROM %s WHERE %s = #{params.pkValue}",
              metadata.getTableName(), metadata.getPrimaryKeyColumnName());
      queryExecutor.delete(sql, Map.of("pkValue", pkValue));

    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error when deleting entity: " + model.getClass().getSimpleName(),
          e);
    }
  }

  @Transactional
  public void saveGraph(Model model, SaveOptions options, Map<Model, Model> processedEntities) {
    if (model == null || processedEntities.containsKey(model)) {
      return;
    }

    try {
      EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));

      for (RelationMetadata relMeta : metadata.getRelations().values()) {
        if (relMeta.isOwningSideOfAssociation()) {
          Field field = findField(model.getClass(), relMeta.getFieldName());
          field.setAccessible(true);
          Object relatedValue = field.get(model);
          if (relatedValue instanceof Model) {
            saveGraph((Model) relatedValue, options, processedEntities);
          }
        }
      }

      Object pkValue = getPrimaryKeyValue(model, metadata);
      boolean isNew =
          (pkValue == null || (pkValue instanceof Number && ((Number) pkValue).longValue() == 0));
      if (isNew) {
        crudExecutor.insert(model, metadata);
      } else {
        crudExecutor.update(model, metadata);
      }
      processedEntities.put(model, model);

      Set<String> relationsToCascade = options.getRelationsToCascade();
      for (String relationName : relationsToCascade) {
        RelationMetadata relMeta = metadata.getRelations().get(relationName);
        if (relMeta == null) {
          continue;
        }

        Field field = findField(model.getClass(), relationName);
        field.setAccessible(true);
        Object relatedValue = field.get(model);
        if (relatedValue == null) continue;

        if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_ONE) {
          if (relMeta.isInverseSide()) {
            Model relatedModel = (Model) relatedValue;
            saveGraph(relatedModel, options, processedEntities);
          }
        } else if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY) {
          if (relMeta.isInverseSide()) {
            relationshipSynchronizer.synchronizeOneToMany(
                model, relMeta, (Collection<?>) relatedValue, options, processedEntities);
          }
        }
        else if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
          relationshipSynchronizer.synchronizeManyToMany(
              model, relMeta, (Collection<?>) relatedValue, options, processedEntities);
        }
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error during save graph operation.", e);
    }
  }

  private Object getPrimaryKeyValue(Model model, EntityMetadata metadata) throws Exception {
    Field pkField = findField(model.getClass(), metadata.getPrimaryKeyFieldName());
    pkField.setAccessible(true);
    return pkField.get(model);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }

  private Class<?> getUnproxiedClass(Class<?> clazz) {
    if (clazz.getName().contains("$$EnhancerByCGLIB$$")) {
      return clazz.getSuperclass();
    }
    return clazz;
  }
}
