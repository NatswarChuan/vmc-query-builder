package io.github.natswarchuan.vmc.core.query.clause;

import java.util.Map;
import lombok.Getter;

/**
 * Đại diện cho một câu lệnh SQL đã được chuẩn bị để thực thi.
 *
 * <p>Lớp này đóng gói chuỗi SQL cuối cùng và một Map chứa các tham số tương ứng. Việc tách biệt câu
 * lệnh SQL và các tham số giúp ngăn chặn các cuộc tấn công SQL Injection.
 *
 * @author NatswarChuan
 */
@Getter
public class PreparedQuery {
  /** Chuỗi câu lệnh SQL hoàn chỉnh. */
  private final String sql;

  /** Một Map chứa các tham số cho câu lệnh SQL, với key là tên tham số. */
  private final Map<String, Object> params;

  /**
   * Khởi tạo một đối tượng PreparedQuery mới.
   *
   * @param sql Chuỗi SQL.
   * @param params Map các tham số.
   */
  public PreparedQuery(String sql, Map<String, Object> params) {
    this.sql = sql;
    this.params = params;
  }
}
