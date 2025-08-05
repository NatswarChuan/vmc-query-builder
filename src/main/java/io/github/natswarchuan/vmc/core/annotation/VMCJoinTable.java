package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Chỉ định một bảng liên kết (còn gọi là bảng trung gian) cho mối quan hệ Many-to-Many.
 *
 * <p>Annotation này thường được sử dụng ở phía sở hữu (owning side) của một mối quan hệ
 * Many-to-Many.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCManyToMany
 * &#64;VMCJoinTable(
 * name = "user_roles",
 * joinColumns = &#64;VMCJoinColumn(name = "user_id"),
 * inverseJoinColumns = &#64;VMCJoinColumn(name = "role_id")
 * )
 * private Set&lt;Role&gt; roles;
 * </pre>
 * 
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCJoinTable {
  /**
   * (Bắt buộc) Tên của bảng liên kết trong cơ sở dữ liệu.
   *
   * @return Tên bảng liên kết.
   */
  String name();

  /**
   * (Bắt buộc) Các cột khóa ngoại trong bảng liên kết tham chiếu đến thực thể sở hữu (thực thể chứa
   * annotation này).
   *
   * @return Mảng các cột join.
   */
  VMCJoinColumn[] joinColumns();

  /**
   * (Bắt buộc) Các cột khóa ngoại trong bảng liên kết tham chiếu đến thực thể không sở hữu (phía
   * đối diện của mối quan hệ).
   *
   * @return Mảng các cột join nghịch đảo.
   */
  VMCJoinColumn[] inverseJoinColumns();
}
