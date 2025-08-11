package io.github.natswarchuan.vmc.core.validation;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCQueryValidation;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Triển khai logic cho annotation {@link VMCQueryValidation}.
 *
 * <p>Class này là một Spring Component, cho phép inject các dependency như {@link
 * HttpServletRequest} và {@link GenericQueryExecutorMapper}. Nó có khả năng xử lý validation ở cả
 * cấp độ trường và lớp, bằng cách phân tích câu lệnh SQL tùy chỉnh và thu thập các tham số từ nhiều
 * nguồn khác nhau của một HTTP request.
 *
 * @author NatswarChuan
 */
@Component
public class VMCQueryValidator implements ConstraintValidator<VMCQueryValidation, Object> {

  private VMCQueryValidation constraint;

  @Autowired private HttpServletRequest request;

  @Autowired private GenericQueryExecutorMapper queryExecutor;

  /**
   * Biểu thức chính quy để phân tích các tham số đặt tên trong câu lệnh SQL.
   *
   * <p>Pattern này có thể bắt được các dạng sau:
   *
   * <ul>
   *   <li>{@code :name} (mặc định là body)
   *   <li>{@code :body.name}
   *   <li>{@code :path.name}
   *   <li>{@code :param.name}
   *   <li>{@code :this}
   * </ul>
   *
   * Group 1: prefix (body, path, param) - tùy chọn. Group 2: tên tham số.
   */
  private static final Pattern NAMED_PARAM_PATTERN =
      Pattern.compile(":(?:(body|path|param)\\.)?(\\w+)");

  @Override
  public void initialize(VMCQueryValidation constraintAnnotation) {
    this.constraint = constraintAnnotation;
  }

  /**
   * Thực hiện logic validation chính.
   *
   * @param value giá trị của trường đang được validate (đối với validation cấp trường), hoặc toàn
   *     bộ đối tượng DTO (đối với validation cấp lớp).
   * @param context context của constraint, được sử dụng để truy cập payload (toàn bộ DTO).
   * @return {@code true} nếu đối tượng hợp lệ, ngược lại là {@code false}.
   */
  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    Object rootDto =
        context
            .unwrap(HibernateConstraintValidatorContext.class)
            .getConstraintValidatorPayload(Object.class);
    if (rootDto == null) {
      rootDto = value;
    }

    String rawSql = constraint.query();
    Map<String, Object> finalQueryParams = new HashMap<>();
    StringBuilder myBatisSqlBuilder = new StringBuilder();
    int lastEnd = 0;

    Map<String, Object> bodyParams = getBodyAsMap(rootDto);
    Map<String, String> pathParams = getPathVariables();
    Map<String, String[]> requestParams = request.getParameterMap();

    Matcher matcher = NAMED_PARAM_PATTERN.matcher(rawSql);
    while (matcher.find()) {
      myBatisSqlBuilder.append(rawSql, lastEnd, matcher.start());

      String prefix = matcher.group(1);
      String paramName = matcher.group(2);

      Object paramValue = null;
      String paramKey;

      if ("path".equals(prefix)) {
        paramKey = "path_" + paramName;
        paramValue = pathParams.get(paramName);
      } else if ("param".equals(prefix)) {
        paramKey = "param_" + paramName;
        String[] values = requestParams.get(paramName);
        if (values != null && values.length > 0) {
          paramValue = values.length == 1 ? values[0] : List.of(values);
        }
      } else if ("this".equals(paramName) && prefix == null) {
        paramKey = "this";
        paramValue = value;
      } else {
        paramKey = (prefix != null ? prefix + "_" : "body_") + paramName;
        paramValue = bodyParams.get(paramName);
      }

      if (paramValue == null && !"this".equals(paramName)) {
        throw new VMCException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Validation query parameter ':"
                + matcher.group(0).substring(1)
                + "' could not be resolved.");
      }

      finalQueryParams.put(paramKey, paramValue);
      myBatisSqlBuilder.append("#{params.").append(paramKey).append("}");
      lastEnd = matcher.end();
    }
    myBatisSqlBuilder.append(rawSql.substring(lastEnd));
    String myBatisSql = myBatisSqlBuilder.toString();

    if (!myBatisSql.toLowerCase().contains("limit")) {
      myBatisSql += " LIMIT 1";
    }

    List<Map<String, Object>> result = queryExecutor.execute(myBatisSql, finalQueryParams);
    boolean exists = !result.isEmpty();

    return !(constraint.mustNotExist() ? exists : !exists);
  }

  /**
   * Trích xuất các biến đường dẫn (path variables) từ request hiện tại.
   *
   * @return một Map chứa các path variables.
   */
  private Map<String, String> getPathVariables() {
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> pathVariables =
          (Map<String, String>)
              request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
      return pathVariables != null ? pathVariables : Collections.emptyMap();
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }

  /**
   * Chuyển đổi một đối tượng DTO thành một Map các thuộc tính của nó bằng reflection.
   *
   * @param dtoBody đối tượng DTO cần chuyển đổi.
   * @return một Map chứa tên và giá trị các thuộc tính của DTO.
   */
  private Map<String, Object> getBodyAsMap(Object dtoBody) {
    Map<String, Object> bodyMap = new HashMap<>();
    try {
      for (PropertyDescriptor pd :
          Introspector.getBeanInfo(dtoBody.getClass()).getPropertyDescriptors()) {
        if (pd.getReadMethod() != null && !"class".equals(pd.getName())) {
          bodyMap.put(pd.getName(), pd.getReadMethod().invoke(dtoBody));
        }
      }
    } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to introspect DTO for validation.", e);
    }
    return bodyMap;
  }
}
