package io.github.natswarchuan.vmc.core.dto;

import io.github.natswarchuan.vmc.core.exception.VMCExceptionHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một cấu trúc phản hồi lỗi chuẩn được trả về cho client.
 *
 * <p>Lớp này được sử dụng để đóng gói thông tin chi tiết về một lỗi xảy ra trên máy chủ, bao gồm mã
 * trạng thái HTTP, một thông điệp lỗi rõ ràng và dấu thời gian của lỗi.
 *
 * @see VMCExceptionHandler
 * 
 * @author NatswarChuan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
  /** Mã trạng thái HTTP của lỗi (ví dụ: 404, 500). */
  private int status;

  /** Một thông điệp mô tả lỗi một cách ngắn gọn, dễ hiểu. */
  private String message;

  /** Dấu thời gian (timestamp) khi lỗi xảy ra, tính bằng mili giây từ epoch. */
  private long timestamp;
}
