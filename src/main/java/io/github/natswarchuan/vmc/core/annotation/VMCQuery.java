package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cung cấp một câu lệnh SQL gốc (native SQL) để thực thi cho một phương thức trong repository.
 *
 * <p>Câu lệnh truy vấn sử dụng các tham số được đặt tên theo cú pháp dấu hai chấm (ví dụ: {@code
 * :name}). Mỗi tham số trong phương thức phải được liên kết với một tham số đặt tên bằng annotation
 * {@link VMCParam}.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCQuery("SELECT u.* FROM users u JOIN roles r ON u.role_id = r.id WHERE r.name = :roleName")
 * List&lt;User&gt; findByRoleName(&#64;VMCParam("roleName") String roleName);
 * </pre>
 * 
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VMCQuery {

  /**
   * (Bắt buộc) Câu lệnh SQL gốc để thực thi.
   *
   * @return Chuỗi câu lệnh SQL.
   */
  String value();
}
