package io.github.natswarchuan.vmc.core.query.enums;

/**
 * Định nghĩa các loại mệnh đề JOIN được hỗ trợ trong các câu lệnh SQL.
 *
 * @author NatswarChuan
 */
public enum VMCSqlJoinType {
  /**
   * Trả về các bản ghi có giá trị khớp trong cả hai bảng. Tương đương với INNER JOIN trong nhiều hệ
   * quản trị CSDL.
   */
  JOIN("JOIN"),

  /** Trả về tất cả các bản ghi từ bảng bên trái và các bản ghi khớp từ bảng bên phải. */
  LEFT_JOIN("LEFT JOIN"),

  /** Trả về tất cả các bản ghi từ bảng bên phải và các bản ghi khớp từ bảng bên trái. */
  RIGHT_JOIN("RIGHT JOIN"),

  /** Trả về các bản ghi có giá trị khớp trong cả hai bảng. */
  INNER_JOIN("INNER JOIN"),

  /** Trả về tất cả các bản ghi khi có sự khớp trong bảng bên trái hoặc bên phải. */
  FULL_JOIN("FULL JOIN");

  private final String sql;

  VMCSqlJoinType(String sql) {
    this.sql = sql;
  }

  /**
   * Lấy chuỗi biểu diễn SQL của loại JOIN.
   *
   * @return Chuỗi SQL tương ứng (ví dụ: "LEFT JOIN").
   */
  public String getSql() {
    return sql;
  }

  @Override
  public String toString() {
    return this.sql;
  }
}
