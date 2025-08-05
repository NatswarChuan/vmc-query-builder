package io.github.natswarchuan.vmc.core.query.enums;

/**
 * Định nghĩa các toán tử so sánh được sử dụng trong mệnh đề WHERE của một câu lệnh SQL.
 *
 * @author NatswarChuan
 */
public enum VMCSqlOperator {
  /** Bằng (=). */
  EQUAL("="),

  /** Không bằng (&lt;&gt;). */
  NOT_EQUAL("<>"),

  /** Lớn hơn (&gt;). */
  GREATER_THAN(">"),

  /** Nhỏ hơn (&lt;). */
  LESS_THAN("<"),

  /** Lớn hơn hoặc bằng (&gt;=). */
  GREATER_THAN_OR_EQUAL(">="),

  /** Nhỏ hơn hoặc bằng (&lt;=). */
  LESS_THAN_OR_EQUAL("<="),

  /** So khớp với một mẫu (LIKE). */
  LIKE("LIKE"),

  /** Không so khớp với một mẫu (NOT LIKE). */
  NOT_LIKE("NOT LIKE"),

  /** Kiểm tra xem giá trị có nằm trong một tập hợp (IN). */
  IN("IN"),

  /** Kiểm tra xem giá trị có không nằm trong một tập hợp (NOT IN). */
  NOT_IN("NOT IN"),

  /** Kiểm tra giá trị có phải là NULL (IS NULL). */
  IS_NULL("IS NULL"),

  /** Kiểm tra giá trị có khác NULL (IS NOT NULL). */
  IS_NOT_NULL("IS NOT NULL");

  private final String sql;

  VMCSqlOperator(String sql) {
    this.sql = sql;
  }

  /**
   * Lấy chuỗi biểu diễn SQL của toán tử.
   *
   * @return Chuỗi SQL tương ứng (ví dụ: ">=", "LIKE").
   */
  public String getSql() {
    return sql;
  }

  @Override
  public String toString() {
    return this.sql;
  }
}
