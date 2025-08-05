package io.github.natswarchuan.vmc.core.query.enums;

/** Định nghĩa các hướng sắp xếp cho mệnh đề ORDER BY trong một câu lệnh SQL. 
 * 
 * @author NatswarChuan
*/
public enum VMCSortDirection {
  /** Sắp xếp theo thứ tự tăng dần (Ascending). */
  ASC("ASC"),

  /** Sắp xếp theo thứ tự giảm dần (Descending). */
  DESC("DESC");

  private final String sql;

  VMCSortDirection(String sql) {
    this.sql = sql;
  }

  /**
   * Lấy chuỗi biểu diễn SQL của hướng sắp xếp.
   *
   * @return Chuỗi SQL (ví dụ: "ASC", "DESC").
   */
  public String getSql() {
    return sql;
  }

  @Override
  public String toString() {
    return this.sql;
  }
}
