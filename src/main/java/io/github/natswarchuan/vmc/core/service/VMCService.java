package io.github.natswarchuan.vmc.core.service;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.builder.Paginator;
import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các hoạt động nghiệp vụ (business operations) tiêu chuẩn cho một dịch vụ
 * (service).
 *
 * <p>Interface này cung cấp một bộ các phương thức CRUD (Tạo, Đọc, Cập nhật, Xóa) và các hoạt động
 * phổ biến khác ở tầng service, giúp trừu tượng hóa logic truy cập dữ liệu từ các repository.
 *
 * @param <T> Kiểu của thực thể (entity) mà service này quản lý.
 * @param <ID> Kiểu của khóa chính (primary key) của thực thể. * @author NatswarChuan
 */
public interface VMCService<T extends Model, ID> {

  /**
   * Lấy tất cả các instance của thực thể.
   *
   * @return một danh sách chứa tất cả các thực thể.
   */
  List<T> findAll();

  /**
   * Lấy một "trang" (page) các thực thể.
   *
   * @param page Số trang hiện tại (bắt đầu từ 1).
   * @param perPage Số lượng mục trên mỗi trang.
   * @return một đối tượng {@link Paginator} chứa dữ liệu và thông tin phân trang.
   */
  Paginator<T> findAll(int page, int perPage);

  /**
   * Lấy một thực thể bằng ID của nó.
   *
   * @param id ID của thực thể.
   * @return một {@link Optional} chứa thực thể nếu tìm thấy, ngược lại là rỗng.
   */
  Optional<T> findById(ID id);

  /**
   * Lấy một thực thể bằng ID và chuyển đổi nó thành một DTO.
   *
   * @param id ID của thực thể.
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return một {@link Optional} chứa DTO nếu tìm thấy, ngược lại là rỗng.
   */
  <D extends BaseDto<T, D>> Optional<D> findByIdGetDto(ID id, Class<D> dtoClass);

  /**
   * Lấy tất cả các thực thể và chuyển đổi chúng thành một danh sách các DTO.
   *
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return một danh sách các DTO.
   */
  <D extends BaseDto<T, D>> List<D> findAllGetDtos(Class<D> dtoClass);

  /**
   * Lấy một "trang" các thực thể và chuyển đổi chúng thành DTO.
   *
   * @param page Số trang hiện tại.
   * @param perPage Số lượng mục trên mỗi trang.
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return một đối tượng {@link Paginator} chứa dữ liệu DTO.
   */
  <D extends BaseDto<T, D>> Paginator<D> findAllGetDtos(int page, int perPage, Class<D> dtoClass);

  /**
   * Tạo một thực thể mới.
   *
   * @param entity Thực thể cần tạo.
   * @return Thực thể đã được lưu.
   */
  T create(T entity);

  /**
   * Tạo một thực thể mới với các tùy chọn lưu tùy chỉnh.
   *
   * @param entity Thực thể cần tạo.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @return Thực thể đã được lưu.
   */
  T create(T entity, SaveOptions options);

  /**
   * Tạo một loạt các thực thể mới.
   *
   * @param entities Một {@link Iterable} chứa các thực thể cần tạo.
   * @return một danh sách các thực thể đã được lưu.
   */
  List<T> createAll(Iterable<T> entities);

  /**
   * Tạo một thực thể mới từ một DTO.
   *
   * @param dto DTO chứa dữ liệu để tạo thực thể.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật với dữ liệu từ thực thể mới được tạo.
   */
  <D extends BaseDto<T, D>> D createFromDto(D dto);

  /**
   * Tạo một thực thể mới từ một DTO với các tùy chọn lưu tùy chỉnh.
   *
   * @param dto DTO chứa dữ liệu để tạo thực thể.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật.
   */
  <D extends BaseDto<T, D>> D createFromDto(D dto, SaveOptions options);

  /**
   * Cập nhật một thực thể đã tồn tại.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param entity Thực thể với các thông tin đã được cập nhật.
   * @return Thực thể sau khi đã cập nhật.
   */
  T update(ID id, T entity);

  /**
   * Cập nhật một thực thể đã tồn tại với các tùy chọn lưu tùy chỉnh.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param entity Thực thể với các thông tin đã được cập nhật.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @return Thực thể sau khi đã cập nhật.
   */
  T update(ID id, T entity, SaveOptions options);

