package io.github.natswarchuan.vmc.core.query.clause;

import io.github.natswarchuan.vmc.core.query.enums.VMCSortDirection;
import lombok.Getter;

/**
 * Đại diện cho một mệnh đề ORDER BY trong một câu lệnh SQL.
 *
 * <p>Lớp này là một cấu trúc dữ liệu không thay đổi (immutable) chứa tên cột và hướng sắp xếp (tăng
 * dần hoặc giảm dần).
 *
 * @author NatswarChuan
 */
@Getter
public class OrderByClause {
  /** Tên của cột cần sắp xếp. */
  private final String column;

  /** Hướng sắp xếp (ASC hoặc DESC). */
  private final VMCSortDirection direction;

  /**
   * Khởi tạo một đối tượng OrderByClause mới.
   *
   * @param column Tên cột để sắp xếp.
   * @param direction Hướng sắp xếp.
   */
  public OrderByClause(String column, VMCSortDirection direction) {
    this.column = column;
    this.direction = direction;
  }
}
