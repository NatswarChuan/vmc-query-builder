package io.github.natswarchuan.vmc.core.validation;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCQueryRule;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

/**
 * Lớp triển khai logic cho annotation validation {@link VMCQueryRule}.
 *
 * <p>Lớp này thực hiện việc kiểm tra validation bằng cách xây dựng và thực thi một câu truy vấn đơn
 * giản để kiểm tra sự tồn tại hoặc duy nhất của một giá trị trong một cột cụ thể của cơ sở dữ liệu.
 *
 * @author NatswarChuan
 */
public class VMCQueryRuleValidator implements ConstraintValidator<VMCQueryRule, Object> {

  private Class<? extends Model> entityClass;
  private String fieldName;
  private boolean mustNotExist;
  private VMCSqlOperator operator;

  /**
   * Khởi tạo trình xác thực (validator) với các thông tin từ annotation.
   *
   * <p>Phương thức này được gọi bởi framework validation trước khi thực hiện kiểm tra, nhằm lấy các
   * thuộc tính đã được định nghĩa trong annotation {@code VMCQueryRule}.
   *
   * @param constraintAnnotation instance của annotation {@code VMCQueryRule} được áp dụng trên
   *     trường.
   */
  @Override
  public void initialize(VMCQueryRule constraintAnnotation) {
    this.entityClass = constraintAnnotation.entity();
    this.fieldName = constraintAnnotation.field();
    this.mustNotExist = constraintAnnotation.mustNotExist();
    this.operator = constraintAnnotation.operator();
  }

  /**
   * Thực hiện logic validation chính.
   *
   * <p>Phương thức này kiểm tra xem giá trị của trường được validate có thỏa mãn quy tắc đã định
   * nghĩa trong annotation hay không (phải tồn tại hoặc không được tồn tại trong cơ sở dữ liệu).
   *
   * @param value Giá trị của trường (field) đang được validate.
   * @param context Bối cảnh mà trong đó constraint được đánh giá.
   * @return {@code true} nếu giá trị hợp lệ, ngược lại là {@code false}.
   */
  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    try {
      Optional<? extends Model> result =
          VMCQueryBuilder.from(entityClass).where(fieldName, operator, value).findFirst();

      if (mustNotExist) {

        return !result.isPresent();
      } else {

        return result.isPresent();
      }
    } catch (Exception e) {

      return false;
    }
  }
}
