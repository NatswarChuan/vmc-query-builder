package io.github.natswarchuan.vmc.core.annotation.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

/**
 * Định nghĩa một điều kiện đơn lẻ để sử dụng trong @VMCClassValidation. Annotation này không phải
 * là một constraint độc lập.
 *
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface VMCFieldCondition {
  /**
   * (Bắt buộc) Tên của trường (field) trong đối tượng đang được validate (ví dụ: DTO). Giá trị của
   * trường này sẽ được sử dụng để so sánh.
   */
  String field();

  /**
   * (Tùy chọn) Tên của cột trong entity cơ sở dữ liệu để so sánh. Nếu không được chỉ định,
   * framework sẽ sử dụng tên giống với 'field'.
   */
  String column() default "";

  /**
   * (Tùy chọn) Bí danh (alias) của bảng mà điều kiện này áp dụng. Phải khớp với bí danh của entity
   * chính hoặc một trong các @VMCJoin. Mặc định là "root".
   */
  String alias() default "root";

  /** (Tùy chọn) Toán tử so sánh SQL để sử dụng. Mặc định là EQUAL (=). */
  VMCSqlOperator operator() default VMCSqlOperator.EQUAL;
}
