package io.github.natswarchuan.vmc.core.query.builder.helper;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.query.builder.AbstractQueryBuilder;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.clause.PreparedQuery;
import io.github.natswarchuan.vmc.core.query.clause.WhereClause;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/** Xây dựng các câu lệnh SQL (SELECT, COUNT) từ trạng thái của một AbstractQueryBuilder. */
public class SqlBuilder {

  private final AbstractQueryBuilder<?> builder;

  public SqlBuilder(AbstractQueryBuilder<?> builder) {
    this.builder = builder;
  }

  /**
   * Xây dựng câu lệnh SQL SELECT hoàn chỉnh.
   *
   * @return Một đối tượng PreparedQuery chứa SQL và các tham số.
   */
  public PreparedQuery buildSelectQuery() {
    prepareJoinsForWith();
    List<String> finalSelectColumns = new ArrayList<>(builder.getSelectColumns());

    if (finalSelectColumns.isEmpty()) {
      addDefaultSelectColumns(finalSelectColumns);
    }

    Set<String> selectAliases = extractSelectAliases(finalSelectColumns);

    StringBuilder sql = new StringBuilder("SELECT ").append(String.join(", ", finalSelectColumns));
    sql.append(" FROM ")
        .append(builder.getFromTable())
        .append(" AS ")
        .append(builder.getFromAlias());

    appendJoins(sql);
    Map<String, Object> params = appendWhereClause(sql);
    appendGroupBy(sql);
    appendOrderBy(sql, selectAliases);
    appendLimitOffset(sql);

    return new PreparedQuery(sql.toString(), params);
  }

  /**
   * Xây dựng câu lệnh SQL để đếm số lượng bản ghi.
   *
   * @return Một đối tượng PreparedQuery chứa SQL và các tham số.
   */
  public PreparedQuery buildCountQuery() {
    prepareJoinsForWith();

    StringBuilder baseQuery = new StringBuilder();
    baseQuery
        .append(" FROM ")
        .append(builder.getFromTable())
        .append(" AS ")
        .append(builder.getFromAlias());

    appendJoins(baseQuery);
    Map<String, Object> params = appendWhereClause(baseQuery);

    if (!builder.getGroupByColumns().isEmpty()) {
      String groupByClause =
          builder.getGroupByColumns().stream()
              .map(c -> c.contains(".") || c.contains("(") ? c : builder.getFromAlias() + "." + c)
              .collect(Collectors.joining(", "));
      String subQuery = "SELECT 1" + baseQuery + " GROUP BY " + groupByClause;
      String finalQuery = "SELECT COUNT(*) AS count FROM (" + subQuery + ") AS count_subquery";
      return new PreparedQuery(finalQuery, params);
    } else {
      EntityMetadata mainMetadata = MetadataCache.getMetadata(builder.getModelClass());
      String pkColumn = mainMetadata.getPrimaryKeyColumnName();
      String pkColumnWithAlias = builder.getFromAlias() + "." + pkColumn;
      String finalQuery = "SELECT COUNT(DISTINCT " + pkColumnWithAlias + ") AS count" + baseQuery;
      return new PreparedQuery(finalQuery, params);
    }
  }

  /** Chuẩn bị các mệnh đề JOIN cần thiết cho các mối quan hệ được chỉ định trong with(). */
  private void prepareJoinsForWith() {
    if (builder.getWithRelations().isEmpty()) return;

    EntityMetadata mainMetadata = MetadataCache.getMetadata(builder.getModelClass());

    for (String relationName : builder.getWithRelations()) {
      if (builder.getJoinClauses().stream()
          .anyMatch(jc -> jc != null && relationName.equals(jc.getRelationName()))) {
        continue;
      }

      RelationMetadata relMeta = mainMetadata.getRelations().get(relationName);
      if (relMeta == null) {
        throw new VMCException(
            HttpStatus.BAD_REQUEST,
            "Relation '"
                + relationName
                + "' not found in "
                + builder.getModelClass().getSimpleName());
      }
      addJoinForRelation(mainMetadata, relMeta);
    }
  }

