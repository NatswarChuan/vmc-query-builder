package io.github.natswarchuan.vmc.core.mapping;

import java.util.Map;
import lombok.Getter;

/**
 * Lưu trữ siêu dữ liệu (metadata) đã được phân tích của một lớp thực thể (entity).
 *
 * <p>Lớp này là một đối tượng chỉ đọc (read-only) chứa tất cả thông tin cần thiết để framework
 * tương tác với bảng cơ sở dữ liệu tương ứng của thực thể, bao gồm tên bảng, ánh xạ cột, thông tin
 * khóa chính và các mối quan hệ.
 *
 * <p>Các instance của lớp này được tạo và quản lý bởi {@link MetadataCache}.
 *
 * @author NatswarChuan
 */
@Getter
public class EntityMetadata {
  /** Tên của bảng trong cơ sở dữ liệu. */
  private final String tableName;

  /** Một Map ánh xạ từ tên trường (field) trong lớp Java đến tên cột trong bảng cơ sở dữ liệu. */
  private final Map<String, String> fieldToColumnMap;

  /** Tên của trường (field) đóng vai trò là khóa chính trong lớp thực thể. */
  private final String primaryKeyFieldName;

  /** Tên của cột khóa chính trong bảng cơ sở dữ liệu. */
  private final String primaryKeyColumnName;

  /**
   * Một Map chứa siêu dữ liệu cho tất cả các mối quan hệ (ví dụ: OneToOne, ManyToMany) được định
   * nghĩa trong thực thể. Key là tên trường, và value là đối tượng {@link RelationMetadata}.
   */
  private final Map<String, RelationMetadata> relations;

  /**
   * Khởi tạo một đối tượng EntityMetadata mới.
   *
   * @param tableName Tên bảng.
   * @param fieldToColumnMap Ánh xạ từ trường sang cột.
   * @param pkFieldName Tên trường khóa chính.
   * @param pkColumnName Tên cột khóa chính.
   * @param relations Siêu dữ liệu về các mối quan hệ.
   */
  public EntityMetadata(
      String tableName,
      Map<String, String> fieldToColumnMap,
      String pkFieldName,
      String pkColumnName,
      Map<String, RelationMetadata> relations) {
    this.tableName = tableName;
    this.fieldToColumnMap = fieldToColumnMap;
    this.primaryKeyFieldName = pkFieldName;
    this.primaryKeyColumnName = pkColumnName;
    this.relations = relations;
  }

  /**
   * Lấy siêu dữ liệu cho một lớp thực thể từ cache. Đây là một phương thức factory tiện ích để thay
   * thế cho việc gọi trực tiếp MetadataCache.
   *
   * @param modelClass Lớp thực thể cần lấy siêu dữ liệu.
   * @return Đối tượng {@link EntityMetadata} tương ứng.
   */
  public static EntityMetadata of(Class<?> modelClass) {
    return MetadataCache.getMetadata(modelClass);
  }
}
