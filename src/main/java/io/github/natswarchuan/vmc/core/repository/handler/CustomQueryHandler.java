package io.github.natswarchuan.vmc.core.repository.handler;

import io.github.natswarchuan.vmc.core.annotation.VMCParam;
import io.github.natswarchuan.vmc.core.annotation.VMCQuery;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Xử lý các phương thức repository được chú thích bằng {@link VMCQuery}.
 *
 * <p>Lớp này chịu trách nhiệm phân tích câu lệnh SQL gốc và các tham số từ annotation, thực thi
 * truy vấn thông qua MyBatis, và sau đó ánh xạ kết quả trả về thành các đối tượng Entity hoặc DTO
 * theo đúng kiểu trả về của phương thức.
 *
 * @author NatswarChuan
 */
public class CustomQueryHandler {

  private GenericQueryExecutorMapper queryExecutor;

  /** Khởi tạo một instance mới của CustomQueryHandler. */
  public CustomQueryHandler() {}

  /**
   * Lấy instance của {@code GenericQueryExecutorMapper} một cách lười biếng (lazy).
   *
   * <p>Phương thức này đảm bảo rằng bean chỉ được lấy từ Spring context khi cần thiết lần đầu tiên.
   *
   * @return instance của {@code GenericQueryExecutorMapper}.
   */
  private GenericQueryExecutorMapper getQueryExecutor() {
    if (this.queryExecutor == null) {
      this.queryExecutor = BeanUtil.getBean(GenericQueryExecutorMapper.class);
    }
    return this.queryExecutor;
  }

  /**
   * Xử lý một lời gọi phương thức repository được chú thích bằng {@code @VMCQuery}.
   *
   * @param method Phương thức repository đã được gọi.
   * @param args Các đối số được truyền cho phương thức.
   * @param entityClass Lớp entity được quản lý bởi repository.
   * @return Kết quả của truy vấn, có thể là một thực thể, DTO, {@code Optional}, hoặc {@code List}
   *     của chúng.
   */
  public Object handle(Method method, Object[] args, Class<? extends Model> entityClass) {
    VMCQuery queryAnnotation = method.getAnnotation(VMCQuery.class);
    String sql = queryAnnotation.value();
    Map<String, Object> params = new HashMap<>();
    Parameter[] parameters = method.getParameters();

    for (int i = 0; i < parameters.length; i++) {
      VMCParam paramAnnotation = parameters[i].getAnnotation(VMCParam.class);
      if (paramAnnotation == null) {
        throw new VMCException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "@VMCParam annotation is required for all parameters in @VMCQuery method: "
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
