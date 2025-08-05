package io.github.natswarchuan.vmc.core.repository.support;

import java.lang.reflect.Proxy;
import org.springframework.beans.factory.FactoryBean;

/**
 * Một {@link FactoryBean} của Spring để tạo các instance proxy cho các interface repository của VMC.
 *
 * <p>Lớp này đóng vai trò là nhà máy sản xuất các đối tượng repository. Thay vì tự tạo
 * một lớp triển khai, nó tạo ra một proxy động (dynamic proxy) tại thời điểm chạy.
 * Mọi lời gọi phương thức đến repository sẽ được chặn và xử lý bởi
 * {@link VMCRepositoryProxyHandler}.
 *
 * @param <T> Kiểu của interface repository.
 * @author NatswarChuan
 */
public class VMCRepositoryFactoryBean<T> implements FactoryBean<T> {

  private final Class<T> repositoryInterface;

  /**
   * Khởi tạo một FactoryBean mới cho một interface repository cụ thể.
   *
   * @param repositoryInterface Lớp của interface repository cần tạo proxy.
   */
  public VMCRepositoryFactoryBean(Class<T> repositoryInterface) {
    this.repositoryInterface = repositoryInterface;
  }

  /**
   * Tạo và trả về đối tượng proxy của repository.
   *
   * @return Một instance proxy triển khai interface repository.
   * @throws Exception nếu có lỗi xảy ra trong quá trình tạo proxy.
   */
  @Override
  @SuppressWarnings("unchecked")
  public T getObject() throws Exception {
    return (T)
        Proxy.newProxyInstance(
            repositoryInterface.getClassLoader(),
            new Class[] {repositoryInterface},
            new VMCRepositoryProxyHandler(repositoryInterface));
  }

  /**
   * Trả về kiểu của đối tượng mà FactoryBean này tạo ra.
   *
   * @return Lớp của interface repository.
   */
  @Override
  public Class<?> getObjectType() {
    return repositoryInterface;
  }
}
