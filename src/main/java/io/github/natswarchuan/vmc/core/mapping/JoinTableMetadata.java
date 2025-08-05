package io.github.natswarchuan.vmc.core.mapping;

import io.github.natswarchuan.vmc.core.annotation.VMCJoinTable;
import lombok.Builder;
import lombok.Getter;

/**
 * Đại diện cho siêu dữ liệu (metadata) của một bảng liên kết (join table) được sử dụng trong mối
 * quan hệ Many-to-Many.
 *
 * <p>Lớp này chứa thông tin về tên bảng liên kết và các cột khóa ngoại tham chiếu đến hai bảng
 * chính của mối quan hệ.
 *
 * @see VMCJoinTable
 * 
 * @author NatswarChuan
 */
@Getter
@Builder
public class JoinTableMetadata {
  /** Tên của bảng liên kết trong cơ sở dữ liệu. */
  private final String tableName;

  /**
   * Tên của cột trong bảng liên kết tham chiếu đến khóa chính của thực thể sở hữu (owning side).
   */
  private final String joinColumn;

  /**
   * Tên của cột trong bảng liên kết tham chiếu đến khóa chính của thực thể phía đối diện (inverse
   * side).
   */
  private final String inverseJoinColumn;
}
