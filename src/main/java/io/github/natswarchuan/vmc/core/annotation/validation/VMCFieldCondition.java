package io.github.natswarchuan.vmc.core.annotation.validation;

import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Định nghĩa một điều kiện đơn lẻ để sử dụng trong @VMCClassValidation.
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
   * (Bắt buộc) Tên của trường nguồn.
   *
   * <ul>
   *   <li>Nếu source là {@code BODY}, đây là tên trường trong DTO.
   *   <li>Nếu source là {@code PATH}, đây là tên của path variable.
   *   <li>Nếu source là {@code PARAM}, đây là tên của request parameter.
   * </ul>
   */
  String name();

  /**
   * (Tùy chọn) Nguồn của giá trị để validation.
   *
   * @return Nguồn dữ liệu, mặc định là {@code Source.BODY}.
   */
  Source source() default Source.BODY;

  /**
   * (Tùy chọn) Tên của cột trong entity cơ sở dữ liệu để so sánh. Nếu không được chỉ định,
   * framework sẽ sử dụng tên giống với thuộc tính 'name'.
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
