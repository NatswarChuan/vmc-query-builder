package io.github.natswarchuan.vmc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.github.natswarchuan.vmc.core.config.EnableVMCRepositories;

@SpringBootApplication
@EnableTransactionManagement
@EnableVMCRepositories(basePackages = "io.github.natswarchuan.vmc.repository")
public class QueryBuilderApplication {

  public static void main(String[] args) {
    SpringApplication.run(QueryBuilderApplication.class, args);
  }
}
