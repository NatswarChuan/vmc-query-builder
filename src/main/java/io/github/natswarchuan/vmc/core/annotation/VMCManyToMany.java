package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Định nghĩa một mối quan hệ nhiều-nhiều (many-to-many) giữa hai thực thể.
 *
 * <p>Phía sở hữu (owning side) của mối quan hệ cần phải chỉ định bảng liên kết bằng annotation
 * {@link VMCJoinTable}. Phía không sở hữu (non-owning side) phải sử dụng thuộc tính {@code
 * mappedBy} để tham chiếu đến trường sở hữu.
 *
 * <p><b>Ví dụ (Phía sở hữu):</b>
 *
 * <pre>
 * &#64;VMCManyToMany
 * &#64;VMCJoinTable(
 * name = "user_roles",
 * joinColumns = @VMCJoinColumn(name = "user_id"),
 * inverseJoinColumns = @VMCJoinColumn(name = "role_id")
 * )
 * private Set&lt;Role&gt; roles;
 * </pre>
 *
 * <p><b>Ví dụ (Phía không sở hữu):</b>
 *
 * <pre>
 * &#64;VMCManyToMany(mappedBy = "roles")
 * private Set&lt;User&gt; users;
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCManyToMany {
  /**
   * (Tùy chọn) Tên của trường (field) sở hữu mối quan hệ ở phía đối diện.
   *
   * <p>Thuộc tính này chỉ được chỉ định ở phía không sở hữu (non-owning side) của mối quan hệ.
   *
   * @return Tên trường ở phía sở hữu.
   */
  String mappedBy() default "";
}
