package io.github.natswarchuan.vmc.core.query.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.clause.OrderByClause;
import io.github.natswarchuan.vmc.core.query.clause.PreparedQuery;
import io.github.natswarchuan.vmc.core.query.clause.WhereClause;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

/**
 * Xây dựng các chuỗi câu lệnh SQL và các tham số tương ứng từ các thành phần truy vấn.
 *
 * @author NatswarChuan
 */
public class SqlBuilder {

  private final Class<? extends Model> modelClass;
  private final String fromAlias;
  private final List<String> selectColumns;
  private final List<WhereClause> whereClauses;
  private final List<OrderByClause> orderByClauses;
  private final List<String> groupByColumns;
  private final Integer limit;
  private final Integer offset;
  private final List<JoinClause> joinClauses;

  public SqlBuilder(
      Class<? extends Model> modelClass,
      String fromAlias,
      List<String> selectColumns,
      List<WhereClause> whereClauses,
      List<OrderByClause> orderByClauses,
      List<String> groupByColumns,
      Integer limit,
      Integer offset,
      List<JoinClause> joinClauses,
      List<String> withRelations) {
    this.modelClass = modelClass;
    this.fromAlias = fromAlias;
    this.selectColumns = selectColumns;
    this.whereClauses = whereClauses;
    this.orderByClauses = orderByClauses;
    this.groupByColumns = groupByColumns;
    this.limit = limit;
    this.offset = offset;
    this.joinClauses = joinClauses;
  }

  public PreparedQuery build() {
    EntityMetadata mainMetadata = MetadataCache.getMetadata(this.modelClass);
    if (this.selectColumns.isEmpty()) {
      addDefaultSelects(mainMetadata);
    }

    StringBuilder sql = new StringBuilder("SELECT ").append(String.join(", ", this.selectColumns));
    sql.append(" FROM ").append(mainMetadata.getTableName()).append(" AS ").append(this.fromAlias);

    appendJoins(sql);
    Map<String, Object> params = buildWhereClause(sql, this.fromAlias);
    appendGroupBy(sql);
    appendOrderBy(sql);
    appendLimitOffset(sql);

    return new PreparedQuery(sql.toString(), params);
  }

  public PreparedQuery buildCountQuery() {
    EntityMetadata mainMetadata = MetadataCache.getMetadata(this.modelClass);
    StringBuilder baseQuery = new StringBuilder();
    baseQuery
        .append(" FROM ")
        .append(mainMetadata.getTableName())
        .append(" AS ")
        .append(this.fromAlias);

    appendJoins(baseQuery);
    Map<String, Object> params = buildWhereClause(baseQuery, this.fromAlias);

    if (!groupByColumns.isEmpty()) {
      String groupByClause =
          groupByColumns.stream()
              .map(c -> c.contains(".") || c.contains("(") ? c : fromAlias + "." + c)
              .collect(Collectors.joining(", "));
      String subQuery = "SELECT 1" + baseQuery.toString() + " GROUP BY " + groupByClause;
      String finalQuery = "SELECT COUNT(*) AS count FROM (" + subQuery + ") AS count_subquery";
      return new PreparedQuery(finalQuery, params);
    } else {
      String pkColumn = mainMetadata.getPrimaryKeyColumnName();
      String pkColumnWithAlias = this.fromAlias + "." + pkColumn;
      String finalQuery =
          "SELECT COUNT(DISTINCT " + pkColumnWithAlias + ") AS count" + baseQuery.toString();
      return new PreparedQuery(finalQuery, params);
    }
  }

  private void addDefaultSelects(EntityMetadata mainMetadata) {

    addColumnsForEntity(mainMetadata, fromAlias);

    for (JoinClause join : joinClauses) {
      if (join.getRelatedClass() != null) {
        EntityMetadata relatedMetadata = MetadataCache.getMetadata(join.getRelatedClass());
        addColumnsForEntity(relatedMetadata, join.getAlias());
      }
    }
  }