  /** Thêm một JOIN cho một mối quan hệ cụ thể. */
  @SuppressWarnings("unchecked")
  private void addJoinForRelation(EntityMetadata mainMetadata, RelationMetadata relMeta) {
    String relationName = relMeta.getFieldName();
    String relationAlias = relationName.toLowerCase();
    EntityMetadata targetMetadata = MetadataCache.getMetadata(relMeta.getTargetEntity());

    switch (relMeta.getType()) {
      case MANY_TO_ONE:
      case ONE_TO_ONE:
        if (relMeta.getJoinColumnName() != null) {
          builder
              .getJoinClauses()
              .add(
                  new JoinClause(
                      VMCSqlJoinType.LEFT_JOIN,
                      targetMetadata.getTableName(),
                      relationAlias,
                      builder.getFromAlias() + "." + relMeta.getJoinColumnName(),
                      "=",
                      relationAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
                      (Class<? extends Model>) relMeta.getTargetEntity(),
                      relationName,
                      RelationMetadata.RelationType.ONE_TO_ONE));
        } else {
          RelationMetadata inverseRelation =
              targetMetadata.getRelations().get(relMeta.getMappedBy());
          builder
              .getJoinClauses()
              .add(
                  new JoinClause(
                      VMCSqlJoinType.LEFT_JOIN,
                      targetMetadata.getTableName(),
                      relationAlias,
                      builder.getFromAlias() + "." + mainMetadata.getPrimaryKeyColumnName(),
                      "=",
                      relationAlias + "." + inverseRelation.getJoinColumnName(),
                      (Class<? extends Model>) relMeta.getTargetEntity(),
                      relationName,
                      RelationMetadata.RelationType.ONE_TO_ONE));
        }
        break;
      case ONE_TO_MANY:
        RelationMetadata inverseRelation = targetMetadata.getRelations().get(relMeta.getMappedBy());
        builder
            .getJoinClauses()
            .add(
                new JoinClause(
                    VMCSqlJoinType.LEFT_JOIN,
                    targetMetadata.getTableName(),
                    relationAlias,
                    builder.getFromAlias() + "." + mainMetadata.getPrimaryKeyColumnName(),
                    "=",
                    relationAlias + "." + inverseRelation.getJoinColumnName(),
                    (Class<? extends Model>) relMeta.getTargetEntity(),
                    relationName,
                    RelationMetadata.RelationType.ONE_TO_MANY));
        break;
      case MANY_TO_MANY:
        JoinTableMetadata joinTable = relMeta.getJoinTable();
        String pivotAlias = relationName + "_pivot";
        builder
            .getJoinClauses()
            .add(
                new JoinClause(
                    VMCSqlJoinType.LEFT_JOIN,
                    joinTable.getTableName(),
                    pivotAlias,
                    builder.getFromAlias() + "." + mainMetadata.getPrimaryKeyColumnName(),
                    "=",
                    pivotAlias + "." + joinTable.getJoinColumn(),
                    null,
                    null,
                    null));
        builder
            .getJoinClauses()
            .add(
                new JoinClause(
                    VMCSqlJoinType.LEFT_JOIN,
                    targetMetadata.getTableName(),
                    relationAlias,
                    pivotAlias + "." + joinTable.getInverseJoinColumn(),
                    "=",
                    relationAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
                    (Class<? extends Model>) relMeta.getTargetEntity(),
                    relationName,
                    RelationMetadata.RelationType.MANY_TO_MANY));
        break;
    }
  }

  /** Thêm các cột select mặc định nếu người dùng không chỉ định. */
  private void addDefaultSelectColumns(List<String> selectColumns) {
    String fromAlias = builder.getFromAlias();
    EntityMetadata mainMetadata = MetadataCache.getMetadata(builder.getModelClass());
    mainMetadata
        .getFieldToColumnMap()
        .values()
        .forEach(
            c -> selectColumns.add(String.format("%s.%s as %s_%s", fromAlias, c, fromAlias, c)));

    for (RelationMetadata relMeta : mainMetadata.getRelations().values()) {
      if (!builder.getWithRelations().contains(relMeta.getFieldName())
          && relMeta.getJoinColumnName() != null) {
        String fkColumn = relMeta.getJoinColumnName();
        String aliasedFk =
            String.format("%s.%s as %s_%s", fromAlias, fkColumn, fromAlias, fkColumn);
        if (!selectColumns.contains(aliasedFk)) {
          selectColumns.add(aliasedFk);
        }
      }
    }

    for (JoinClause join : builder.getJoinClauses()) {
      if (join.getRelatedClass() != null) {
        EntityMetadata relatedMetadata = MetadataCache.getMetadata(join.getRelatedClass());
        relatedMetadata
            .getFieldToColumnMap()
            .values()
            .forEach(
                c ->
                    selectColumns.add(
                        String.format("%s.%s as %s_%s", join.getAlias(), c, join.getAlias(), c)));
      }
    }
  }

