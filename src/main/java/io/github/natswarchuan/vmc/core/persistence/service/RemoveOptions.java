package io.github.natswarchuan.vmc.core.persistence.service;

import java.util.HashSet;
import java.util.Set;

/**
 * Cung cấp các tùy chọn để kiểm soát hành vi của các thao tác xóa (remove).
 *
 * <p>Lớp này được sử dụng để chỉ định các hành vi phức tạp khi xóa một thực thể, chẳng hạn như xóa
 * theo tầng (cascading) cho các mối quan hệ cụ thể.
 *
 * <p><b>Ví dụ sử dụng:</b>
 *
 * <pre>
 * RemoveOptions options = new RemoveOptions().with("posts").with("comments");
 * userRepository.delete(user, options);
 * </pre>
 */
public class RemoveOptions {

  private final Set<String> relationsToCascade = new HashSet<>();

  /**
   * Chỉ định một mối quan hệ cần được xóa theo tầng (cascade).
   *
   * @param relationName Tên của trường (field) đại diện cho mối quan hệ trong lớp thực thể.
   * @return Chính instance {@code RemoveOptions} này, để cho phép gọi chuỗi (method chaining).
   */
  public RemoveOptions with(String relationName) {
    this.relationsToCascade.add(relationName);
    return this;
  }

  /**
   * Lấy tập hợp các tên mối quan hệ đã được chỉ định để xóa theo tầng.
   *
   * @return Một {@link Set} chứa tên của các mối quan hệ sẽ được cascade.
   */
  public Set<String> getRelationsToCascade() {
    return relationsToCascade;
  }

  /**
   * Trả về một đối tượng RemoveOptions mặc định (không xóa theo tầng).
   *
   * @return RemoveOptions mặc định.
   */
  public static RemoveOptions defaults() {
    return new RemoveOptions();
  }
}
