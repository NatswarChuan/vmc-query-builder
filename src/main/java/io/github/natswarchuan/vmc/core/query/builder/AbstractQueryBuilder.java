package io.github.natswarchuan.vmc.core.query.builder;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.clause.OrderByClause;
import io.github.natswarchuan.vmc.core.query.clause.WhereClause;
import io.github.natswarchuan.vmc.core.query.enums.VMCLogicalOperator;
import io.github.natswarchuan.vmc.core.query.enums.VMCSortDirection;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * Lớp trừu tượng chứa trạng thái và các phương thức fluent API để xây dựng câu truy vấn.
 *
 * @param <T> Kiểu của lớp builder cụ thể (ví dụ: VMCQueryBuilder).
 */
@Getter
@SuppressWarnings("unchecked")
public abstract class AbstractQueryBuilder<T extends AbstractQueryBuilder<T>> {

  protected Class<? extends Model> modelClass;
  protected String fromTable;
  protected String fromAlias;
  protected final List<String> selectColumns = new ArrayList<>();
  protected final List<WhereClause> whereClauses = new ArrayList<>();
  protected final List<OrderByClause> orderByClauses = new ArrayList<>();
  protected final List<String> groupByColumns = new ArrayList<>();
  protected Integer limit;
  protected Integer offset;
  protected final List<JoinClause> joinClauses = new ArrayList<>();
  protected final List<String> withRelations = new ArrayList<>();

  /**
   * Khởi tạo builder với lớp Model và alias.
   *
   * @param modelClass Lớp thực thể gốc.
   * @param alias Bí danh cho bảng gốc.
   */
  protected AbstractQueryBuilder(Class<? extends Model> modelClass, String alias) {
    this.modelClass = modelClass;
    EntityMetadata metadata = MetadataCache.getMetadata(modelClass);
    this.fromTable = metadata.getTableName();
    this.fromAlias = alias;
  }

  /**
   * Chỉ định các cột cần chọn trong truy vấn.
   *
   * @param columns Danh sách các cột cần chọn.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T select(String... columns) {
    this.selectColumns.clear();
    this.selectColumns.addAll(Arrays.asList(columns));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề WHERE với toán tử AND.
   *
   * @param column Tên cột.
   * @param operator Toán tử so sánh (ví dụ: EQUAL, GREATER_THAN).
   * @param value Giá trị để so sánh.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T where(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.AND, column, operator, value));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề WHERE với toán tử OR.
   *
   * @param column Tên cột.
   * @param operator Toán tử so sánh.
   * @param value Giá trị để so sánh.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T orWhere(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.OR, column, operator, value));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề WHERE ... IN (...).
   *
   * @param column Tên cột.
   * @param values Một collection các giá trị để kiểm tra.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T whereIn(String column, Collection<?> values) {
    this.whereClauses.add(
        new WhereClause(VMCLogicalOperator.AND, column, VMCSqlOperator.IN, values));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề GROUP BY.
   *
   * @param columns Các cột để nhóm kết quả.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T groupBy(String... columns) {
    this.groupByColumns.addAll(Arrays.asList(columns));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề LIMIT.
   *
   * @param limit Số lượng bản ghi tối đa cần trả về.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T limit(int limit) {
    this.limit = limit;
    return (T) this;
  }

  /**
   * Thêm một mệnh đề OFFSET.
   *
   * @param offset Vị trí bắt đầu lấy bản ghi.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T offset(int offset) {
    this.offset = offset;
    return (T) this;
  }

  /**
   * Thêm một mệnh đề ORDER BY.
   *
   * @param column Cột cần sắp xếp.
   * @param direction Hướng sắp xếp (ASC hoặc DESC).
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T orderBy(String column, VMCSortDirection direction) {
    this.orderByClauses.add(new OrderByClause(column, direction));
    return (T) this;
  }

  /**
   * Chỉ định các mối quan hệ cần được tải cùng (eager loading).
   *
   * @param relations Tên các trường quan hệ cần tải.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T with(String... relations) {
    this.withRelations.addAll(Arrays.asList(relations));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề JOIN tùy chỉnh.
   *
   * @param type Loại JOIN (INNER, LEFT, RIGHT).
   * @param table Bảng cần join.
   * @param alias Bí danh cho bảng join.
   * @param first Điều kiện join thứ nhất.
   * @param operator Toán tử điều kiện join.
   * @param second Điều kiện join thứ hai.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T join(
      VMCSqlJoinType type,
      String table,
      String alias,
      String first,
      String operator,
      String second) {
    this.joinClauses.add(
        new JoinClause(type, table, alias, first, operator, second, null, null, null));
    return (T) this;
  }
}
