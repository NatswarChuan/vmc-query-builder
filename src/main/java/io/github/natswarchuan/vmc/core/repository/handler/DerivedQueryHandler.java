package io.github.natswarchuan.vmc.core.repository.handler;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCLogicalOperator;
import io.github.natswarchuan.vmc.core.query.enums.VMCSortDirection;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý các truy vấn dẫn xuất (derived queries) bằng cách phân tích tên phương thức repository.
 *
 * <p>Lớp này chứa logic để phân tích cú pháp tên của một phương thức trong interface repository (ví
 * dụ: {@code findByNameAndStatusOrderByCreatedAtDesc}), chuyển đổi nó thành một instance {@link
 * VMCQueryBuilder} tương ứng với các mệnh đề WHERE và ORDER BY, sau đó thực thi và ánh xạ kết quả.
 *
 * @author NatswarChuan
 */
public class DerivedQueryHandler {

  /**
   * Biểu thức chính quy (regex) để nhận dạng các từ khóa toán tử trong tên phương thức. Ví dụ: And,
   * Or, Like, Between, IsNull.
   */
  private static final Pattern OPERATOR_PATTERN =
      Pattern.compile(
          "(And|Or|True|False|Like|Containing|NotLike|In|NotIn|Between|GreaterThan|LessThan|GreaterThanEqual|LessThanEqual|IsNull|IsNotNull)");

  /**
   * Xử lý một lời gọi phương thức truy vấn dẫn xuất.
   *
   * @param method Phương thức repository đã được gọi.
   * @param args Các đối số được truyền cho phương thức.
   * @param methodMatcher Một đối tượng {@link Matcher} đã khớp với tên phương thức, chứa các nhóm
   *     (group) đã được phân tích (hành động, tiêu chí, sắp xếp).
   * @param entityClass Lớp entity được quản lý bởi repository.
   * @return Kết quả của truy vấn, có thể là một thực thể, DTO, {@code Optional}, hoặc {@code List}
   *     của chúng.
   */
  public Object handle(
      Method method, Object[] args, Matcher methodMatcher, Class<? extends Model> entityClass) {
    String action = methodMatcher.group(1);
    boolean isDtoByName = methodMatcher.group(2) != null;
    String quantifier = methodMatcher.group(3);
    String criteria = methodMatcher.group(4);
    String orderBy = methodMatcher.group(6);

    VMCQueryBuilder builder =
        VMCQueryBuilder.from(entityClass).with(getAllRelationNames(entityClass));

    if (criteria != null && !criteria.isEmpty()) {
      applyCriteria(builder, criteria, args);
    }

    if (orderBy != null) {
      applyOrderBy(builder, orderBy);
    }

    if (action.startsWith("count")) {
      return builder.count();
    }

    return executeAndMapResults(builder, method, isDtoByName, quantifier);
  }

  /**
   * Áp dụng các điều kiện WHERE cho query builder dựa trên phần tiêu chí của tên phương thức.
   *
   * @param builder Instance của {@code VMCQueryBuilder}.
   * @param criteria Chuỗi chứa các điều kiện (ví dụ: "NameContainingAndStatusIn").
   * @param args Mảng các đối số của phương thức.
   */
  private void applyCriteria(VMCQueryBuilder builder, String criteria, Object[] args) {
    Matcher criteriaMatcher = OPERATOR_PATTERN.matcher(criteria);
    int lastEnd = 0;
    int argIndex = 0;
    VMCLogicalOperator conjunction = VMCLogicalOperator.AND;

    while (criteriaMatcher.find()) {
      String segment = criteria.substring(lastEnd, criteriaMatcher.start());
      String operatorKeyword = criteriaMatcher.group(1);

      if ("And".equalsIgnoreCase(operatorKeyword)) {
        argIndex += applyCondition(builder, segment, args, argIndex, conjunction);
        conjunction = VMCLogicalOperator.AND;
      } else if ("Or".equalsIgnoreCase(operatorKeyword)) {
        argIndex += applyCondition(builder, segment, args, argIndex, conjunction);
        conjunction = VMCLogicalOperator.OR;
      } else {
        argIndex += applyCondition(builder, segment + operatorKeyword, args, argIndex, conjunction);
      }
      lastEnd = criteriaMatcher.end();
    }
    String lastSegment = criteria.substring(lastEnd);
    if (!lastSegment.isEmpty()) {
      applyCondition(builder, lastSegment, args, argIndex, conjunction);
    }
  }

