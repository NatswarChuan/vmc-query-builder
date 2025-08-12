package io.github.natswarchuan.vmc.core.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Lớp tự động cấu hình cho VMC Framework. Kích hoạt việc đăng ký VMC Repositories và quét các thành
 * phần cốt lõi.
 */
@Configuration
@Import(VMCRepositoryRegistrar.class)
@ComponentScan("io.github.natswarchuan.vmc.core")
@MapperScan("io.github.natswarchuan.vmc.core.persistence.mapper")
public class VMCAutoConfiguration {}
