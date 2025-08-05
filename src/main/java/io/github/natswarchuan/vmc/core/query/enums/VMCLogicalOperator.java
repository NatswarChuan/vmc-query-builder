package io.github.natswarchuan.vmc.core.query.enums;

/**
 * Định nghĩa các toán tử logic được sử dụng để kết hợp các mệnh đề WHERE trong một câu lệnh SQL.
 * 
 * @author NatswarChuan
 */
public enum VMCLogicalOperator {
  /**
   * Đại diện cho toán tử logic AND.
   *
   * <p>Kết quả là true nếu tất cả các điều kiện được kết nối đều true.
   */
  AND("AND"),

  /**
   * Đại diện cho toán tử logic OR.
   *
   * <p>Kết quả là true nếu ít nhất một trong các điều kiện được kết nối là true.
   */
  OR("OR");

  private final String sql;

  VMCLogicalOperator(String sql) {
    this.sql = sql;
  }

  /**
   * Lấy chuỗi biểu diễn SQL của toán tử logic.
   *
   * @return Chuỗi SQL (ví dụ: "AND", "OR").
   */
  public String getSql() {
    return sql;
  }

  @Override
  public String toString() {
    return this.sql;
  }
}
