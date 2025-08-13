package io.github.natswarchuan.vmc.core.query.helper;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.clause.OrderByClause;
import io.github.natswarchuan.vmc.core.query.clause.PreparedQuery;
import io.github.natswarchuan.vmc.core.query.clause.WhereClause;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xây dựng các chuỗi câu lệnh SQL và các tham số tương ứng từ các thành phần truy vấn.
 *
 * <p>Lớp này đóng vai trò là một "thợ xây" cho các câu lệnh SQL, chuyển đổi các cấu hình trừu tượng
 * (như danh sách cột, điều kiện WHERE, mệnh đề JOIN) từ một query builder thành một đối tượng
 * {@link PreparedQuery} cụ thể, sẵn sàng để được thực thi bởi một trình thực thi truy vấn như
 * MyBatis. Nó đảm bảo rằng các câu lệnh được tạo ra là hợp lệ về mặt cú pháp và các tham số được
 * quản lý một cách an toàn để chống lại các cuộc tấn công SQL Injection.
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

  /**
   * Khởi tạo một instance mới của SqlBuilder với tất cả các thành phần cần thiết.
   *
   * @param modelClass Lớp thực thể gốc của truy vấn (bảng FROM).
   * @param fromAlias Bí danh sẽ được sử dụng cho bảng gốc.
   * @param selectColumns Danh sách các cột sẽ được chọn trong mệnh đề SELECT.
   * @param whereClauses Danh sách các điều kiện trong mệnh đề WHERE.
   * @param orderByClauses Danh sách các quy tắc sắp xếp trong mệnh đề ORDER BY.
   * @param groupByColumns Danh sách các cột trong mệnh đề GROUP BY.
   * @param limit Giá trị cho mệnh đề LIMIT (có thể là null).
   * @param offset Giá trị cho mệnh đề OFFSET (có thể là null).
   * @param joinClauses Danh sách các mệnh đề JOIN.
   * @param withRelations Danh sách tên các mối quan hệ cần tải ngay (không được sử dụng trực tiếp
   *     trong lớp này nhưng là một phần của trạng thái builder).
   */
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

  /**
   * Xây dựng câu lệnh SQL SELECT hoàn chỉnh.
   *
   * <p>Phương thức này tổng hợp tất cả các thành phần đã được cấu hình (select, from, join, where,
   * group by, order by, limit, offset) để tạo ra một câu lệnh SELECT cuối cùng. Nếu không có cột
   * nào được chỉ định, nó sẽ tự động thêm các cột mặc định.
   *
   * @return Một đối tượng {@link PreparedQuery} chứa chuỗi SQL và các tham số.
   */
  public PreparedQuery build() {
    EntityMetadata mainMetadata = MetadataCache.getMetadata(this.modelClass);
    if (this.selectColumns.isEmpty()) {
      addDefaultSelects(mainMetadata);
    }

    List<String> processedSelectColumns = new ArrayList<>();

    if (this.selectColumns.isEmpty()) {

      addDefaultSelects(mainMetadata);
      processedSelectColumns.addAll(this.selectColumns);
    } else {
      for (String column : this.selectColumns) {
        if (column.contains("(") || column.toLowerCase().contains(" as ")) {
          processedSelectColumns.add(column);
        } else {
          String baseColumnName =
              column.contains(".") ? column.substring(column.lastIndexOf(".") + 1) : column;
          processedSelectColumns.add(
              String.format(
                  "%s.%s AS %s_%s",
                  this.fromAlias, baseColumnName, this.fromAlias, baseColumnName));
        }
      }
    }

    StringBuilder sql =
        new StringBuilder("SELECT ").append(String.join(", ", processedSelectColumns));
    sql.append(" FROM ").append(mainMetadata.getTableName()).append(" AS ").append(this.fromAlias);

    appendJoins(sql);
    Map<String, Object> params = buildWhereClause(sql, this.fromAlias);
    appendGroupBy(sql);
    appendOrderBy(sql);
    appendLimitOffset(sql);

    return new PreparedQuery(sql.toString(), params);
  }

  /**
   * Xây dựng câu lệnh SQL để đếm số lượng bản ghi.
   *
   * <p>Phương thức này tạo một câu lệnh {@code COUNT}. Nếu có mệnh đề {@code GROUP BY}, nó sẽ sử
   * dụng một subquery để đếm đúng số lượng nhóm. Nếu không, nó sẽ sử dụng {@code COUNT(DISTINCT
   * primary_key)} để đảm bảo kết quả chính xác khi có các mệnh đề JOIN.
   *
   * @return Một đối tượng {@link PreparedQuery} chứa chuỗi SQL và các tham số.
   */
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

  /**
   * Thêm các cột select mặc định nếu người dùng không chỉ định.
   *
   * <p>Mặc định sẽ là {@code SELECT alias.column1 as alias_column1, ...} cho thực thể chính và tất
   * cả các thực thể được join.
   *
   * @param mainMetadata Metadata của thực thể chính.
   */
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
   * Thêm tất cả các cột cần thiết cho một entity vào danh sách SELECT.
   *
   * <p>Bao gồm các cột dữ liệu (`@VMCColumn`) và các cột khóa ngoại (`@VMCJoinColumn`). Các cột
   * được đặt bí danh theo mẫu {@code alias_columnName} để tránh xung đột tên.
   *
   * @param metadata Metadata của thực thể cần thêm cột.
   * @param alias Bí danh của bảng tương ứng.
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

  /**
   * Nối các mệnh đề JOIN vào câu lệnh SQL.
   *
   * @param sql StringBuilder chứa câu lệnh SQL đang được xây dựng.
   */
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

  /**
   * Xây dựng và nối mệnh đề WHERE vào câu lệnh SQL, đồng thời tạo ra map các tham số.
   *
   * @param sql StringBuilder chứa câu lệnh SQL đang được xây dựng.
   * @param alias Bí danh của bảng chính, được sử dụng cho các cột không có bí danh rõ ràng.
   * @return Một {@code Map} chứa các tham số cho mệnh đề WHERE.
   */
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
            sql.append("1 = 0"); // Điều kiện luôn sai nếu collection IN rỗng
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

  /**
   * Nối mệnh đề GROUP BY vào câu lệnh SQL.
   *
   * @param sql StringBuilder chứa câu lệnh SQL đang được xây dựng.
   */
  private void appendGroupBy(StringBuilder sql) {
    if (!groupByColumns.isEmpty()) {
      sql.append(" GROUP BY ")
          .append(
              groupByColumns.stream()
                  .map(c -> c.contains(".") || c.contains("(") ? c : fromAlias + "." + c)
                  .collect(Collectors.joining(", ")));
    }
  }

  /**
   * Nối mệnh đề ORDER BY vào câu lệnh SQL.
   *
   * @param sql StringBuilder chứa câu lệnh SQL đang được xây dựng.
   */
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

  /**
   * Nối các mệnh đề LIMIT và OFFSET vào câu lệnh SQL.
   *
   * @param sql StringBuilder chứa câu lệnh SQL đang được xây dựng.
   */
  private void appendLimitOffset(StringBuilder sql) {
    if (this.limit != null) {
      sql.append(" LIMIT ").append(this.limit);
    }
    if (this.offset != null) {
      sql.append(" OFFSET ").append(this.offset);
    }
  }
}