  /**
   * Thêm tất cả các cột cần thiết cho một entity vào danh sách SELECT. Bao gồm các cột dữ liệu
   * (@VMCColumn) và các cột khóa ngoại (@VMCJoinColumn).
   */
  private void addColumnsForEntity(EntityMetadata metadata, String alias) {

    metadata
        .getFieldToColumnMap()
        .values()
        .forEach(
            columnName -> {
              String aliasedColumn =
                  String.format("%s.%s as %s_%s", alias, columnName, alias, columnName);
              if (!selectColumns.contains(aliasedColumn)) {
                selectColumns.add(aliasedColumn);
              }
            });

    metadata
        .getRelations()
        .values()
        .forEach(
            relMeta -> {
              if (relMeta.getJoinColumnName() != null) {
                String fkColumn = relMeta.getJoinColumnName();
                String aliasedFk =
                    String.format("%s.%s as %s_%s", alias, fkColumn, alias, fkColumn);
                if (!selectColumns.contains(aliasedFk)) {
                  selectColumns.add(aliasedFk);
                }
              }
            });
  }

  private void appendJoins(StringBuilder sql) {
    for (JoinClause join : this.joinClauses) {
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

  public Map<String, Object> buildWhereClause(StringBuilder sql, String alias) {
    Map<String, Object> params = new HashMap<>();
    if (!whereClauses.isEmpty()) {
      sql.append(" WHERE ");
      for (int i = 0; i < whereClauses.size(); i++) {
        WhereClause clause = whereClauses.get(i);
        if (i > 0) {
          sql.append(" ").append(clause.getConjunction().getSql()).append(" ");
        }

        String columnWithAlias =
            clause.getColumn().contains(".") || clause.getColumn().contains("(")
                ? clause.getColumn()
                : alias + "." + clause.getColumn();

        if (clause.getOperator() == VMCSqlOperator.IN && clause.getValue() instanceof Collection) {
          Collection<?> values = (Collection<?>) clause.getValue();
          if (values.isEmpty()) {
            sql.append("1 = 0");
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
        } else if (clause.getOperator() == VMCSqlOperator.IS_NULL
            || clause.getOperator() == VMCSqlOperator.IS_NOT_NULL) {
          sql.append(String.format("%s %s", columnWithAlias, clause.getOperator().getSql()));
        } else {
          String pName = "p" + params.size();
          params.put(pName, clause.getValue());
          sql.append(
              String.format(
                  "%s %s #{params.%s}", columnWithAlias, clause.getOperator().getSql(), pName));
        }
      }
    }
    return params;
  }

  private void appendGroupBy(StringBuilder sql) {
    if (!groupByColumns.isEmpty()) {
      sql.append(" GROUP BY ")
          .append(
              groupByColumns.stream()
                  .map(c -> c.contains(".") || c.contains("(") ? c : fromAlias + "." + c)
                  .collect(Collectors.joining(", ")));
    }
  }

  private void appendOrderBy(StringBuilder sql) {
    if (!orderByClauses.isEmpty()) {
      sql.append(" ORDER BY ");
      Set<String> selectAliases = new HashSet<>();
      for (String col : this.selectColumns) {
        String lowerCol = col.toLowerCase();
        int asIndex = lowerCol.lastIndexOf(" as ");
        if (asIndex != -1) {
          selectAliases.add(col.substring(asIndex + 4).trim());
        }
      }

      sql.append(
          orderByClauses.stream()
              .map(
                  o -> {
                    String column = o.getColumn();
                    if (selectAliases.contains(column)
                        || column.contains(".")
                        || column.contains("(")) {
                      return column + " " + o.getDirection().getSql();
                    } else {
                      return fromAlias + "." + column + " " + o.getDirection().getSql();
                    }
                  })
              .collect(Collectors.joining(", ")));
    }
  }

  private void appendLimitOffset(StringBuilder sql) {
    if (this.limit != null) {
      sql.append(" LIMIT ").append(this.limit);
    }
    if (this.offset != null) {
      sql.append(" OFFSET ").append(this.offset);
    }
  }
}
