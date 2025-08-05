package io.github.natswarchuan.vmc.core.query.clause;

import io.github.natswarchuan.vmc.core.query.enums.VMCLogicalOperator;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import lombok.Getter;

/**
 * Đại diện cho một điều kiện đơn lẻ trong mệnh đề WHERE của một câu lệnh SQL.
 *
 * <p>Lớp này là một cấu trúc dữ liệu không thay đổi (immutable) chứa tất cả thông tin về một điều
 * kiện, bao gồm toán tử logic để kết nối với điều kiện trước đó (AND/OR), tên cột, toán tử so sánh,
 * và giá trị.
 * 
 * @author NatswarChuan
 */
@Getter
public class WhereClause {
  /** Toán tử logic (AND/OR) để kết nối với mệnh đề trước đó. */
  private final VMCLogicalOperator conjunction;

  /** Tên của cột trong điều kiện. */
  private final String column;

  /** Toán tử so sánh SQL (ví dụ: =, &lt;&gt;, LIKE). */
  private final VMCSqlOperator operator;

  /** Giá trị để so sánh với cột. */
  private final Object value;

  /**
   * Khởi tạo một đối tượng WhereClause mới.
   *
   * @param conjunction Toán tử logic (AND/OR).
   * @param column Tên cột.
   * @param operator Toán tử so sánh.
   * @param value Giá trị so sánh.
   */
  public WhereClause(
      VMCLogicalOperator conjunction, String column, VMCSqlOperator operator, Object value) {
    this.conjunction = conjunction;
    this.column = column;
    this.operator = operator;
    this.value = value;
  }
}
