package io.github.natswarchuan.vmc.core.mapping;

import lombok.Builder;
import lombok.Getter;

/**
 * Lưu trữ siêu dữ liệu (metadata) cho một mối quan hệ giữa các thực thể.
 *
 * <p>Lớp này chứa tất cả thông tin cần thiết để hiểu và quản lý một mối quan hệ, chẳng hạn như loại
 * quan hệ (OneToOne, OneToMany, v.v.), thực thể đích, và thông tin về các cột join.
 *
 * @author NatswarChuan
 */
@Getter
@Builder
public class RelationMetadata {
  /** Tên của trường (field) trong lớp Java đại diện cho mối quan hệ. */
  private final String fieldName;

  /** Lớp của thực thể ở phía đối diện của mối quan hệ. */
  private final Class<?> targetEntity;

  /** Loại của mối quan hệ. */
  private final RelationType type;

  /**
   * Tên của trường ở phía đối diện sở hữu mối quan hệ. Chỉ được sử dụng ở phía không sở hữu
   * (non-owning side).
   */
  private final String mappedBy;

  /** Tên của cột khóa ngoại trong cơ sở dữ liệu. */
  private final String joinColumnName;

  /** Cho biết cột khóa ngoại có thể nhận giá trị NULL hay không. */
  private final boolean foreignKeyNullable;

  /** Cho biết có nên xóa các thực thể con "mồ côi" (không còn được tham chiếu) hay không. */
  private final boolean orphanRemoval;

  /** Siêu dữ liệu của bảng liên kết, chỉ được sử dụng cho quan hệ Many-to-Many. */
  private final JoinTableMetadata joinTable;

  /** Enum định nghĩa các loại mối quan hệ được hỗ trợ. */
  public enum RelationType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY;

    /**
     * Kiểm tra xem loại quan hệ này có phải là một collection (ToMany) hay không.
     * @return true nếu là OneToMany hoặc ManyToMany, ngược lại là false.
     */
    public boolean isCollection() {
      return this == ONE_TO_MANY || this == MANY_TO_MANY;
    }
  }

  /**
   * Kiểm tra xem mối quan hệ này có phải là phía sở hữu của một liên kết (association) hay không.
   * Cụ thể là các quan hệ ToOne (ManyToOne, OneToOne) mà có định nghĩa cột khóa ngoại (@VMCJoinColumn).
   * @return true nếu là phía sở hữu của một liên kết ToOne, ngược lại là false.
   */
  public boolean isOwningSideOfAssociation() {
    return (type == RelationType.MANY_TO_ONE || type == RelationType.ONE_TO_ONE) && joinColumnName != null;
  }

  /**
   * Kiểm tra xem mối quan hệ này có phải là một collection (ToMany) hay không.
   * @return true nếu là OneToMany hoặc ManyToMany, ngược lại là false.
   */
  public boolean isCollection() {
    return type == RelationType.ONE_TO_MANY || type == RelationType.MANY_TO_MANY;
  }

  /**
   * Kiểm tra xem mối quan hệ này có phải là phía nghịch đảo (inverse side) hay không,
   * được xác định bởi thuộc tính 'mappedBy'.
   * @return true nếu là phía nghịch đảo, ngược lại là false.
   */
  public boolean isInverseSide() {
    return mappedBy != null && !mappedBy.isEmpty();
  }
  
  /**
   * Kiểm tra xem đây có phải là phía sở hữu (owning side) của mối quan hệ ManyToMany hay không.
   * Phía sở hữu là phía định nghĩa @VMCJoinTable.
   * @return true nếu là phía sở hữu của quan hệ ManyToMany.
   */
  public boolean isOwningSide() {
    if (type == RelationType.MANY_TO_MANY) {
        return this.joinTable != null;
    }
    return isOwningSideOfAssociation();
  }
}
