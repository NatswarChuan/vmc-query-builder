package io.github.natswarchuan.vmc.core.annotation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import io.github.natswarchuan.vmc.core.validation.VMCQueryValidator;

/**
 * Annotation để kiểm tra sự tồn tại hoặc duy nhất của một giá trị trong cơ sở dữ liệu. Sử dụng với
 * Spring Validation.
 *
 * @author NatswarChuan
 */
@Constraint(validatedBy = VMCQueryValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCQueryRule {
  /** Thông báo lỗi sẽ hiển thị khi validation thất bại. */
  String message() default "Giá trị không hợp lệ.";

  /** Lớp Entity để thực hiện truy vấn. */
  Class<? extends Model> entity();

  /** Tên trường trong Entity để so sánh với giá trị của trường được validate. */
  String field();

  /** Phương thức để so sánh với giá trị của trường được validate. */
  VMCSqlOperator operator() default VMCSqlOperator.EQUAL;

  /**
   * Điều kiện validation: - true: Validation thất bại nếu query tìm thấy bản ghi (dùng để kiểm tra
   * duy nhất - "phải không tồn tại"). - false: Validation thất bại nếu query không tìm thấy bản ghi
   * (dùng để kiểm tra tồn tại - "phải tồn tại").
   */
  boolean mustNotExist() default true;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
