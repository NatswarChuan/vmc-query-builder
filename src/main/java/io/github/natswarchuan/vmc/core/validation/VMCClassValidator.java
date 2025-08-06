package io.github.natswarchuan.vmc.core.validation;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCClassValidation;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCFieldCondition;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCJoin;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Lớp triển khai logic cho annotation validation {@link VMCClassValidation}.
 *
 * <p>Lớp này thực hiện việc kiểm tra validation ở cấp độ lớp bằng cách xây dựng và thực thi một câu
 * truy vấn động dựa trên các điều kiện và mệnh đề join được định nghĩa trong annotation. Nó cho
 * phép thực hiện các quy tắc validation phức tạp, liên quan đến nhiều trường và nhiều bảng.
 *
 * @author NatswarChuan
 */
public class VMCClassValidator implements ConstraintValidator<VMCClassValidation, Object> {

  private VMCClassValidation constraint;

  /**
   * Khởi tạo trình xác thực (validator) với các thông tin từ annotation.
   *
   * <p>Phương thức này được gọi bởi framework validation trước khi thực hiện kiểm tra.
   *
   * @param constraintAnnotation instance của annotation {@code VMCClassValidation} được áp dụng
   *     trên lớp.
   */
  @Override
  public void initialize(VMCClassValidation constraintAnnotation) {
    this.constraint = constraintAnnotation;
  }

  /**
   * Thực hiện logic validation chính.
   *
   * <p>Phương thức này xây dựng một {@link VMCQueryBuilder} dựa trên các thuộc tính của annotation
   * {@code VMCClassValidation}. Nó lấy giá trị từ các trường của đối tượng đang được validate, áp
   * dụng các điều kiện và join, sau đó thực thi truy vấn để kiểm tra sự tồn tại (hoặc không tồn
   * tại) của bản ghi trong cơ sở dữ liệu.
   *
   * @param value Đối tượng (thường là DTO) đang được validate.
   * @param context Bối cảnh mà trong đó constraint được đánh giá.
   * @return {@code true} nếu đối tượng hợp lệ, ngược lại là {@code false}.
   */
  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    try {
      VMCQueryBuilder builder = VMCQueryBuilder.from(constraint.entity(), constraint.alias());

      for (VMCJoin join : constraint.joins()) {
        builder.join(
            join.type(), join.entity().getSimpleName(), join.alias(), join.from(), "=", join.to());
      }

      for (VMCFieldCondition condition : constraint.conditions()) {
        Field field = findField(value.getClass(), condition.field());
        field.setAccessible(true);
        Object fieldValue = field.get(value);

        if (fieldValue == null) {
          continue;
        }

        String columnName =
            StringUtils.hasText(condition.column()) ? condition.column() : condition.field();

        String qualifiedColumnName = condition.alias() + "." + columnName;

        builder.where(qualifiedColumnName, condition.operator(), fieldValue);
      }

      Optional<?> result = builder.findFirst();
      boolean exists = result.isPresent();

      return !(constraint.mustNotExist() ? exists : !exists);

    } catch (Exception e) {

      return false;
    }
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó.
   *
   * @param clazz Lớp bắt đầu tìm kiếm.
   * @param fieldName Tên của trường cần tìm.
   * @return Đối tượng {@code Field} nếu tìm thấy.
   * @throws VMCException nếu không tìm thấy trường trong toàn bộ hệ thống phân cấp lớp.
   */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new VMCException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }
}
