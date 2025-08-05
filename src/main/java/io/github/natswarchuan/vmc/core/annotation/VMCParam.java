package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Liên kết một tham số của phương thức trong repository với một tham số được đặt tên trong câu lệnh
 * truy vấn của {@link VMCQuery}.
 *
 * <p>Tất cả các tham số của một phương thức được đánh dấu {@code @VMCQuery} đều phải có annotation
 * {@code @VMCParam}.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCQuery("SELECT * FROM users WHERE status = :status AND name LIKE :name")
 * List&lt;User&gt; findUsersByStatusAndName(
 * &#64;VMCParam("status") String status,
 * &#64;VMCParam("name") String name
 * );
 * </pre>
 * 
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface VMCParam {

  /**
   * (Bắt buộc) Tên của tham số trong câu lệnh truy vấn (không bao gồm dấu hai chấm).
   *
   * @return Tên tham số.
   */
  String value();
}
