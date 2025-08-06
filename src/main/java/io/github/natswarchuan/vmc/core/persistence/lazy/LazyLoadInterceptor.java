package io.github.natswarchuan.vmc.core.persistence.lazy;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.lang.reflect.Method;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * Một bộ chặn phương thức (Method Interceptor) của CGLIB để triển khai tải lười (lazy loading) cho
 * các mối quan hệ đơn lẻ (single-object associations) như Many-to-One và One-to-One.
 *
 * <p>Khi một phương thức được gọi trên đối tượng proxy, interceptor này sẽ chặn lời gọi. Nếu đối
 * tượng thực sự (target) chưa được tải, nó sẽ thực hiện một truy vấn đến cơ sở dữ liệu để lấy đối
 * tượng đó. Các lời gọi phương thức sau đó sẽ được ủy quyền cho đối tượng thực sự đã được tải.
 *
 * @author NatswarChuan
 */
public class LazyLoadInterceptor implements MethodInterceptor {
  private final Class<? extends Model> targetClass;
  private final String queryColumn;
  private final Object queryValue;
  private final String excludedRelationName;
  private Object target = null;
  private boolean initialized = false;

  /**
   * Khởi tạo một LazyLoadInterceptor mới.
   *
   * @param targetClass Lớp của thực thể cần được tải lười.
   * @param queryColumn Tên cột trong cơ sở dữ liệu được dùng để truy vấn.
   * @param queryValue Giá trị của cột dùng để truy vấn.
   * @param excludedRelationName Tên của mối quan hệ ngược lại cần được loại trừ khỏi eager loading
   *     để tránh đệ quy.
   */
  public LazyLoadInterceptor(
      Class<? extends Model> targetClass,
      String queryColumn,
      Object queryValue,
      String excludedRelationName) {
    this.targetClass = targetClass;
    this.queryColumn = queryColumn;
    this.queryValue = queryValue;
    this.excludedRelationName = excludedRelationName;
  }

  /**
   * Chặn các lời gọi phương thức trên đối tượng proxy.
   *
   * @param obj Đối tượng proxy.
   * @param method Phương thức được gọi.
   * @param args Các tham số của phương thức.
   * @param proxy Proxy được sử dụng để gọi phương thức của lớp cha.
   * @return Kết quả của việc gọi phương thức trên đối tượng thực.
   * @throws Throwable nếu có lỗi xảy ra.
   */
  @Override
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
      throws Throwable {
    if ("finalize".equals(method.getName())) {
      return null;
    }

    if (target == null && !initialized) {
      initialized = true;
      if (queryValue != null) {
        EntityMetadata targetMetadata = MetadataCache.getMetadata(targetClass);

        // Eager load tất cả các mối quan hệ NGOẠI TRỪ mối quan hệ ngược lại
        String[] relationsToLoad =
            targetMetadata.getRelations().keySet().stream()
                .filter(name -> !name.equals(this.excludedRelationName))
                .toArray(String[]::new);

        target =
            VMCQueryBuilder.from(targetClass)
                .with(relationsToLoad)
                .where(queryColumn, VMCSqlOperator.EQUAL, queryValue)
                .getFirst();
      }
    }

    if (target == null) {
      if ("toString".equals(method.getName())) {
        return "LazyProxy<" + targetClass.getSimpleName() + ">[uninitialized]";
      }
      if ("equals".equals(method.getName()) && args != null && args.length == 1) {
        return obj == args[0];
      }
      if ("hashCode".equals(method.getName())) {
        return System.identityHashCode(obj);
      }
      return null;
    }

    return method.invoke(target, args);
  }
}
