package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định bảng cơ sở dữ liệu chính cho lớp thực thể được chú thích.
 *
 * <p>Mỗi lớp thực thể phải có annotation {@code VMCTable} để được framework nhận diện.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCTable(name = "app_users")
 * public class User extends Model {
 * }
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCTable {
  /**
   * (Bắt buộc) Tên của bảng trong cơ sở dữ liệu.
   *
   * @return Tên bảng.
   */
  String name();
}
