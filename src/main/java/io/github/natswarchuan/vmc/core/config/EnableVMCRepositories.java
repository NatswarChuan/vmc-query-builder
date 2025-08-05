package io.github.natswarchuan.vmc.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;

/**
 * Kích hoạt chức năng repository của VMC.
 *
 * <p>Annotation này sẽ được sử dụng trên một lớp cấu hình của Spring (ví dụ: một lớp được chú thích
 * bằng {@code @Configuration}). Nó sẽ kích hoạt việc quét (scanning) các package được chỉ định để
 * tìm các interface kế thừa từ {@code VMCRepository} và đăng ký chúng như các Spring bean.
 *
 * <p><b>Ví dụ sử dụng:</b>
 *
 * <pre>
 * &#64;Configuration
 * &#64;EnableVMCRepositories(basePackages = "com.example.repository")
 * public class AppConfig {
 * }
 * </pre>
 *
 * @see VMCRepositoryRegistrar
 * @see VMCRepository
 * 
 * @author NatswarChuan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(VMCRepositoryRegistrar.class)
public @interface EnableVMCRepositories {
  /**
   * (Tùy chọn) Bí danh (alias) cho {@link #basePackages}.
   *
   * <p>Cho phép chỉ định các package cơ sở cần quét một cách ngắn gọn hơn.
   *
   * @return Mảng các package cơ sở cần quét.
   */
  String[] value() default {};

  /**
   * (Tùy chọn) Các package cơ sở cần quét để tìm các interface repository.
   *
   * <p>Nếu không được chỉ định, framework sẽ mặc định quét package của lớp chứa annotation này.
   *
   * @return Mảng các package cơ sở cần quét.
   */
  String[] basePackages() default {};
}