  /**
   * Cập nhật một thực thể đã tồn tại từ một DTO.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param dto DTO chứa dữ liệu cập nhật.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật.
   */
  <D extends BaseDto<T, D>> D updateFromDto(ID id, D dto);

  /**
   * Cập nhật một thực thể đã tồn tại từ một DTO với các tùy chọn lưu tùy chỉnh.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param dto DTO chứa dữ liệu cập nhật.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật.
   */
  <D extends BaseDto<T, D>> D updateFromDto(ID id, D dto, SaveOptions options);

  /**
   * Lưu một thực thể (tạo mới hoặc cập nhật).
   *
   * @param entity Thực thể cần lưu.
   * @return Thực thể đã được lưu.
   */
  T save(T entity);

  /**
   * Lưu một thực thể (tạo mới hoặc cập nhật) với các tùy chọn lưu tùy chỉnh.
   *
   * @param entity Thực thể cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @return Thực thể đã được lưu.
   */
  T save(T entity, SaveOptions options);

  /**
   * Lưu một tập hợp các thực thể.
   *
   * @param entities Một {@link Iterable} chứa các thực thể cần lưu.
   * @return một danh sách các thực thể đã được lưu.
   */
  List<T> saveAll(Iterable<T> entities);

  /**
   * Lưu một thực thể từ một DTO.
   *
   * @param dto DTO chứa dữ liệu cần lưu.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật.
   */
  <D extends BaseDto<T, D>> D saveFromDto(D dto);

  /**
   * Lưu một thực thể từ một DTO với các tùy chọn lưu tùy chỉnh.
   *
   * @param dto DTO chứa dữ liệu cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật.
   */
  <D extends BaseDto<T, D>> D saveFromDto(D dto, SaveOptions options);

  /**
   * Xóa một thực thể bằng ID của nó.
   *
   * @param id ID của thực thể cần xóa.
   */
  void deleteById(ID id);

  /**
   * Xóa một thực thể bằng ID với các tùy chọn.
   *
   * @param id ID của thực thể cần xóa.
   * @param options Các tùy chọn để kiểm soát hành vi xóa.
   */
  void deleteById(ID id, RemoveOptions options);

  /** Xóa một thực thể đã cho. */
  void delete(T entity);

  /**
   * Xóa một thực thể với các tùy chọn tùy chỉnh.
   *
   * @param entity Thực thể cần xóa.
   * @param options Các tùy chọn để kiểm soát hành vi xóa.
   */
  void delete(T entity, RemoveOptions options);

  /**
   * Đếm tổng số thực thể.
   *
   * @return tổng số thực thể.
   */
  long count();

  /**
   * Tạo một thực thể mới từ một DTO và trả về một DTO khác.
   *
   * @param requestDto DTO chứa dữ liệu để tạo thực thể.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(
      I requestDto, Class<O> responseDtoClass);

  /**
   * Tạo một thực thể mới từ một DTO với các tùy chọn lưu tùy chỉnh và trả về một DTO khác.
   *
   * @param requestDto DTO chứa dữ liệu để tạo thực thể.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(
      I requestDto, Class<O> responseDtoClass, SaveOptions options);

  /**
   * Cập nhật một thực thể đã tồn tại từ một DTO và trả về một DTO khác.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param requestDto DTO chứa dữ liệu cập nhật.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(
      ID id, I requestDto, Class<O> responseDtoClass);

  /**
   * Cập nhật một thực thể đã tồn tại từ một DTO với các tùy chọn lưu tùy chỉnh và trả về một DTO
   * khác.
   *
   * @param id ID của thực thể cần cập nhật.
   * @param requestDto DTO chứa dữ liệu cập nhật.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(
      ID id, I requestDto, Class<O> responseDtoClass, SaveOptions options);

  /**
   * Lưu một thực thể từ một DTO và trả về một DTO khác.
   *
   * @param requestDto DTO chứa dữ liệu cần lưu.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O saveFromDto(
      I requestDto, Class<O> responseDtoClass);

  /**
   * Lưu một thực thể từ một DTO với các tùy chọn lưu tùy chỉnh và trả về một DTO khác.
   *
   * @param requestDto DTO chứa dữ liệu cần lưu.
   * @param responseDtoClass Lớp của DTO trả về.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <I> Kiểu của DTO đầu vào.
   * @param <O> Kiểu của DTO đầu ra.
   * @return DTO trả về đã được cập nhật.
   */
  <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O saveFromDto(
      I requestDto, Class<O> responseDtoClass, SaveOptions options);
}
