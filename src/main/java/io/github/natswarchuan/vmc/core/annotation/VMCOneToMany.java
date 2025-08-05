package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Định nghĩa một mối quan hệ một-nhiều (one-to-many) giữa hai thực thể.
 *
 * <p>Trong một mối quan hệ một-nhiều, một thực thể có thể liên kết với nhiều thực thể khác.
 * Annotation này thường được đặt trên một Collection (ví dụ: List, Set).
 *
 * <p>Phía "một" (one) là phía sở hữu nghịch đảo (inverse side), và phía "nhiều" (many) là phía sở
 * hữu (owning side) chứa khóa ngoại. Do đó, annotation này yêu cầu thuộc tính {@code mappedBy}.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCOneToMany(mappedBy = "user")
 * private List&lt;Post&gt; posts;
 *
 * &#64;VMCManyToOne
 * &#64;VMCJoinColumn(name = "user_id")
 * private User user;
 * </pre>
 * 
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCOneToMany {
  /**
   * (Bắt buộc) Tên của trường (field) sở hữu mối quan hệ ở phía đối diện (phía "nhiều").
   *
   * @return Tên trường ở phía sở hữu.
   */
  String mappedBy();
}
