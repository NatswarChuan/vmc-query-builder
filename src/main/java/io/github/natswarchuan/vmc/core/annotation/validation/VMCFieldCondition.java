package io.github.natswarchuan.vmc.core.annotation.validation;

import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Định nghĩa một điều kiện đơn lẻ để sử dụng trong {@code @VMCClassValidation}.
 *
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface VMCFieldCondition {

  /** Enum để chỉ định nguồn của giá trị validation. */
  enum Source {
    /** Giá trị được lấy từ một trường trong request body (DTO). */
    BODY,
    /** Giá trị được lấy từ một path variable trên URL. */
    PATH,
    /** Giá trị được lấy từ một request parameter trên URL. */
    PARAM
  }

  /**
   * Tên của trường nguồn.
   *
   * <ul>
   *   <li>Nếu source là {@code BODY}, đây là tên trường trong DTO.
   *   <li>Nếu source là {@code PATH}, đây là tên của path variable.
   *   <li>Nếu source là {@code PARAM}, đây là tên của request parameter.
   * </ul>
   *
   * @return tên của trường nguồn
   */
  String name();

  /**
   * @return nguồn của giá trị để validation, mặc định là {@code Source.BODY}
   */
  Source source() default Source.BODY;

  /**
   * @return tên của cột trong entity cơ sở dữ liệu để so sánh; nếu không chỉ định, framework sẽ sử
   *     dụng giá trị của {@code name}
   */
  String column() default "";

  /**
   * @return bí danh (alias) của bảng mà điều kiện này áp dụng; phải khớp với bí danh của entity
   *     chính hoặc một trong các {@code @VMCJoin}, mặc định là "root"
   */
  String alias() default "root";

  /**
   * @return toán tử so sánh SQL để sử dụng, mặc định là {@code VMCSqlOperator.EQUAL}
   */
  VMCSqlOperator operator() default VMCSqlOperator.EQUAL;
}
