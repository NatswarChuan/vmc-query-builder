package io.github.natswarchuan.vmc.core.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Lớp tự động cấu hình cho VMC Framework. Quét các thành phần và mapper cốt lõi của framework. */
@Configuration
@ComponentScan("io.github.natswarchuan.vmc.core")
@MapperScan("io.github.natswarchuan.vmc.core.persistence.mapper")
public class VMCAutoConfiguration {}
