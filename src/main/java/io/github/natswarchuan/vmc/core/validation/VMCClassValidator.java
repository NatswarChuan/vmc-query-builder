package io.github.natswarchuan.vmc.core.validation;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCClassValidation;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCFieldCondition;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCJoin;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Lớp triển khai logic cho annotation validation {@link VMCClassValidation}.
 *
 * <p>Lớp này thực hiện việc kiểm tra validation ở cấp độ lớp bằng cách xây dựng và thực thi một câu
 * truy vấn động. Câu truy vấn được định hình dựa trên các điều kiện và mệnh đề join được định nghĩa
 * trong annotation {@code VMCClassValidation}. Điều này cho phép thực hiện các quy tắc validation
 * phức tạp, liên quan đến nhiều trường và nhiều bảng khác nhau, với khả năng lấy dữ liệu từ cả
 * request body, path variables, và request parameters.
 *
 * @author NatswarChuan
 * @see VMCClassValidation
 * @see VMCFieldCondition
 */
@Component
public class VMCClassValidator implements ConstraintValidator<VMCClassValidation, Object> {

  /**
   * Lưu trữ instance của annotation constraint để truy cập các thuộc tính của nó trong quá trình
   * validation.
   */
  private VMCClassValidation constraint;

  /**
   * Tự động inject {@code HttpServletRequest} hiện tại để có thể truy cập vào các path variable và
   * request parameter.
   */
  @Autowired private HttpServletRequest request;

  /**
   * Khởi tạo trình xác thực (validator) với các thông tin từ annotation.
   *
   * <p>Phương thức này được framework validation gọi một lần duy nhất trước khi sử dụng validator,
   * nhằm cung cấp cho nó các metadata từ annotation.
   *
   * @param constraintAnnotation instance của annotation {@code VMCClassValidation} được áp dụng
   *     trên lớp DTO.
   */
  @Override
  public void initialize(VMCClassValidation constraintAnnotation) {
    this.constraint = constraintAnnotation;
  }

  /**
   * Thực hiện logic validation chính.
   *
   * <p>Phương thức này xây dựng một {@link VMCQueryBuilder} dựa trên các thuộc tính của annotation.
   * Nó sẽ tuần tự:
   *
   * <ol>
   *   <li>Thêm các mệnh đề JOIN.
   *   <li>Lấy giá trị cho từng điều kiện từ nguồn tương ứng (BODY, PATH, PARAM).
   *   <li>Thêm các điều kiện vào mệnh đề WHERE.
   *   <li>Thực thi truy vấn để kiểm tra sự tồn tại của bản ghi.
   *   <li>Trả về kết quả validation dựa trên thuộc tính {@code mustNotExist}.
   * </ol>
   *
   * @param value đối tượng (thường là DTO) đang được validate.
   * @param context bối cảnh mà trong đó constraint được đánh giá.
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
        Object conditionValue = getConditionValue(condition, value);

        if (conditionValue == null) {

          continue;
        }

        String columnName =
            StringUtils.hasText(condition.column()) ? condition.column() : condition.name();

        String qualifiedColumnName = condition.alias() + "." + columnName;

        builder.where(qualifiedColumnName, condition.operator(), conditionValue);
      }

      Optional<?> result = builder.findFirst();
      boolean exists = result.isPresent();

      return !(constraint.mustNotExist() ? exists : !exists);

    } catch (Exception e) {

      return false;
    }
  }

  /**
   * Lấy giá trị cho một điều kiện dựa trên nguồn được chỉ định (BODY, PATH, PARAM).
   *
   * @param condition annotation điều kiện chứa thông tin về nguồn và tên của giá trị cần lấy.
   * @param dtoBody đối tượng DTO (request body) để lấy giá trị nếu nguồn là {@code BODY}.
   * @return giá trị được lấy từ nguồn tương ứng, hoặc {@code null} nếu không tìm thấy.
   */
  private Object getConditionValue(VMCFieldCondition condition, Object dtoBody) {
    switch (condition.source()) {
      case PATH:
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables =
            (Map<String, String>)
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return pathVariables != null ? pathVariables.get(condition.name()) : null;
      case PARAM:
        return request.getParameter(condition.name());
      case BODY:
      default:
        try {
          Field field = findField(dtoBody.getClass(), condition.name());
          field.setAccessible(true);
          return field.get(dtoBody);
        } catch (Exception e) {
          throw new VMCException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Không thể truy cập trường '" + condition.name() + "' trong DTO.",
              e);
        }
    }
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó bằng reflection.
   *
   * <p>Phương thức này duyệt ngược lên cây kế thừa của lớp để tìm một trường theo tên, cho phép
   * truy cập cả các trường private được kế thừa.
   *
   * @param clazz lớp bắt đầu tìm kiếm.
   * @param fieldName tên của trường cần tìm.
   * @return đối tượng {@code Field} nếu tìm thấy.
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
