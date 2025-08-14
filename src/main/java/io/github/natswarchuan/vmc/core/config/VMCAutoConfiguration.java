package io.github.natswarchuan.vmc.core.config;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.core.repository.support.VMCRepositoryFactoryBean;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * Lớp cấu hình tự động cho VMC Framework trong môi trường Spring Boot.
 *
 * <p>Lớp này chịu trách nhiệm quét các package để tìm các interface kế thừa từ {@link
 * VMCRepository} và tự động đăng ký chúng dưới dạng các bean repository trong Spring context, tương
 * tự như cơ chế của Spring Data.
 */
@Configuration
@ComponentScan("io.github.natswarchuan.vmc.core")
@MapperScan("io.github.natswarchuan.vmc.core.persistence.mapper")
@Slf4j
public class VMCAutoConfiguration implements BeanDefinitionRegistryPostProcessor {

  @Override
  public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry)
      throws BeansException {
    Set<String> basePackages = getBasePackages(registry);

    if (basePackages.isEmpty()) {

      return;
    }

    ClassPathScanningCandidateComponentProvider scanner = createComponentScanner();

    for (String basePackage : basePackages) {
      for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
        try {
          String beanClassName = candidate.getBeanClassName();
          if (beanClassName == null) continue;

          Class<?> repositoryInterface = Class.forName(beanClassName);
          BeanDefinitionBuilder builder =
              BeanDefinitionBuilder.genericBeanDefinition(VMCRepositoryFactoryBean.class);
          builder.addConstructorArgValue(repositoryInterface);
          registry.registerBeanDefinition(
              generateBeanName(repositoryInterface.getSimpleName()), builder.getBeanDefinition());
        } catch (ClassNotFoundException e) {
          log.warn("Không tìm thấy lớp repository, bỏ qua: {}", e.getMessage());
        }
      }
    }
  }

  @Override
  public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory)
      throws BeansException {}

  /**
   * Tìm và trả về các package cơ sở (base packages) để quét repository.
   *
   * <p>Phương thức này sẽ tìm lớp được đánh dấu {@code @SpringBootApplication}. Sau đó, nó ưu tiên
   * các package được định nghĩa trong {@code @EnableVMCRepositories}. Nếu không có, nó sẽ mặc định
   * sử dụng package của chính lớp ứng dụng đó.
   *
   * @param registry nơi chứa các bean definition để tìm kiếm
   * @return một tập hợp các package cơ sở để quét
   */
  private Set<String> getBasePackages(BeanDefinitionRegistry registry) {

    for (String beanName : registry.getBeanDefinitionNames()) {
      BeanDefinition definition = registry.getBeanDefinition(beanName);
      String beanClassName = definition.getBeanClassName();
      if (beanClassName != null) {
        try {
          Class<?> beanClass = Class.forName(beanClassName);
          if (beanClass.isAnnotationPresent(SpringBootApplication.class)) {

            if (beanClass.isAnnotationPresent(EnableVMCRepositories.class)) {
              EnableVMCRepositories annotation =
                  beanClass.getAnnotation(EnableVMCRepositories.class);
              if (annotation.basePackages().length > 0) {
                return Set.of(annotation.basePackages());
              }
              if (annotation.value().length > 0) {
                return Set.of(annotation.value());
              }
            }

            return Set.of(beanClass.getPackageName());
          }
        } catch (Exception e) {
          log.warn("Lỗi khi quét package, có thể không tìm thấy bean @SpringBootApplication", e);
        }
      }
    }
    return new HashSet<>();
  }

  /**
   * Tạo và cấu hình một scanner để tìm các interface repository.
   *
   * <p>Scanner này được cấu hình đặc biệt để chỉ tìm các interface độc lập (không phải nested
   * class) và kế thừa từ {@link VMCRepository}.
   *
   * @return một đối tượng scanner đã được cấu hình
   */
  private ClassPathScanningCandidateComponentProvider createComponentScanner() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface()
                && beanDefinition.getMetadata().isIndependent();
          }
        };
    scanner.addIncludeFilter(new AssignableTypeFilter(VMCRepository.class));
    return scanner;
  }

  /**
   * Tạo tên bean theo chuẩn Spring từ tên của interface.
   *
   * <p>Ví dụ: "UserRepository" sẽ trở thành "userRepository".
   *
   * @param interfaceName tên của repository interface
   * @return tên bean đã được tạo
   */
  private String generateBeanName(String interfaceName) {
    return StringUtils.uncapitalize(interfaceName);
  }
}
