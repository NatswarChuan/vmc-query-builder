package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Chỉ định một cột để join (liên kết). Annotation này được sử dụng để xác định cột khóa ngoại trong
 * các mối quan hệ {@code VMCOneToOne} và {@code VMCManyToOne}.
 *
 * <p>Nó cũng được sử dụng trong annotation {@code VMCJoinTable} để định nghĩa các cột trong bảng
 * liên kết.
 *
 * <p><b>Ví dụ (trong quan hệ ManyToOne):</b>
 *
 * <pre>
 * &#64;VMCManyToOne
 * &#64;VMCJoinColumn(name = "user_id", nullable = false)
 * private User user;
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCJoinColumn {
  /**
   * (Bắt buộc) Tên của cột khóa ngoại trong cơ sở dữ liệu.
   *
   * @return Tên cột khóa ngoại.
   */
  String name();

  /**
   * (Tùy chọn) Xác định xem cột khóa ngoại có cho phép giá trị NULL hay không.
   *
   * @return {@code true} nếu cột cho phép NULL, ngược lại là {@code false}. Mặc định là {@code
   *     true}.
   */
  boolean nullable() default true;
}
