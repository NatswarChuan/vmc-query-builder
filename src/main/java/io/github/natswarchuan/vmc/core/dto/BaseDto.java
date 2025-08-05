package io.github.natswarchuan.vmc.core.dto;

import io.github.natswarchuan.vmc.core.entity.Model;

/**
 * Định nghĩa một interface cơ sở cho các đối tượng truyền dữ liệu (Data Transfer Objects - DTO).
 *
 * <p>Interface này cung cấp một hợp đồng chuẩn cho việc chuyển đổi giữa các đối tượng thực thể
 * (Entity) và DTO. Việc triển khai interface này cho phép ánh xạ dữ liệu một cách nhất quán trong
 * toàn bộ ứng dụng.
 *
 * @param <E> Kiểu của lớp thực thể (Entity) mà DTO này tương ứng.
 * @param <D> Kiểu của chính lớp DTO triển khai interface này, để hỗ trợ fluent API
 * .
 * @author NatswarChuan
 */
public interface BaseDto<E extends Model, D extends BaseDto<E, D>> {

  /**
   * Chuyển đổi một đối tượng thực thể (Entity) thành một DTO.
   *
   * <p>Phương thức này nhận một thực thể và sao chép các thuộc tính liên quan vào đối tượng DTO
   * hiện tại.
   *
   * @param entity Đối tượng thực thể nguồn để chuyển đổi.
   * @return Một instance của DTO đã được điền dữ liệu từ thực thể.
   */
  D toDto(Model entity);

  /**
   * Chuyển đổi DTO này thành một đối tượng thực thể (Entity).
   *
   * <p>Phương thức này tạo ra một instance mới của thực thể và điền dữ liệu từ các thuộc tính của
   * DTO.
   *
   * @return Một instance mới của thực thể với dữ liệu từ DTO.
   */
  E toEntity();
}
