package io.github.natswarchuan.vmc.core.config;

import java.util.Map;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.core.repository.support.VMCRepositoryFactoryBean;

/**
 * Một {@link ImportBeanDefinitionRegistrar} để đăng ký các bean định nghĩa (bean definitions) cho
 * các repository được kích hoạt bởi {@link EnableVMCRepositories}.
 *
 * <p>Lớp này chịu trách nhiệm quét classpath để tìm các interface kế thừa từ {@link VMCRepository}
 * và tạo ra các bean định nghĩa cho chúng. Mỗi bean định nghĩa sẽ sử dụng {@link
 * VMCRepositoryFactoryBean} để tạo ra một proxy tại thời điểm chạy, giúp triển khai các phương thức
 * của repository một cách tự động.
 *
 * @author NatswarChuan
 */
public class VMCRepositoryRegistrar implements ImportBeanDefinitionRegistrar {

  /**
   * Đăng ký các bean định nghĩa cho các VMC repository.
   *
   * <p>Phương thức này được Spring gọi lại khi xử lý annotation {@code @Import}. Nó sẽ đọc các
   * thuộc tính từ {@link EnableVMCRepositories}, thực hiện quét classpath, và đăng ký một {@link
   * VMCRepositoryFactoryBean} cho mỗi interface repository được tìm thấy.
   *
   * @param importingClassMetadata metadata của lớp đã chú thích {@code @EnableVMCRepositories}.
   * @param registry nơi đăng ký các bean định nghĩa mới.
   */
  @Override
  public void registerBeanDefinitions(
      @NonNull AnnotationMetadata importingClassMetadata,
      @NonNull BeanDefinitionRegistry registry) {
    Map<String, Object> annotationAttributes =
        importingClassMetadata.getAnnotationAttributes(EnableVMCRepositories.class.getName());

    if (annotationAttributes == null) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not read attribute of annotation @EnableVMCRepositories. Does annotation exist?");
    }

    String[] basePackages = (String[]) annotationAttributes.get("basePackages");
    if (basePackages == null || basePackages.length == 0) {
      basePackages = (String[]) annotationAttributes.get("value");
    }

    if (basePackages.length == 0) {
      basePackages =
          new String[] {ClassUtils.getPackageName(importingClassMetadata.getClassName())};
    }

    ClassPathScanningCandidateComponentProvider scanner = createComponentScanner();

    for (String basePackage : basePackages) {
      for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
        try {
          Class<?> repositoryInterface = Class.forName(candidate.getBeanClassName());
          BeanDefinitionBuilder builder =
              BeanDefinitionBuilder.genericBeanDefinition(VMCRepositoryFactoryBean.class);
          builder.addConstructorArgValue(repositoryInterface);
          registry.registerBeanDefinition(
              generateBeanName(repositoryInterface.getSimpleName()), builder.getBeanDefinition());
        } catch (ClassNotFoundException e) {
          throw new VMCException(
              HttpStatus.BAD_REQUEST, "Repository class not found: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Tạo một {@link ClassPathScanningCandidateComponentProvider} được cấu hình để tìm các interface
   * repository của VMC.
   *
   * @return một instance của scanner.
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
   * Tạo tên bean từ tên đơn giản của interface.
   *
   * @param interfaceName tên đơn giản của interface repository.
   * @return tên bean được chuyển thành dạng camelCase.
   */
  private String generateBeanName(String interfaceName) {
    return StringUtils.uncapitalize(interfaceName);
  }
}
