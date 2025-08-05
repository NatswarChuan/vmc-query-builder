package io.github.natswarchuan.vmc.core.annotation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.validation.VMCClassValidator;

/**
 * Một annotation validation ở cấp độ class để thực hiện các truy vấn phức tạp, kiểm tra sự tồn tại
 * hoặc duy nhất của một bản ghi dựa trên nhiều trường từ nhiều bảng.
 *
 * <p><b>Ví dụ: Kiểm tra xem username đã tồn tại trong bảng 'users' VÀ profile có active không.</b>
 *
 * <pre>
 * &#64;VMCClassValidation(
 * entity = User.class,
 * alias = "u",
 * joins = {
 * &#64;VMCJoin(entity = UserProfile.class, alias = "p", from = "u.id", to = "p.user_id")
 * },
 * conditions = {
 * &#64;VMCFieldCondition(field = "username", alias = "u"),
 * &#64;VMCFieldCondition(field = "active", alias = "p", column = "is_active")
 * },
 * message = "Username đã tồn tại hoặc profile không active."
 * )
 * </pre>
 *
 * @author NatswarChuan
 */
@Constraint(validatedBy = VMCClassValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(VMCClassValidation.List.class)
public @interface VMCClassValidation {
  /** (Bắt buộc) Lớp Entity chính (bảng FROM) để thực hiện truy vấn kiểm tra. */
  Class<? extends Model> entity();

  /** (Tùy chọn) Bí danh cho entity chính. Mặc định là "root". */
  String alias() default "root";

  /** (Tùy chọn) Một mảng các định nghĩa JOIN để liên kết với các bảng khác. */
  VMCJoin[] joins() default {};

  /**
   * (Bắt buộc) Một mảng các điều kiện. Các điều kiện này sẽ được kết hợp với nhau bằng toán tử AND.
   */
  VMCFieldCondition[] conditions();

  /**
   * (Tùy chọn) Điều kiện validation:
   *
   * <ul>
   *   <li>{@code true}: Validation thất bại nếu query tìm thấy bản ghi (dùng để kiểm tra sự duy
   *       nhất).
   *   <li>{@code false}: Validation thất bại nếu query không tìm thấy bản ghi (dùng để kiểm tra sự
   *       tồn tại).
   * </ul>
   *
   * Mặc định là {@code true}.
   */
  boolean mustNotExist() default true;

  /** (Bắt buộc) Thông báo lỗi sẽ hiển thị khi validation thất bại. */
  String message();

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** Container cho phép lặp lại annotation @VMCClassValidation. */
  @Target({ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @interface List {
    VMCClassValidation[] value();
  }
}
