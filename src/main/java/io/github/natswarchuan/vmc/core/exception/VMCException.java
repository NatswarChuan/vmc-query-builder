package io.github.natswarchuan.vmc.core.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

/**
 * Lớp ngoại lệ (exception) tùy chỉnh cơ sở cho toàn bộ ứng dụng VMC.
 *
 * <p>Đây là một ngoại lệ không kiểm tra (unchecked exception) kế thừa từ {@link RuntimeException}.
 * Nó được thiết kế để mang theo một mã trạng thái HTTP {@link HttpStatus}, cho phép xử lý lỗi một
 * cách nhất quán và trả về các phản hồi HTTP có ý nghĩa cho client.
 *
 * @see VMCExceptionHandler
 * 
 * @author NatswarChuan
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VMCException extends RuntimeException {

  /** Mã trạng thái HTTP liên quan đến ngoại lệ này. */
  private HttpStatus status;

  /**
   * Khởi tạo một VMCException mới với mã trạng thái và thông điệp chi tiết.
   *
   * @param status Mã trạng thái HTTP.
   * @param message Thông điệp mô tả lỗi.
   */
  public VMCException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  /**
   * Khởi tạo một VMCException mới với mã trạng thái, thông điệp chi tiết và nguyên nhân gốc.
   *
   * @param status Mã trạng thái HTTP.
   * @param message Thông điệp mô tả lỗi.
   * @param cause Ngoại lệ gốc đã gây ra lỗi này.
   */
  public VMCException(HttpStatus status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }
}
