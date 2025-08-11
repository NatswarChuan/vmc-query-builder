package io.github.natswarchuan.vmc.core.annotation.validation;

import io.github.natswarchuan.vmc.core.validation.VMCQueryValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation validation hợp nhất, cho phép thực thi một câu SQL tùy chỉnh để kiểm tra các quy tắc
 * nghiệp vụ ở cả cấp độ trường (field) và lớp (class).
 *
 * <p>Annotation này cung cấp một cơ chế mạnh mẽ để xác thực dữ liệu đầu vào dựa trên trạng thái
 * hiện tại của cơ sở dữ liệu, kết hợp thông tin từ nhiều nguồn trong một HTTP request.
 *
 * <h3>Cú pháp tham số trong câu query</h3>
 *
 * <p>Câu lệnh SQL trong thuộc tính {@code query()} sử dụng cú pháp tham số đặt tên đặc biệt để tham
 * chiếu đến các giá trị từ các phần khác nhau của request:
 *
 * <ul>
 *   <li>{@code :this} &mdash; (Chỉ dùng ở cấp trường) Tham chiếu đến giá trị của chính trường đang
 *       được validate.
 *   <li>{@code :fieldName} hoặc {@code :body.fieldName} &mdash; Tham chiếu đến một trường có tên
 *       {@code fieldName} trong đối tượng DTO (request body). Nếu không có tiền tố, {@code body} sẽ
 *       được sử dụng làm mặc định.
 *   <li>{@code :path.variableName} &mdash; Tham chiếu đến một Path Variable có tên {@code
 *       variableName} từ URL.
 *   <li>{@code :param.parameterName} &mdash; Tham chiếu đến một Request Parameter có tên {@code
 *       parameterName} từ URL.
 * </ul>
 *
 * <h3>Ví dụ sử dụng</h3>
 *
 * <p><b>1. Validation cấp trường (kiểm tra email duy nhất khi cập nhật):</b>
 *
 * <pre>
 * public class UserUpdateDto {
 * &#64;VMCQueryValidation(
 * query = "SELECT id FROM users WHERE email = :this AND id != :path.userId",
 * message = "Email đã được sử dụng bởi người dùng khác."
 * )
 * private String email;
 * }
 * </pre>
 *
 * <p><b>2. Validation cấp lớp (kiểm tra quyền sở hữu):</b>
 *
 * <pre>
 * &#64;VMCQueryValidation(
 * query = "SELECT id FROM posts WHERE id = :path.postId AND author_id = :userId",
 * mustNotExist = false,
 * message = "Bạn không có quyền truy cập tài nguyên này."
 * )
 * public class PostAccessDto {
 * private String userId;
 * // ...
 * }
 * </pre>
 *
 * @author NatswarChuan
 * @see VMCQueryValidator
 */
@Constraint(validatedBy = VMCQueryValidator.class)
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(VMCQueryValidationList.class)
public @interface VMCQueryValidation {

  /**
   * (Bắt buộc) Câu lệnh SQL gốc để thực thi.
   *
   * @return chuỗi câu lệnh SQL.
   */
  String query();

  /**
   * (Tùy chọn) Xác định logic kiểm tra sự tồn tại của kết quả truy vấn.
   *
   * <ul>
   *   <li>{@code true}: Validation thất bại nếu truy vấn trả về bất kỳ bản ghi nào. Hữu ích để kiểm
   *       tra tính duy nhất (ví dụ: "email này không được tồn tại").
   *   <li>{@code false}: Validation thất bại nếu truy vấn không trả về bản ghi nào. Hữu ích để kiểm
   *       tra sự tồn tại hoặc quyền truy cập (ví dụ: "người dùng này phải tồn tại").
   * </ul>
   *
   * @return {@code true} nếu kết quả không được phép tồn tại, mặc định là {@code true}.
   */
  boolean mustNotExist() default true;

  /**
   * (Bắt buộc) Thông báo lỗi sẽ được trả về khi validation thất bại.
   *
   * @return thông báo lỗi.
   */
  String message();

  /**
   * (Tùy chọn) Chỉ định các nhóm validation.
   *
   * @return mảng các lớp nhóm.
   */
  Class<?>[] groups() default {};

  /**
   * (Tùy chọn) Chỉ định payload cho validation.
   *
   * @return mảng các lớp payload.
   */
  Class<? extends Payload>[] payload() default {};
}
