package io.github.natswarchuan.vmc.core.repository.support;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

import io.github.natswarchuan.vmc.core.annotation.VMCQuery;
import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.builder.Paginator;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.repository.handler.CustomQueryHandler;
import io.github.natswarchuan.vmc.core.repository.handler.DerivedQueryHandler;
import io.github.natswarchuan.vmc.core.util.BeanUtil;

/**
 * Một {@link InvocationHandler} để xử lý các lời gọi phương thức trên các interface repository của
 * VMC. Lớp này hoạt động như một bộ điều phối, ủy quyền các tác vụ cho các handler chuyên biệt.
 *
 * @author NatswarChuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class VMCRepositoryProxyHandler implements InvocationHandler {

  private final Class<? extends Model> entityClass;
  private VMCPersistenceManager persistenceManager;

  private final CustomQueryHandler customQueryHandler;
  private final DerivedQueryHandler derivedQueryHandler;

  private static final Pattern DERIVED_QUERY_PATTERN =
      Pattern.compile("^(find|get|count|delete|remove)(Dto)?(All|First)?By(.*?)(OrderBy(.+))?$");

  public VMCRepositoryProxyHandler(Class<?> repositoryInterface) {
    this.entityClass =
        (Class<? extends Model>)
            ((ParameterizedType) repositoryInterface.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];

    this.customQueryHandler = new CustomQueryHandler();
    this.derivedQueryHandler = new DerivedQueryHandler();
  }

  private VMCPersistenceManager getPersistenceManager() {
    if (this.persistenceManager == null) {
      this.persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    }
    return this.persistenceManager;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    if (method.isAnnotationPresent(VMCQuery.class)) {
      return customQueryHandler.handle(method, args, entityClass);
    }

    Object result = handleStandardMethods(method, args);
    if (result != null) {
      return result;
    }

    Matcher matcher = DERIVED_QUERY_PATTERN.matcher(method.getName());
    if (matcher.matches()) {
      return derivedQueryHandler.handle(method, args, matcher, entityClass);
    }

    throw new UnsupportedOperationException(
        "Phương thức chưa được triển khai: " + method.getName());
  }

  private Object handleStandardMethods(Method method, Object[] args) {
    String methodName = method.getName();

    if ("save".equals(methodName)) {
      SaveOptions options =
          (args.length > 1 && args[1] instanceof SaveOptions)
              ? (SaveOptions) args[1]
              : new SaveOptions();
      getPersistenceManager().save((Model) args[0], options);
      return args[0];
    }
    if ("saveAll".equals(methodName)) {
      Iterable<Model> argIterable = (Iterable<Model>) args[0];
      getPersistenceManager().saveAll(argIterable, new SaveOptions());
      List<Model> resultList = new ArrayList<>();
      argIterable.forEach(resultList::add);
      return resultList;
    }
    if ("saveDto".equals(methodName)) {
      return getPersistenceManager()
          .saveDto((BaseDto) args[0], createSaveOptionsFromDto((BaseDto) args[0]));
    }
    if ("saveAllDtos".equals(methodName)) {
      Iterable<BaseDto> dtos = (Iterable<BaseDto>) args[0];
      Iterator<BaseDto> iterator = dtos.iterator();
      if (!iterator.hasNext()) return Collections.emptyList();
      return getPersistenceManager().saveAllDtos(dtos, createSaveOptionsFromDto(iterator.next()));
    }
    if ("findById".equals(methodName)) {
      return VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).findById(args[0]);
    }
    if ("findAll".equals(methodName)) {
      VMCQueryBuilder builder = VMCQueryBuilder.from(entityClass).with(getAllRelationNames());
      return (args != null && args.length == 2)
          ? builder.paginate((int) args[0], (int) args[1])
          : builder.get();
    }
    if ("delete".equals(methodName)) {
      getPersistenceManager().remove((Model) args[0]);
      return new Object();
    }
    if ("count".equals(methodName) && (args == null || args.length == 0)) {
      return VMCQueryBuilder.from(entityClass).count();
    }
    if ("findByIdGetDto".equals(methodName)) {
      return handleFindByIdGetDto(args);
    }
    if ("findAllGetDtos".equals(methodName)) {
      if (args.length == 1) return handleFindAllGetDtos(args);
      if (args.length == 3) return handleFindAllGetDtosPaginated(args);
    }
    return null;
  }

  private <T extends Model, D extends BaseDto<T, D>, ID> Optional<D> handleFindByIdGetDto(
      Object[] args) {
    ID id = (ID) args[0];
    Class<D> dtoClass = (Class<D>) args[1];
    Optional<T> entityOptional =
        VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).findById(id);
    return entityOptional.map(entity -> convertToDto(entity, dtoClass));
  }

  private <T extends Model, D extends BaseDto<T, D>> List<D> handleFindAllGetDtos(Object[] args) {
    Class<D> dtoClass = (Class<D>) args[0];
    List<T> entities = VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).get();
    return entities.stream()
        .map(entity -> convertToDto(entity, dtoClass))
        .collect(Collectors.toList());
  }

  private <T extends Model, D extends BaseDto<T, D>> Paginator<D> handleFindAllGetDtosPaginated(
      Object[] args) {
    int page = (int) args[0];
    int perPage = (int) args[1];
    Class<D> dtoClass = (Class<D>) args[2];
    Paginator<T> entityPaginator =
        VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).paginate(page, perPage);
    List<D> dtoList =
        entityPaginator.getData().stream()
            .map(entity -> convertToDto(entity, dtoClass))
            .collect(Collectors.toList());
    return new Paginator<>(
        dtoList,
        entityPaginator.getTotal(),
        entityPaginator.getPerPage(),
        entityPaginator.getCurrentPage());
  }

  private <T extends Model, D extends BaseDto<T, D>> D convertToDto(T entity, Class<D> dtoClass) {
    try {
      return dtoClass.getDeclaredConstructor().newInstance().toDto(entity);
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to instantiate or convert DTO", e);
    }
  }

  private SaveOptions createSaveOptionsFromDto(BaseDto<?, ?> dto) {
    SaveOptions saveOptions = new SaveOptions();
    EntityMetadata metadata = MetadataCache.getMetadata(this.entityClass);
    for (String relationName : metadata.getRelations().keySet()) {
      try {
        Field dtoField = dto.getClass().getDeclaredField(relationName);
        dtoField.setAccessible(true);
        if (dtoField.get(dto) != null) {
          saveOptions.with(relationName);
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {

      }
    }
    return saveOptions;
  }

  private String[] getAllRelationNames() {
    EntityMetadata metadata = MetadataCache.getMetadata(entityClass);
    return metadata.getRelations().keySet().toArray(new String[0]);
  }
}
