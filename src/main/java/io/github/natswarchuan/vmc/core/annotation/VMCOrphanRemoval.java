package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định rằng các thực thể liên quan không còn được tham chiếu (mồ côi) nên được xóa khỏi cơ sở
 * dữ liệu.
 *
 * <p>Annotation này thường được áp dụng cho các mối quan hệ {@code VMCOneToOne} và {@code
 * VMCOneToMany}. Khi một thực thể con bị xóa khỏi collection của thực thể cha và mối quan hệ bị
 * ngắt, thực thể con đó sẽ tự động bị xóa.
 *
 * <p><b>Lưu ý:</b> Cần sử dụng cẩn thận để tránh xóa dữ liệu không mong muốn.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCOneToMany(mappedBy = "user")
 * &#64;VMCOrphanRemoval
 * private List&lt;Post&gt; posts;
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCOrphanRemoval {}
