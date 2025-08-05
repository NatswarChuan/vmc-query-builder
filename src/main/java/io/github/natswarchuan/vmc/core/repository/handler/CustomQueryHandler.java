package io.github.natswarchuan.vmc.core.repository.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.natswarchuan.vmc.core.annotation.VMCParam;
import io.github.natswarchuan.vmc.core.annotation.VMCQuery;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.util.BeanUtil;

/**
 * Xử lý các phương thức repository được chú thích bằng @VMCQuery. Lớp này xây dựng và thực thi các
 * câu lệnh SQL gốc được cung cấp.
 *
 * @author NatswarChuan
 */
public class CustomQueryHandler {

  private GenericQueryExecutorMapper queryExecutor;

  public CustomQueryHandler() {}

  /** Lấy bean GenericQueryExecutorMapper một cách lười biếng. */
  private GenericQueryExecutorMapper getQueryExecutor() {
    if (this.queryExecutor == null) {
      this.queryExecutor = BeanUtil.getBean(GenericQueryExecutorMapper.class);
    }
    return this.queryExecutor;
  }

  public Object handle(Method method, Object[] args, Class<? extends Model> entityClass) {
    VMCQuery queryAnnotation = method.getAnnotation(VMCQuery.class);
    String sql = queryAnnotation.value();
    Map<String, Object> params = new HashMap<>();
    Parameter[] parameters = method.getParameters();

    for (int i = 0; i < parameters.length; i++) {
      VMCParam paramAnnotation = parameters[i].getAnnotation(VMCParam.class);
      if (paramAnnotation == null) {
        throw new IllegalArgumentException(
            "The @VMCParam annotation is required for all parameters in the @VMCQuery method: "
                + method.getName());
      }
      params.put(paramAnnotation.value(), args[i]);
    }

    String myBatisSql = sql.replaceAll(":(\\w+)", "#{params.$1}");
    List<Map<String, Object>> rawResults = getQueryExecutor().execute(myBatisSql, params);

    Class<?> returnType = method.getReturnType();
    Class<?> resultClass;
    boolean isList = List.class.isAssignableFrom(returnType);
    boolean isOptional = Optional.class.isAssignableFrom(returnType);

    if (isList || isOptional) {
      resultClass =
          (Class<?>)
              ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
    } else {
      resultClass = returnType;
    }

    boolean isDto = !Model.class.isAssignableFrom(resultClass);

    VMCQueryBuilder builder = VMCQueryBuilder.from(entityClass);
    List<Model> entities =
        rawResults.stream()
            .map(row -> builder.mapRowToModel(entityClass, row, null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (isDto) {
      List<?> dtoList = builder.mapEntitiesToDtos(entities, resultClass);
      if (isList) {
        return dtoList;
      }
      return dtoList.isEmpty() ? (isOptional ? Optional.empty() : null) : dtoList.get(0);
    } else {
      if (isList) {
        return entities;
      }
      return entities.isEmpty() ? (isOptional ? Optional.empty() : null) : entities.get(0);
    }
  }
}