  /**
   * Áp dụng các quy tắc sắp xếp ORDER BY cho query builder.
   *
   * @param builder Instance của {@code VMCQueryBuilder}.
   * @param orderBy Chuỗi chứa các quy tắc sắp xếp (ví dụ: "NameAscEmailDesc").
   */
  private void applyOrderBy(VMCQueryBuilder builder, String orderBy) {
    String[] orderByParts = orderBy.split("(?=Asc|Desc)");
    for (String part : orderByParts) {
      if (part.endsWith("Desc")) {
        builder.orderBy(
            StringUtils.uncapitalize(part.substring(0, part.length() - 4)), VMCSortDirection.DESC);
      } else if (part.endsWith("Asc")) {
        builder.orderBy(
            StringUtils.uncapitalize(part.substring(0, part.length() - 3)), VMCSortDirection.ASC);
      } else {
        builder.orderBy(StringUtils.uncapitalize(part), VMCSortDirection.ASC);
      }
    }
  }

  /**
   * Thực thi truy vấn đã được xây dựng và ánh xạ kết quả theo đúng kiểu trả về của phương thức.
   *
   * @param builder Instance của {@code VMCQueryBuilder} đã được cấu hình hoàn chỉnh.
   * @param method Phương thức repository gốc.
   * @param isDtoByName Cờ cho biết có nên trả về DTO hay không (dựa trên từ khóa 'Dto' trong tên).
   * @param quantifier Từ khóa định lượng (ví dụ: "First", "All").
   * @return Kết quả cuối cùng của truy vấn.
   */
  private Object executeAndMapResults(
      VMCQueryBuilder builder, Method method, boolean isDtoByName, String quantifier) {
    Class<?> returnType = method.getReturnType();
    boolean isList = List.class.isAssignableFrom(returnType);
    boolean isOptional = Optional.class.isAssignableFrom(returnType);

    Class<?> resultClass;
    if (isList || isOptional) {
      resultClass =
          (Class<?>)
              ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
    } else {
      resultClass = returnType;
    }

    boolean isDtoReturnType = isDtoByName || !Model.class.isAssignableFrom(resultClass);

    if (isDtoReturnType) {
      if (isList || "All".equals(quantifier)) {
        return builder.getDtos(resultClass);
      }
      return builder.findDto(resultClass);
    } else {
      if (isList || "All".equals(quantifier)) {
        return builder.get();
      }
      return builder.findFirst();
    }
  }

