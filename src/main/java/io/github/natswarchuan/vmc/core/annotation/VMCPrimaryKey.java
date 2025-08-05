package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định trường (field) làm khóa chính của một thực thể.
 *
 * <p>Mỗi thực thể phải có đúng một trường được đánh dấu là khóa chính.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCPrimaryKey(name = "user_id")
 * private Long id;
 * </pre>
 * 
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCPrimaryKey {
  /**
   * (Tùy chọn) Tên của cột khóa chính trong cơ sở dữ liệu.
   *
   * @return Tên cột khóa chính. Mặc định là "id".
   */
  String name() default "id";
}
