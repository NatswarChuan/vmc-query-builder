package io.github.natswarchuan.vmc.core.annotation;

import java.lang.annotation.*;

/**
 * Định nghĩa một mối quan hệ nhiều-một (many-to-one) giữa hai thực thể. Đây là phía "nhiều" của mối
 * quan hệ.
 *
 * <p>Thực thể chứa annotation này sẽ lưu trữ khóa ngoại. Cột khóa ngoại được chỉ định bằng
 * annotation {@link VMCJoinColumn}.
 *
 * <p><b>Ví dụ:</b>
 *
 * <pre>
 * &#64;VMCManyToOne
 * &#64;VMCJoinColumn(name = "author_id")
 * private User author;
 * </pre>
 *
 * @author NatswarChuan
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCManyToOne {}
