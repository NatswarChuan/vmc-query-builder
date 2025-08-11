package io.github.natswarchuan.vmc.core.annotation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation container để cho phép lặp lại @VMCQueryValidation.
 *
 * @author NatswarChuan
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VMCQueryValidationList {
    VMCQueryValidation[] value();
}
