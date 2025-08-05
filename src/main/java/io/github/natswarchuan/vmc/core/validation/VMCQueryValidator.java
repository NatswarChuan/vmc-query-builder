package io.github.natswarchuan.vmc.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

import io.github.natswarchuan.vmc.core.annotation.validation.VMCQueryRule;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

/**
 * Lớp logic để xử lý annotation @VMCQueryRule.
 *
 * @author NatswarChuan
 */
public class VMCQueryValidator implements ConstraintValidator<VMCQueryRule, Object> {

  private Class<? extends Model> entityClass;
  private String fieldName;
  private boolean mustNotExist;
  private VMCSqlOperator operator;


  @Override
  public void initialize(VMCQueryRule constraintAnnotation) {
    this.entityClass = constraintAnnotation.entity();
    this.fieldName = constraintAnnotation.field();
    this.mustNotExist = constraintAnnotation.mustNotExist();
    this.operator = constraintAnnotation.operator();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    try {
      Optional<? extends Model> result =
          VMCQueryBuilder.from(entityClass)
              .where(fieldName, operator, value)
              .findFirst();

      if (mustNotExist) {
        return !result.isPresent();
      } else {
        return result.isPresent();
      }
    } catch (Exception e) {
      return false;
    }
  }
}