  /** Trích xuất các bí danh từ danh sách cột SELECT. */
  private Set<String> extractSelectAliases(List<String> selectColumns) {
    Set<String> selectAliases = new HashSet<>();
    for (String col : selectColumns) {
      String lowerCol = col.toLowerCase();
      int asIndex = lowerCol.lastIndexOf(" as ");
      if (asIndex != -1) {
        selectAliases.add(col.substring(asIndex + 4).trim());
      }
    }
    return selectAliases;
  }

  /** Nối các mệnh đề JOIN vào câu lệnh SQL. */
  private void appendJoins(StringBuilder sql) {
    for (JoinClause join : builder.getJoinClauses()) {
      sql.append(" ")
          .append(join.getType().getSql())
          .append(" ")
          .append(join.getTable())
          .append(" AS ")
          .append(join.getAlias())
          .append(" ON ")
          .append(join.getFirst())
          .append(" ")
          .append(join.getOperator())
          .append(" ")
          .append(join.getSecond());
    }
  }

  /** Nối mệnh đề WHERE vào câu lệnh SQL và trả về các tham số. */
  private Map<String, Object> appendWhereClause(StringBuilder sql) {
    Map<String, Object> params = new HashMap<>();
    if (!builder.getWhereClauses().isEmpty()) {
      sql.append(" WHERE ");
      for (int i = 0; i < builder.getWhereClauses().size(); i++) {
        WhereClause clause = builder.getWhereClauses().get(i);
        if (i > 0) {
          sql.append(" ").append(clause.getConjunction()).append(" ");
        }
        String columnWithAlias =
            clause.getColumn().contains(".")
                ? clause.getColumn()
                : builder.getFromAlias() + "." + clause.getColumn();

        if (clause.getOperator() == VMCSqlOperator.IN && clause.getValue() instanceof Collection) {
          handleInClause(sql, params, clause, columnWithAlias);
        } else {
          handleSimpleClause(sql, params, clause, columnWithAlias);
        }
      }
    }
    return params;
  }

  private void handleInClause(
      StringBuilder sql, Map<String, Object> params, WhereClause clause, String columnWithAlias) {
    Collection<?> values = (Collection<?>) clause.getValue();
    if (values.isEmpty()) {
      sql.append("1 = 0"); // Điều kiện luôn sai nếu IN list rỗng
    } else {
      String placeholders =
          values.stream()
              .map(
                  v -> {
                    String pName = "p" + params.size();
                    params.put(pName, v);
                    return "#{params." + pName + "}";
                  })
              .collect(Collectors.joining(","));
      sql.append(String.format("%s IN (%s)", columnWithAlias, placeholders));
    }
  }

  private void handleSimpleClause(
      StringBuilder sql, Map<String, Object> params, WhereClause clause, String columnWithAlias) {
    String pName = "p" + params.size();
    params.put(pName, clause.getValue());
    sql.append(String.format("%s %s #{params.%s}", columnWithAlias, clause.getOperator(), pName));
  }

  /** Nối mệnh đề GROUP BY vào câu lệnh SQL. */
  private void appendGroupBy(StringBuilder sql) {
    if (!builder.getGroupByColumns().isEmpty()) {
      sql.append(" GROUP BY ")
          .append(
              builder.getGroupByColumns().stream()
                  .map(
                      c ->
                          c.contains(".") || c.contains("(") ? c : builder.getFromAlias() + "." + c)
                  .collect(Collectors.joining(", ")));
    }
  }

  /** Nối mệnh đề ORDER BY vào câu lệnh SQL. */
  private void appendOrderBy(StringBuilder sql, Set<String> selectAliases) {
    if (!builder.getOrderByClauses().isEmpty()) {
      sql.append(" ORDER BY ")
          .append(
              builder.getOrderByClauses().stream()
                  .map(
                      o -> {
                        String column = o.getColumn();
                        if (selectAliases.contains(column)
                            || column.contains(".")
                            || column.contains("(")) {
                          return column + " " + o.getDirection().getSql();
                        } else {
                          return builder.getFromAlias()
                              + "."
                              + column
                              + " "
                              + o.getDirection().getSql();
                        }
                      })
                  .collect(Collectors.joining(", ")));
    }
  }

  /** Nối các mệnh đề LIMIT và OFFSET vào câu lệnh SQL. */
  private void appendLimitOffset(StringBuilder sql) {
    if (builder.getLimit() != null) {
      sql.append(" LIMIT ").append(builder.getLimit());
    }
    if (builder.getOffset() != null) {
      sql.append(" OFFSET ").append(builder.getOffset());
    }
  }
}
