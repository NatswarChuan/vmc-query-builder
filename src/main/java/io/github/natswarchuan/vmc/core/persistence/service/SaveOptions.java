package io.github.natswarchuan.vmc.core.persistence.service;

import java.util.HashSet;
import java.util.Set;

import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;

/**
 * Cung cấp các tùy chọn để kiểm soát hành vi của các thao tác lưu (save).
 *
 * <p>Lớp này được sử dụng để chỉ định các hành vi phức tạp khi lưu một thực thể, chẳng hạn như lưu
 * theo tầng (cascading) cho các mối quan hệ cụ thể. Nó cho phép người dùng quyết định một cách linh
 * hoạt những thực thể liên quan nào sẽ được tự động lưu cùng với thực thể chính.
 *
 * <p><b>Ví dụ sử dụng:</b>
 *
 * <pre>
 * User user = new User();
 * SaveOptions options = new SaveOptions().with("posts");
 * userRepository.save(user, options); 
 * </pre>
 *
 * @see VMCPersistenceManager
 * @author NatswarChuan
 */
public class SaveOptions {

  private final Set<String> relationsToCascade = new HashSet<>();

  /**
   * Chỉ định một mối quan hệ cần được lưu theo tầng (cascade).
   *
   * <p>Khi tên của một trường quan hệ được thêm vào đây, framework sẽ tự động lưu hoặc cập nhật các
   * thực thể trong mối quan hệ đó khi thực thể chính được lưu.
   *
   * @param relationName Tên của trường (field) đại diện cho mối quan hệ trong lớp thực thể.
   * @return Chính instance {@code SaveOptions} này, để cho phép gọi chuỗi (method chaining).
   */
  public SaveOptions with(String relationName) {
    this.relationsToCascade.add(relationName);
    return this;
  }

  /**
   * Lấy tập hợp các tên mối quan hệ đã được chỉ định để lưu theo tầng.
   *
   * @return Một {@link Set} chứa tên của các mối quan hệ sẽ được cascade.
   */
  public Set<String> getRelationsToCascade() {
    return relationsToCascade;
  }
}
