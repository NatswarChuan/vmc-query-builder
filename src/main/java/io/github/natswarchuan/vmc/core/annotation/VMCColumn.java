package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định cột được ánh xạ cho một trường (field) hoặc thuộc tính (property) của thực thể.
 *
 * <p>Nếu không có annotation {@code VMCColumn} nào được chỉ định cho một thuộc tính, framework sẽ
 * áp dụng các quy tắc ánh xạ mặc định.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCColumn(name = "user_name")
 * private String name;
 * </pre>
 * 
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCColumn {
  /**
   * (Bắt buộc) Tên của cột trong cơ sở dữ liệu.
   *
   * @return Tên cột.
   */
  String name();
}
