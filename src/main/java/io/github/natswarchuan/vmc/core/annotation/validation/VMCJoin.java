package io.github.natswarchuan.vmc.core.annotation.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;

/**
 * Định nghĩa một mệnh đề JOIN để sử dụng trong @VMCClassValidation. Annotation này không phải là
 * một constraint độc lập.
 *
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface VMCJoin {
  /** (Bắt buộc) Lớp Entity của bảng cần join. */
  Class<? extends Model> entity();

  /** (Bắt buộc) Bí danh (alias) sẽ được sử dụng cho bảng này trong truy vấn. */
  String alias();

  /**
   * (Bắt buộc) Điều kiện join "bên trái". Ví dụ: "root.user_id", trong đó 'root' là bí danh của
   * entity chính.
   */
  String from();

  /**
   * (Bắt buộc) Điều kiện join "bên phải". Ví dụ: "u.id", trong đó 'u' là bí danh được định nghĩa
   * trong thuộc tính 'alias'.
   */
  String to();

  /** (Tùy chọn) Loại JOIN. Mặc định là LEFT_JOIN. */
  VMCSqlJoinType type() default VMCSqlJoinType.LEFT_JOIN;
}
