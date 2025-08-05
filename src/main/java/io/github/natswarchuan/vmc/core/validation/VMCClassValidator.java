package io.github.natswarchuan.vmc.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Optional;
import org.springframework.util.StringUtils;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCClassValidation;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCFieldCondition;
import io.github.natswarchuan.vmc.core.annotation.validation.VMCJoin;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;

/**
 * Lớp logic để xử lý annotation @VMCClassValidation.
 *
 * @author NatswarChuan
 */
public class VMCClassValidator implements ConstraintValidator<VMCClassValidation, Object> {

  private VMCClassValidation constraint;

  @Override
  public void initialize(VMCClassValidation constraintAnnotation) {
    this.constraint = constraintAnnotation;
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    try {
      VMCQueryBuilder builder = VMCQueryBuilder.from(constraint.entity(), constraint.alias());

      for (VMCJoin join : constraint.joins()) {
        builder.join(
            join.type(), join.entity().getSimpleName(), join.alias(), join.from(), "=", join.to());
      }

      for (VMCFieldCondition condition : constraint.conditions()) {
        Field field = findField(value.getClass(), condition.field());
        field.setAccessible(true);
        Object fieldValue = field.get(value);

        if (fieldValue == null) {
          continue;
        }

        String columnName =
            StringUtils.hasText(condition.column()) ? condition.column() : condition.field();

        String qualifiedColumnName = condition.alias() + "." + columnName;

        builder.where(qualifiedColumnName, condition.operator(), fieldValue);
      }

      Optional<?> result = builder.findFirst();
      boolean exists = result.isPresent();

      return !(constraint.mustNotExist() ? exists : !exists);

    } catch (Exception e) {

      return false;
    }
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }
}
