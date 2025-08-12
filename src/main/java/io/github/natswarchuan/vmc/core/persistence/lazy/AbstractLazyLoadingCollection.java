package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.Collection;
import java.util.Iterator;

/**
 * Một lớp cơ sở trừu tượng cho các collection (bộ sưu tập) hỗ trợ tải lười (lazy loading).
 *
 * <p>Lớp này quản lý trạng thái khởi tạo và ủy quyền (delegate) các lời gọi phương thức đến một
 * collection thực sự bên dưới (delegate). Collection thực sự này chỉ được tạo và điền dữ liệu khi
 * có một thao tác được thực hiện trên nó lần đầu tiên.
 *
 * @param <E> Kiểu của các phần tử trong collection.
 * @author NatswarChuan
 */
public abstract class AbstractLazyLoadingCollection<E> implements Collection<E> {
  /**
   * Bộ sưu tập nội bộ để lưu trữ dữ liệu sau khi đã được tải.
   *
   * <p>Ban đầu, giá trị của nó là {@code null} và sẽ được khởi tạo trong lần truy cập đầu tiên.
   */
  protected Collection<E> delegate;

  /** Đối tượng chịu trách nhiệm tải dữ liệu khi cần, không thể thay đổi sau khi khởi tạo. */
  private final LazyLoader<E> loader;

  /** Cờ đánh dấu trạng thái đã khởi tạo của bộ sưu tập. {@code true} nếu dữ liệu đã được tải. */
  private boolean initialized = false;

  /**
   * Khởi tạo một collection với cơ chế tải lười.
   *
   * @param loader đối tượng loader sẽ được dùng để tải dữ liệu khi có yêu cầu
   */
  public AbstractLazyLoadingCollection(LazyLoader<E> loader) {
    this.loader = loader;
  }

  /**
   * Khởi tạo collection nếu nó chưa được khởi tạo.
   *
   * <p>Phương thức này kiểm tra cờ {@code initialized}. Nếu là {@code false}, nó sẽ gọi phương thức
   * {@code load()} của {@link LazyLoader} để lấy dữ liệu và gán cho collection ủy quyền (delegate).
   */
  protected void initialize() {
    if (!initialized) {
      this.delegate = loader.load();
      if (this.delegate == null) {
        this.delegate = createEmptyCollection();
      }
      this.initialized = true;
    }
  }

  /**
   * Tạo một collection rỗng mặc định.
   *
   * <p>Các lớp con phải triển khai phương thức này để cung cấp một instance collection cụ thể (ví
   * dụ: ArrayList, HashSet) trong trường hợp {@code LazyLoader} trả về null.
   *
   * @return Một instance collection rỗng.
   */
  protected abstract Collection<E> createEmptyCollection();

  @Override
  public int size() {
    initialize();
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    initialize();
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    initialize();
    return delegate.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    initialize();
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    initialize();
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    initialize();
    return delegate.toArray(a);
  }

  @Override
  public boolean add(E e) {
    initialize();
    return delegate.add(e);
  }

  @Override
  public boolean remove(Object o) {
    initialize();
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    initialize();
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    initialize();
    return delegate.addAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    initialize();
    return delegate.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    initialize();
    return delegate.retainAll(c);
  }

  @Override
  public void clear() {
    initialize();
    delegate.clear();
  }

  @Override
  public String toString() {
    initialize();
    return delegate.toString();
  }

  @Override
  public int hashCode() {
    initialize();
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    initialize();
    return delegate.equals(obj);
  }
}
