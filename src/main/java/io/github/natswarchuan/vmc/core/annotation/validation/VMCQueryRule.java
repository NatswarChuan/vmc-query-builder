package io.github.natswarchuan.vmc.core.annotation.validation;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import io.github.natswarchuan.vmc.core.validation.VMCQueryRuleValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để kiểm tra sự tồn tại hoặc duy nhất của một giá trị trong cơ sở dữ liệu.
 *
 * <p>Sử dụng với Spring Validation để áp dụng cho các trường (field) trong DTO.
 *
 * @author NatswarChuan
 */
@Constraint(validatedBy = VMCQueryRuleValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCQueryRule {

  /**
   * @return thông báo lỗi sẽ hiển thị khi validation thất bại
   */
  String message() default "Giá trị không hợp lệ.";

  /**
   * @return lớp entity để thực hiện truy vấn kiểm tra
   */
  Class<? extends Model> entity();

  /**
   * @return tên trường trong entity để so sánh với giá trị của trường được validate
   */
  String field();

  /**
   * @return toán tử so sánh SQL để sử dụng, mặc định là {@code VMCSqlOperator.EQUAL}
   */
  VMCSqlOperator operator() default VMCSqlOperator.EQUAL;

  /**
   * Xác định điều kiện validation.
   *
   * <ul>
   *   <li>{@code true}: Validation thất bại nếu query tìm thấy bản ghi (dùng để kiểm tra sự duy
   *       nhất).
   *   <li>{@code false}: Validation thất bại nếu query không tìm thấy bản ghi (dùng để kiểm tra sự
   *       tồn tại).
   * </ul>
   *
   * @return {@code true} nếu validation yêu cầu bản ghi không tồn tại, mặc định là {@code true}
   */
  boolean mustNotExist() default true;

  /**
   * @return các nhóm validation mà constraint này thuộc về
   */
  Class<?>[] groups() default {};

  /**
   * @return payload tùy chỉnh có thể được gán cho constraint này
   */
  Class<? extends Payload>[] payload() default {};
}
