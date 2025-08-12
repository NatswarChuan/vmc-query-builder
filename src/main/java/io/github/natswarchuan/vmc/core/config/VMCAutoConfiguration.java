package io.github.natswarchuan.vmc.core.config;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.core.repository.support.VMCRepositoryFactoryBean;
import java.util.HashSet;
import java.util.Set;
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

@Configuration
@ComponentScan("io.github.natswarchuan.vmc.core")
@MapperScan("io.github.natswarchuan.vmc.core.persistence.mapper")
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

        }
      }
    }
  }

  @Override
  public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory)
      throws BeansException {}

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

        }
      }
    }
    return new HashSet<>();
  }

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

  private String generateBeanName(String interfaceName) {
    return StringUtils.uncapitalize(interfaceName);
  }
}
