package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.Collection;

/**
 * Một interface chức năng (functional interface) định nghĩa hợp đồng cho việc tải dữ liệu lười.
 *
 * <p>Các lớp triển khai interface này sẽ chứa logic cụ thể để truy vấn và lấy một collection các
 * thực thể từ cơ sở dữ liệu khi cần thiết.
 *
 * @param <T> Kiểu của các thực thể sẽ được tải.
 * @author NatswarChuan
 */
@FunctionalInterface
public interface LazyLoader<T> {
  /**
   * Thực hiện việc tải dữ liệu.
   *
   * @return Một collection chứa các thực thể đã được tải từ cơ sở dữ liệu.
   */
  Collection<T> load();
}
