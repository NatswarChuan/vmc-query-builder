package io.github.natswarchuan.vmc.core.persistence.handler;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.util.BeanUtil;

/**
 * Xử lý logic xóa theo tầng (cascade remove) và dọn dẹp các mối quan hệ khi một thực thể bị xóa.
 * @author NatswarChuan
 */
public class CascadeRemoveHandler {

  private GenericQueryExecutorMapper queryExecutor;

  public CascadeRemoveHandler() {
  }

  private GenericQueryExecutorMapper getQueryExecutor() {
    if (this.queryExecutor == null) {
      this.queryExecutor = BeanUtil.getBean(GenericQueryExecutorMapper.class);
    }
    return this.queryExecutor;
  }

  public void handleCascades(Model model) throws Exception {
    EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));
    for (RelationMetadata relMeta : metadata.getRelations().values()) {
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

  private void handleOneToManyOnRemove(Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta) throws Exception {
    EntityMetadata childMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
    RelationMetadata inverseRelMeta = childMeta.getRelations().get(relMeta.getMappedBy());
    String fkColumn = inverseRelMeta.getJoinColumnName();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);

    if (relMeta.isOrphanRemoval()) {
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
    } else {
      String updateSql =
          String.format(
              "UPDATE %s SET %s = NULL WHERE %s = #{params.ownerId}",
              childMeta.getTableName(), fkColumn, fkColumn);
      getQueryExecutor().update(updateSql, Map.of("ownerId", ownerId));
    }
  }

  private void handleOneToOneInverseOnRemove(Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta)
      throws Exception {
    EntityMetadata relatedMeta = MetadataCache.getMetadata(relMeta.getTargetEntity());
    RelationMetadata inverseRelMeta = relatedMeta.getRelations().get(relMeta.getMappedBy());
    String fkColumn = inverseRelMeta.getJoinColumnName();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);

    if (relMeta.isOrphanRemoval() || !inverseRelMeta.isForeignKeyNullable()) {
      String deleteSql =
          String.format(
              "DELETE FROM %s WHERE %s = #{params.ownerId}", relatedMeta.getTableName(), fkColumn);
      getQueryExecutor().delete(deleteSql, Map.of("ownerId", ownerId));
    } else {
      String updateSql =
          String.format(
              "UPDATE %s SET %s = NULL WHERE %s = #{params.ownerId}",
              relatedMeta.getTableName(), fkColumn, fkColumn);
      getQueryExecutor().update(updateSql, Map.of("ownerId", ownerId));
    }
  }

  private void handleManyToManyOnRemove(Model owner, RelationMetadata relMeta, EntityMetadata ownerMeta) throws Exception {
    JoinTableMetadata joinTable = relMeta.getJoinTable();
    Object ownerId = getPrimaryKeyValue(owner, ownerMeta);
    String deleteSql =
        String.format(
            "DELETE FROM %s WHERE %s = #{params.ownerId}",
            joinTable.getTableName(), joinTable.getJoinColumn());
    getQueryExecutor().delete(deleteSql, Map.of("ownerId", ownerId));
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
