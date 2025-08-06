package io.github.natswarchuan.vmc.core.repository;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.builder.Paginator;
import java.util.List;
import java.util.Optional;

/**
 * Interface trung tâm cho các repository, cung cấp các chức năng CRUD (Tạo, Đọc, Cập nhật, Xóa)
 * chung và các phương thức truy cập dữ liệu cơ bản.
 *
 * <p>Các interface repository cụ thể cho từng thực thể nên kế thừa từ interface này để có được các
 * phương thức thao tác dữ liệu chuẩn một cách tự động.
 *
 * <p><b>Ví dụ về cách định nghĩa một repository cụ thể:</b>
 *
 * <pre>
 * &#64;Repository
 * public interface UserRepository extends VMCRepository&lt;User, Long&gt; {
 * Optional&lt;User&gt; findByEmail(String email);
 * }
 * </pre>
 *
 * @param <T> Kiểu của thực thể (entity) mà repository này quản lý.
 * @param <ID> Kiểu của khóa chính (primary key) của thực thể.
 * @author NatswarChuan
 */
public interface VMCRepository<T extends Model, ID> {

  /**
   * Lưu một thực thể.
   *
   * <p>Phương thức này có thể được sử dụng để tạo mới (nếu khóa chính là null) hoặc cập nhật một
   * thực thể đã tồn tại.
   *
   * @param entity Thực thể cần lưu.
   * @param <S> Kiểu của thực thể, phải là một lớp con của T.
   * @return Thực thể đã được lưu (có thể chứa các giá trị được cập nhật từ DB, ví dụ: ID tự tăng).
   */
  <S extends T> S save(S entity);

  /**
   * Lưu một thực thể với các tùy chọn lưu tùy chỉnh.
   *
   * @param entity Thực thể cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu, ví dụ như cascade.
   * @param <S> Kiểu của thực thể.
   * @return Thực thể đã được lưu.
   * @see SaveOptions
   */
  <S extends T> S save(S entity, SaveOptions options);

  /**
   * Lưu một tập hợp các thực thể.
   *
   * @param entities Một {@link Iterable} chứa các thực thể cần lưu.
   * @param <S> Kiểu của các thực thể.
   * @return Một danh sách các thực thể đã được lưu.
   */
  <S extends T> List<S> saveAll(Iterable<S> entities);

  /**
   * Lưu một thực thể từ một đối tượng DTO (Data Transfer Object).
   *
   * <p>DTO sẽ được chuyển đổi thành thực thể trước khi lưu.
   *
   * @param dto DTO chứa dữ liệu cần lưu.
   * @param <D> Kiểu của DTO.
   * @return DTO đã được cập nhật với dữ liệu từ thực thể đã lưu.
   */
  <D extends BaseDto<T, ?>> D saveDto(D dto);

  /**
   * Lưu một tập hợp các thực thể từ một {@link Iterable} các DTO.
   *
   * @param dtos Một {@code Iterable} các DTO cần lưu.
   * @param <D> Kiểu của DTO.
   * @return Một danh sách các DTO đã được cập nhật.
   */
  <D extends BaseDto<T, ?>> List<D> saveAllDtos(Iterable<D> dtos);

  /**
   * Tìm và trả về một thực thể bằng ID của nó.
   *
   * @param id ID của thực thể cần tìm. Phải không được null.
   * @return Một {@link Optional} chứa thực thể nếu tìm thấy, ngược lại trả về {@code
   *     Optional.empty()}.
   */
  Optional<T> findById(ID id);

  /**
   * Trả về tất cả các instance của thực thể.
   *
   * @return Một danh sách chứa tất cả các thực thể.
   */
  List<T> findAll();

  /**
   * Trả về một "trang" (page) các thực thể theo thông tin phân trang.
   *
   * @param page Số trang hiện tại (bắt đầu từ 1).
   * @param perPage Số lượng thực thể trên mỗi trang.
   * @return Một đối tượng {@link Paginator} chứa dữ liệu của trang và thông tin phân trang.
   */
  Paginator<T> findAll(int page, int perPage);

  /**
   * Tìm một thực thể bằng ID và chuyển đổi nó thành một DTO.
   *
   * @param id ID của thực thể.
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return Một {@link Optional} chứa DTO nếu thực thể được tìm thấy, ngược lại là rỗng.
   */
  <D extends BaseDto<T, D>> Optional<D> findByIdGetDto(ID id, Class<D> dtoClass);

  /**
   * Lấy tất cả các thực thể và chuyển đổi chúng thành một danh sách các DTO.
   *
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return Một danh sách các DTO.
   */
  <D extends BaseDto<T, D>> List<D> findAllGetDtos(Class<D> dtoClass);

  /**
   * Lấy một "trang" các thực thể, chuyển đổi chúng thành DTO và trả về dưới dạng Paginator.
   *
   * @param page Số trang hiện tại.
   * @param perPage Số lượng mục trên mỗi trang.
   * @param dtoClass Lớp của DTO đích.
   * @param <D> Kiểu của DTO.
   * @return Một đối tượng {@link Paginator} chứa dữ liệu DTO và thông tin phân trang.
   */
  <D extends BaseDto<T, D>> Paginator<D> findAllGetDtos(int page, int perPage, Class<D> dtoClass);

  /**
   * Xóa một thực thể đã cho.
   *
   * @param entity Thực thể cần xóa. Phải không được null.
   */
  void delete(T entity);

  /**
   * Xóa một thực thể với các tùy chọn tùy chỉnh.
   *
   * @param entity Thực thể cần xóa.
   * @param options Các tùy chọn để kiểm soát hành vi xóa.
   */
  void delete(T entity, RemoveOptions options);

  /**
   * Trả về số lượng thực thể có sẵn.
   *
   * @return Tổng số thực thể.
   */
  long count();
}
