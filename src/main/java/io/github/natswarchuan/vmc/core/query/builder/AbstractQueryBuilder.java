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
 * Lớp trừu tượng cơ sở chứa trạng thái và các phương thức fluent API để xây dựng câu truy vấn.
 *
 * <p>Lớp này đóng vai trò là nền tảng cho các lớp query builder cụ thể, cung cấp các thuộc tính
 * chung để lưu trữ các thành phần của một câu lệnh SQL (ví dụ: mệnh đề select, where, order by) và
 * các phương thức để xây dựng chúng một cách tuần tự (method chaining).
 *
 * @param <T> Kiểu của lớp builder cụ thể kế thừa từ lớp này, để đảm bảo tính fluent của API.
 * @author NatswarChuan
 */
@Getter
@SuppressWarnings("unchecked")
public abstract class AbstractQueryBuilder<T extends AbstractQueryBuilder<T>> {

  /** Lớp thực thể gốc của truy vấn. */
  protected Class<? extends Model> modelClass;

  /** Tên bảng cơ sở dữ liệu của thực thể gốc. */
  protected String fromTable;

  /** Bí danh (alias) cho bảng gốc trong câu lệnh SQL. */
  protected String fromAlias;

  /** Danh sách các cột sẽ được chọn trong mệnh đề SELECT. */
  protected final List<String> selectColumns = new ArrayList<>();

  /** Danh sách các điều kiện trong mệnh đề WHERE. */
  protected final List<WhereClause> whereClauses = new ArrayList<>();

  /** Danh sách các quy tắc sắp xếp trong mệnh đề ORDER BY. */
  protected final List<OrderByClause> orderByClauses = new ArrayList<>();

  /** Danh sách các cột trong mệnh đề GROUP BY. */
  protected final List<String> groupByColumns = new ArrayList<>();

  /** Giá trị cho mệnh đề LIMIT. */
  protected Integer limit;

  /** Giá trị cho mệnh đề OFFSET. */
  protected Integer offset;

  /** Danh sách các mệnh đề JOIN. */
  protected final List<JoinClause> joinClauses = new ArrayList<>();

  /** Danh sách tên các mối quan hệ cần được tải ngay lập tức (eager loading). */
  protected final List<String> withRelations = new ArrayList<>();

  /**
   * Khởi tạo một builder mới với lớp Model và bí danh được chỉ định.
   *
   * @param modelClass Lớp thực thể gốc cho truy vấn.
   * @param alias Bí danh sẽ được sử dụng cho bảng gốc.
   */
  protected AbstractQueryBuilder(Class<? extends Model> modelClass, String alias) {
    this.modelClass = modelClass;
    EntityMetadata metadata = MetadataCache.getMetadata(modelClass);
    this.fromTable = metadata.getTableName();
    this.fromAlias = alias;
  }

  /**
   * Chỉ định các cột cần chọn trong mệnh đề SELECT.
   *
   * <p>Lưu ý: Việc gọi phương thức này sẽ xóa mọi cột đã được chọn trước đó.
   *
   * @param columns Danh sách các cột cần chọn.
   * @return Chính instance builder này để cho phép gọi chuỗi (method chaining).
   */
  public T select(String... columns) {
    this.selectColumns.clear();
    this.selectColumns.addAll(Arrays.asList(columns));
    return (T) this;
  }

  /**
   * Thêm một điều kiện vào mệnh đề WHERE, được nối với điều kiện trước đó bằng toán tử AND.
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
   * Thêm một điều kiện vào mệnh đề WHERE, được nối với điều kiện trước đó bằng toán tử OR.
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
   * Thêm một điều kiện {@code WHERE ... IN (...)} vào truy vấn.
   *
   * @param column Tên cột.
   * @param values Một collection chứa các giá trị để kiểm tra.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T whereIn(String column, Collection<?> values) {
    this.whereClauses.add(
        new WhereClause(VMCLogicalOperator.AND, column, VMCSqlOperator.IN, values));
    return (T) this;
  }

  /**
   * Thêm một hoặc nhiều cột vào mệnh đề GROUP BY.
   *
   * @param columns Các cột để nhóm kết quả.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T groupBy(String... columns) {
    this.groupByColumns.addAll(Arrays.asList(columns));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề LIMIT để giới hạn số lượng bản ghi trả về.
   *
   * @param limit Số lượng bản ghi tối đa.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T limit(int limit) {
    this.limit = limit;
    return (T) this;
  }

  /**
   * Thêm một mệnh đề OFFSET để chỉ định vị trí bắt đầu lấy bản ghi.
   *
   * @param offset Vị trí bắt đầu (bản ghi đầu tiên có offset là 0).
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T offset(int offset) {
    this.offset = offset;
    return (T) this;
  }

  /**
   * Thêm một quy tắc sắp xếp vào mệnh đề ORDER BY.
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
   * Chỉ định các mối quan hệ cần được tải ngay lập tức (eager loading).
   *
   * <p>Khi được gọi, framework sẽ tự động thêm các mệnh đề JOIN cần thiết để tải dữ liệu của các
   * mối quan hệ này trong cùng một truy vấn.
   *
   * @param relations Tên các trường (field) đại diện cho mối quan hệ cần tải.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public T with(String... relations) {
    this.withRelations.addAll(Arrays.asList(relations));
    return (T) this;
  }

  /**
   * Thêm một mệnh đề JOIN tùy chỉnh vào truy vấn.
   *
   * @param type Loại JOIN (ví dụ: INNER, LEFT, RIGHT).
   * @param table Bảng cần join.
   * @param alias Bí danh cho bảng join.
   * @param first Vế đầu tiên của điều kiện ON (ví dụ: 'users.id').
   * @param operator Toán tử so sánh trong điều kiện ON (thường là '=').
   * @param second Vế thứ hai của điều kiện ON (ví dụ: 'posts.user_id').
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
