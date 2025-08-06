package io.github.natswarchuan.vmc.core.query.builder.helper;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import java.util.List;

/**
 * Lớp tiện ích tĩnh để xử lý các thao tác lưu trữ (save).
 *
 * <p>Lớp này đóng vai trò là một cầu nối (facade) đơn giản, cung cấp các phương thức tĩnh để ủy
 * quyền các thao tác lưu DTO cho {@link VMCPersistenceManager}. Điều này cho phép truy cập chức
 * năng lưu trữ từ các thành phần không được quản lý bởi Spring một cách thuận tiện.
 *
 * @author NatswarChuan
 */
public class PersistenceHandler {

  /**
   * Lưu một đối tượng DTO và thực thể tương ứng của nó bằng cách sử dụng các tùy chọn lưu mặc định.
   *
   * @param <E> Kiểu của Entity, phải kế thừa từ {@link Model}.
   * @param <D> Kiểu của DTO, phải triển khai {@link BaseDto}.
   * @param dto Đối tượng DTO cần lưu.
   * @return DTO đã được cập nhật với dữ liệu từ thực thể sau khi lưu (ví dụ: ID được tạo tự động).
   */
  public static <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto) {
    return saveDto(dto, new SaveOptions());
  }

  /**
   * Lưu một đối tượng DTO và thực thể tương ứng của nó với các tùy chọn lưu tùy chỉnh.
   *
   * @param <E> Kiểu của Entity, phải kế thừa từ {@link Model}.
   * @param <D> Kiểu của DTO, phải triển khai {@link BaseDto}.
   * @param dto Đối tượng DTO cần lưu.
   * @param saveOptions Các tùy chọn để kiểm soát hành vi lưu, ví dụ như cascade.
   * @return DTO đã được cập nhật với dữ liệu từ thực thể sau khi lưu.
   */
  public static <E extends Model, D extends BaseDto<E, D>> D saveDto(
      D dto, SaveOptions saveOptions) {
    VMCPersistenceManager persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    return persistenceManager.saveDto(dto, saveOptions);
  }

  /**
   * Lưu một danh sách các đối tượng DTO bằng cách sử dụng các tùy chọn lưu mặc định.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dtos Một {@code Iterable} chứa các DTO cần lưu.
   * @return Một danh sách các DTO đã được cập nhật sau khi lưu.
   */
  public static <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(Iterable<D> dtos) {
    return saveAllDtos(dtos, new SaveOptions());
  }

  /**
   * Lưu một danh sách các đối tượng DTO với các tùy chọn lưu tùy chỉnh.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dtos Một {@code Iterable} chứa các DTO cần lưu.
   * @param saveOptions Các tùy chọn để kiểm soát hành vi lưu.
   * @return Một danh sách các DTO đã được cập nhật sau khi lưu.
   */
  public static <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions saveOptions) {
    VMCPersistenceManager persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    return persistenceManager.saveAllDtos(dtos, saveOptions);
  }
}
