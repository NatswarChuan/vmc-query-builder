package io.github.natswarchuan.vmc.core.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Một lớp tiện ích để truy cập các Spring bean một cách tĩnh (statically).
 *
 * <p>Lớp này triển khai {@link ApplicationContextAware} để nhận được instance của {@link
 * ApplicationContext} từ Spring. Sau đó, nó cung cấp một phương thức tĩnh {@code getBean} để cho
 * phép các lớp không được quản lý bởi Spring (ví dụ: các lớp được tạo bằng từ khóa {@code new}) có
 * thể lấy được các bean từ Spring container.
 *
 * <p><b>Cảnh báo:</b> Việc sử dụng lớp này nên được hạn chế vì nó tạo ra sự phụ thuộc vào Spring
 * context ở những nơi khó kiểm soát và kiểm thử (test). Hãy ưu tiên sử dụng cơ chế tiêm phụ thuộc
 * (dependency injection) của Spring bất cứ khi nào có thể.
 * 
 * @author NatswarChuan
 */
@Component
public class BeanUtil implements ApplicationContextAware {

  private static ApplicationContext context;

  /**
   * Phương thức này được Spring gọi lại để tiêm vào ApplicationContext.
   *
   * @param applicationContext ApplicationContext của Spring.
   * @throws BeansException nếu có lỗi xảy ra khi xử lý context.
   */
  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    context = applicationContext;
  }

  /**
   * Lấy một Spring bean theo lớp của nó.
   *
   * @param beanClass Lớp của bean cần lấy.
   * @param <T> Kiểu của bean.
   * @return Một instance của bean được quản lý bởi Spring.
   * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException nếu không tìm thấy
   *     bean.
   */
  public static <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }
}