  /**
   * Phân tích một đoạn điều kiện (ví dụ: "NameContaining") và áp dụng nó vào query builder.
   *
   * @param builder Instance của {@code VMCQueryBuilder}.
   * @param segment Đoạn chuỗi điều kiện.
   * @param args Mảng các đối số của phương thức.
   * @param argIndex Chỉ số của đối số hiện tại cần xử lý.
   * @param conjunction Toán tử logic (AND/OR) để nối với điều kiện trước đó.
   * @return Số lượng đối số đã được sử dụng bởi điều kiện này (0, 1, hoặc 2 cho Between).
   */
  private int applyCondition(
      VMCQueryBuilder builder,
      String segment,
      Object[] args,
      int argIndex,
      VMCLogicalOperator conjunction) {
    if (segment == null || segment.isEmpty()) return 0;

    String fieldName;
    VMCSqlOperator operator;
    Object value = null;
    int argsConsumed = 1;

    if (segment.endsWith("Like")) {
      fieldName = segment.substring(0, segment.length() - 4);
      operator = VMCSqlOperator.LIKE;
    } else if (segment.endsWith("Containing")) {
      fieldName = segment.substring(0, segment.length() - 10);
      operator = VMCSqlOperator.LIKE;
      value = "%" + args[argIndex] + "%";
    } else if (segment.endsWith("NotLike")) {
      fieldName = segment.substring(0, segment.length() - 7);
      operator = VMCSqlOperator.NOT_LIKE;
    } else if (segment.endsWith("In")) {
      fieldName = segment.substring(0, segment.length() - 2);
      operator = VMCSqlOperator.IN;
    } else if (segment.endsWith("NotIn")) {
      fieldName = segment.substring(0, segment.length() - 5);
      operator = VMCSqlOperator.NOT_IN;
    } else if (segment.endsWith("Between")) {
      fieldName = StringUtils.uncapitalize(segment.substring(0, segment.length() - 7));
      builder.where(fieldName, VMCSqlOperator.GREATER_THAN_OR_EQUAL, args[argIndex]);
      builder.where(fieldName, VMCSqlOperator.LESS_THAN_OR_EQUAL, args[argIndex + 1]);
      return 2;
    } else if (segment.endsWith("GreaterThanEqual")) {
      fieldName = segment.substring(0, segment.length() - 16);
      operator = VMCSqlOperator.GREATER_THAN_OR_EQUAL;
    } else if (segment.endsWith("LessThanEqual")) {
      fieldName = segment.substring(0, segment.length() - 13);
      operator = VMCSqlOperator.LESS_THAN_OR_EQUAL;
    } else if (segment.endsWith("GreaterThan")) {
      fieldName = segment.substring(0, segment.length() - 11);
      operator = VMCSqlOperator.GREATER_THAN;
    } else if (segment.endsWith("LessThan")) {
      fieldName = segment.substring(0, segment.length() - 8);
      operator = VMCSqlOperator.LESS_THAN;
    } else if (segment.endsWith("IsNull") || segment.endsWith("IsNotNull")) {
      argsConsumed = 0;
      operator = segment.endsWith("IsNull") ? VMCSqlOperator.IS_NULL : VMCSqlOperator.IS_NOT_NULL;
      fieldName = segment.substring(0, segment.length() - (operator.name().length() - 2));
    } else if (segment.endsWith("True") || segment.endsWith("False")) {
      argsConsumed = 0;
      operator = VMCSqlOperator.EQUAL;
      value = segment.endsWith("True");
      fieldName = segment.substring(0, segment.length() - (value.toString().length()));
    } else {
      fieldName = segment;
      operator = VMCSqlOperator.EQUAL;
    }

    if (argsConsumed > 0 && value == null) {
      value = args[argIndex];
    }

    String finalFieldName = StringUtils.uncapitalize(fieldName);
    if (operator == VMCSqlOperator.IN && !(value instanceof Collection)) {
      throw new IllegalArgumentException(
          "IN operator requires a Collection argument for field " + finalFieldName);
    }

    if (conjunction == VMCLogicalOperator.AND) {
      builder.where(finalFieldName, operator, value);
    } else {
      builder.orWhere(finalFieldName, operator, value);
    }
    return argsConsumed;
  }

  /**
   * Lấy tất cả tên của các mối quan hệ từ metadata của một thực thể.
   *
   * @param entityClass Lớp của thực thể.
   * @return Một mảng chuỗi chứa tên của tất cả các mối quan hệ.
   */
  private String[] getAllRelationNames(Class<? extends Model> entityClass) {
    EntityMetadata metadata = MetadataCache.getMetadata(entityClass);
    return metadata.getRelations().keySet().toArray(new String[0]);
  }

  /** Lớp tiện ích nội bộ để xử lý chuỗi. */
  private static class StringUtils {
    /**
     * Chuyển đổi ký tự đầu tiên của một chuỗi thành chữ thường.
     *
     * @param str Chuỗi cần chuyển đổi.
     * @return Chuỗi đã được chuyển đổi.
     */
    static String uncapitalize(String str) {
      if (str == null || str.isEmpty()) {
        return str;
      }
      return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
  }
}
