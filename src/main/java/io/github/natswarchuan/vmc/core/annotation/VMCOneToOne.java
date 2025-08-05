package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Định nghĩa một mối quan hệ một-một (one-to-one) giữa hai thực thể.
 *
 * <p>Có hai cách để định nghĩa phía sở hữu (owning side):
 *
 * <ol>
 *   <li>Phía sở hữu chứa annotation {@link VMCJoinColumn} để chỉ định cột khóa ngoại.
 *   <li>Phía không sở hữu (non-owning side) sử dụng thuộc tính {@code mappedBy} để tham chiếu đến
 *       trường ở phía sở hữu.
 * </ol>
 *
 * <p><b>Ví dụ (Phía sở hữu có khóa ngoại):</b>
 *
 * <pre>
 * &#64;VMCOneToOne
 * &#64;VMCJoinColumn(name = "address_id")
 * private Address address;
 * </pre>
 *
 * <p><b>Ví dụ (Phía không sở hữu):</b>
 *
 * <pre>
 * &#64;VMCOneToOne(mappedBy = "address")
 * private Employee employee;
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCOneToOne {
  /**
   * (Tùy chọn) Tên của trường (field) sở hữu mối quan hệ ở phía đối diện.
   *
   * <p>Nếu được chỉ định, thực thể chứa annotation này được coi là phía không sở hữu.
   *
   * @return Tên trường ở phía sở hữu.
   */
  String mappedBy() default "";
}
