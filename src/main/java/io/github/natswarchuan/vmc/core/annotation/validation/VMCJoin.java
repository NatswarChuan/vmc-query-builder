package io.github.natswarchuan.vmc.core.annotation.validation;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Định nghĩa một mệnh đề JOIN để sử dụng trong {@code @VMCClassValidation}.
 *
 * <p>Annotation này không phải là một constraint độc lập.
 *
 * @author NatswarChuan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface VMCJoin {

  /**
   * @return lớp entity của bảng cần join
   */
  Class<? extends Model> entity();

  /**
   * @return bí danh (alias) sẽ được sử dụng cho bảng này trong truy vấn
   */
  String alias();

  /**
   * @return điều kiện join "bên trái", ví dụ: "root.user_id"
   */
  String from();

  /**
   * @return điều kiện join "bên phải", ví dụ: "u.id"
   */
  String to();

  /**
   * @return loại JOIN sẽ được sử dụng, mặc định là {@code VMCSqlJoinType.LEFT_JOIN}
   */
  VMCSqlJoinType type() default VMCSqlJoinType.LEFT_JOIN;
}
