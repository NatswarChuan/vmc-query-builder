package io.github.natswarchuan.vmc.core.query.clause;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import lombok.Getter;

/**
 * Đại diện cho một mệnh đề JOIN trong một câu lệnh SQL.
 *
 * <p>Lớp này là một cấu trúc dữ liệu không thay đổi (immutable) chứa tất cả thông tin cần thiết để
 * xây dựng một phần của mệnh đề JOIN, bao gồm loại join, bảng tham gia, điều kiện join, và thông
 * tin về mối quan hệ (nếu có).
 *
 * @author NatswarChuan
 */
@Getter
public class JoinClause {
  /** Loại của mệnh đề JOIN (ví dụ: LEFT JOIN, INNER JOIN). */
  private final VMCSqlJoinType type;

  /** Tên của bảng sẽ được join. */
  private final String table;

  /** Bí danh (alias) cho bảng được join. */
  private final String alias;

  /** Vế đầu tiên của điều kiện ON (ví dụ: 'users.id'). */
  private final String first;

  /** Toán tử so sánh trong điều kiện ON (thường là '='). */
  private final String operator;

  /** Vế thứ hai của điều kiện ON (ví dụ: 'posts.user_id'). */
  private final String second;

  /** Tên của trường (field) quan hệ trong thực thể gốc. */
  private final String relationName;

  /** Loại của mối quan hệ (ví dụ: ONE_TO_ONE, ONE_TO_MANY). */
  private final RelationMetadata.RelationType relationType;

  /** Lớp của thực thể được liên kết. */
  private final Class<? extends Model> relatedClass;

  /**
   * Khởi tạo một đối tượng JoinClause mới.
   *
   * @param type Loại JOIN.
   * @param table Tên bảng join.
   * @param alias Bí danh cho bảng join.
   * @param first Vế đầu tiên của điều kiện.
   * @param operator Toán tử điều kiện.
   * @param second Vế thứ hai của điều kiện.
   * @param relatedClass Lớp của thực thể liên kết.
   * @param relationName Tên của trường quan hệ.
   * @param relationType Loại của mối quan hệ.
   */
  public JoinClause(
      VMCSqlJoinType type,
      String table,
      String alias,
      String first,
      String operator,
      String second,
      Class<? extends Model> relatedClass,
      String relationName,
      RelationMetadata.RelationType relationType) {
    this.type = type;
    this.table = table;
    this.alias = alias;
    this.first = first;
    this.operator = operator;
    this.second = second;
    this.relatedClass = relatedClass;
    this.relationName = relationName;
    this.relationType = relationType;
  }
}
