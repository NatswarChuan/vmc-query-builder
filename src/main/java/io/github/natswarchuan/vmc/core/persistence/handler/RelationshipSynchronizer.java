package io.github.natswarchuan.vmc.core.persistence.handler;

import io.github.natswarchuan.vmc.core.entity.Model;
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

/**
 * Đồng bộ hóa trạng thái của các mối quan hệ (OneToMany, ManyToMany) giữa trạng thái trong bộ nhớ
 * và cơ sở dữ liệu.
 *
 * @author NatswarChuan
 */
public class RelationshipSynchronizer {

  private GenericQueryExecutorMapper queryExecutor;
  private final VMCPersistenceManager persistenceManager;

  public RelationshipSynchronizer(VMCPersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  private GenericQueryExecutorMapper getQueryExecutor() {
    if (this.queryExecutor == null) {
      this.queryExecutor = BeanUtil.getBean(GenericQueryExecutorMapper.class);
    }
    return this.queryExecutor;
  }

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
        if (relMeta.isOrphanRemoval()) {
          String deleteSql =
              String.format(
                  "DELETE FROM %s WHERE %s IN (%s)",
                  childMeta.getTableName(),
                  childMeta.getPrimaryKeyColumnName(),
                  getInClauseValues(idsToDisassociate));
          getQueryExecutor().delete(deleteSql, Collections.emptyMap());
        } else {
          String updateSql =
              String.format(
                  "UPDATE %s SET %s = NULL WHERE %s IN (%s)",
                  childMeta.getTableName(),
                  fkColumn,
                  childMeta.getPrimaryKeyColumnName(),
                  getInClauseValues(idsToDisassociate));
          getQueryExecutor().update(updateSql, Collections.emptyMap());
        }
      }
    } catch (Exception e) {

    }
  }

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

    }
  }

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

  private void setInverseSide(Model child, Model parent, RelationMetadata parentRelMeta)
      throws Exception {
    String mappedByField = parentRelMeta.getMappedBy();
    if (mappedByField != null && !mappedByField.isEmpty()) {
      Field inverseField = findField(child.getClass(), mappedByField);
      inverseField.setAccessible(true);
      inverseField.set(child, parent);
    }
  }

  private Object getPrimaryKeyValue(Model model) throws Exception {
    if (model == null) return null;
    EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));
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
