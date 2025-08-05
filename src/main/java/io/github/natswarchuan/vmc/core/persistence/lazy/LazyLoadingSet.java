package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Một triển khai của {@link Set} hỗ trợ tải lười (lazy loading).
 *
 * <p>Lớp này kế thừa từ {@link AbstractLazyLoadingCollection} và ủy quyền các thao tác của Set đến
 * một {@link HashSet} bên trong, vốn chỉ được khởi tạo khi có một phương thức được gọi lần đầu
 * tiên.
 *
 * @param <E> Kiểu của các phần tử trong set.
 * @author NatswarChuan
 */
public class LazyLoadingSet<E> extends AbstractLazyLoadingCollection<E> implements Set<E> {
  public LazyLoadingSet(LazyLoader<E> loader) {
    super(loader);
  }

  @Override
  protected Collection<E> createEmptyCollection() {
    return new HashSet<>();
  }
}
