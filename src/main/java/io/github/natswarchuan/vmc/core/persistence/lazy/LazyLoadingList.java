package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Một triển khai của {@link List} hỗ trợ tải lười (lazy loading).
 *
 * <p>Lớp này kế thừa từ {@link AbstractLazyLoadingCollection} và ủy quyền các thao tác của List đến
 * một {@link ArrayList} bên trong, vốn chỉ được khởi tạo khi có một phương thức được gọi lần đầu
 * tiên.
 *
 * @param <E> Kiểu của các phần tử trong list.
 * @author NatswarChuan
 */
public class LazyLoadingList<E> extends AbstractLazyLoadingCollection<E> implements List<E> {
  /**
   * Khởi tạo một danh sách mới hỗ trợ cơ chế tải lười (lazy loading).
   *
   * @param loader đối tượng sẽ được sử dụng để tải dữ liệu khi cần
   */
  public LazyLoadingList(LazyLoader<E> loader) {
    super(loader);
  }

  @Override
  protected Collection<E> createEmptyCollection() {
    return new ArrayList<>();
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    initialize();
    return ((List<E>) delegate).addAll(index, c);
  }

  @Override
  public E get(int index) {
    initialize();
    return ((List<E>) delegate).get(index);
  }

  @Override
  public E set(int index, E element) {
    initialize();
    return ((List<E>) delegate).set(index, element);
  }

  @Override
  public void add(int index, E element) {
    initialize();
    ((List<E>) delegate).add(index, element);
  }

  @Override
  public E remove(int index) {
    initialize();
    return ((List<E>) delegate).remove(index);
  }

  @Override
  public int indexOf(Object o) {
    initialize();
    return ((List<E>) delegate).indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    initialize();
    return ((List<E>) delegate).lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator() {
    initialize();
    return ((List<E>) delegate).listIterator();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    initialize();
    return ((List<E>) delegate).listIterator(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    initialize();
    return ((List<E>) delegate).subList(fromIndex, toIndex);
  }
}
