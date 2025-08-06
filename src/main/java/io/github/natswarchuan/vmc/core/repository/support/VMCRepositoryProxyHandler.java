package io.github.natswarchuan.vmc.core.repository.support;

import io.github.natswarchuan.vmc.core.annotation.VMCQuery;
import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.builder.Paginator;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.repository.handler.CustomQueryHandler;
import io.github.natswarchuan.vmc.core.repository.handler.DerivedQueryHandler;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
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

/**
 * Một trình xử lý lời gọi (invocation handler) trung tâm cho các proxy của VMC Repository.
 *
 * <p>Lớp này là "bộ não" đằng sau các interface repository của framework. Khi một phương thức trên
 * một interface kế thừa từ {@code VMCRepository} được gọi, phương thức {@link #invoke(Object,
 * Method, Object[])} của handler này sẽ được thực thi. Nó chịu trách nhiệm phân tích phương thức
 * được gọi và quyết định cách xử lý phù hợp, bao gồm:
 *
 * <ul>
 *   <li>Thực thi các câu lệnh SQL gốc được định nghĩa bằng {@link VMCQuery}.
 *   <li>Triển khai các phương thức CRUD tiêu chuẩn (như save, findById, findAll, delete).
 *   <li>Phân tích và thực thi các truy vấn dẫn xuất từ tên phương thức (derived queries).
 * </ul>
 *
 * <p>Nó ủy quyền các tác vụ cụ thể cho các handler chuyên biệt như {@link CustomQueryHandler} và
 * {@link DerivedQueryHandler}, và sử dụng {@link VMCPersistenceManager} cho các thao tác bền bỉ.
 *
 * @see VMCRepositoryFactoryBean
 * @see InvocationHandler
 * @author NatswarChuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class VMCRepositoryProxyHandler implements InvocationHandler {

  private final Class<? extends Model> entityClass;
  private VMCPersistenceManager persistenceManager;

  private final CustomQueryHandler customQueryHandler;
  private final DerivedQueryHandler derivedQueryHandler;

  /**
   * Biểu thức chính quy để phân tích các truy vấn dẫn xuất từ tên phương thức.
   *
   * <p>Ví dụ, một phương thức như {@code findDtoAllByNameOrderByCreatedAtDesc} sẽ được phân tích
   * thành các nhóm:
   *
   * <ul>
   *   <li>Group 1 (action): "find"
   *   <li>Group 2 (dto): "Dto"
   *   <li>Group 3 (quantifier): "All"
   *   <li>Group 4 (criteria): "Name"
   *   <li>Group 5 (orderByClause): "OrderByCreatedAtDesc"
   *   <li>Group 6 (orderBy): "CreatedAtDesc"
   * </ul>
   */
  private static final Pattern DERIVED_QUERY_PATTERN =
      Pattern.compile("^(find|get|count|delete|remove)(Dto)?(All|First)?By(.*?)(OrderBy(.+))?$");

  /**
   * Khởi tạo một handler mới cho một interface repository cụ thể.
   *
   * <p>Constructor này trích xuất kiểu thực thể (entity class) từ generic type của interface
   * repository và khởi tạo các handler con.
   *
   * @param repositoryInterface Lớp của interface repository cần tạo proxy.
   */
  public VMCRepositoryProxyHandler(Class<?> repositoryInterface) {
    this.entityClass =
        (Class<? extends Model>)
            ((ParameterizedType) repositoryInterface.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];

    this.customQueryHandler = new CustomQueryHandler();
    this.derivedQueryHandler = new DerivedQueryHandler();
  }

  /**
   * Lấy instance của {@code VMCPersistenceManager} một cách lười biếng.
   *
   * <p>Việc này đảm bảo rằng bean chỉ được lấy từ Spring context khi cần thiết lần đầu tiên, tránh
   * việc phải có Spring context ngay khi proxy được tạo.
   *
   * @return instance của {@code VMCPersistenceManager}.
   */
  private VMCPersistenceManager getPersistenceManager() {
    if (this.persistenceManager == null) {
      this.persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    }
    return this.persistenceManager;
  }

  /**
   * Chặn và xử lý tất cả các lời gọi phương thức trên interface repository.
   *
   * <p>Đây là phương thức cốt lõi của {@link InvocationHandler}. Nó quyết định chiến lược xử lý dựa
   * trên annotation và tên của phương thức được gọi theo thứ tự ưu tiên:
   *
   * <ol>
   *   <li><b>{@link VMCQuery}</b>: Nếu có, xử lý như một truy vấn SQL tùy chỉnh.
   *   <li><b>Phương thức tiêu chuẩn</b>: Nếu là một trong các phương thức CRUD cơ bản, xử lý trực
   *       tiếp.
   *   <li><b>Truy vấn dẫn xuất</b>: Nếu tên phương thức khớp với mẫu, phân tích và thực thi.
   *   <li>Nếu không khớp, ném ngoại lệ {@code VMCException}.
   * </ol>
   *
   * @param proxy Đối tượng proxy mà phương thức được gọi trên đó.
   * @param method Phương thức tương ứng với lời gọi trên interface.
   * @param args Mảng các đối số được truyền vào phương thức.
   * @return Kết quả từ việc thực thi phương thức.
   * @throws Throwable nếu có lỗi xảy ra trong quá trình xử lý.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    if (method.isAnnotationPresent(VMCQuery.class)) {
      return customQueryHandler.handle(method, args, entityClass);
    }

    if (isStandardMethod(method.getName())) {
      return handleStandardMethods(method, args);
    }

    Matcher matcher = DERIVED_QUERY_PATTERN.matcher(method.getName());
    if (matcher.matches()) {
      return derivedQueryHandler.handle(method, args, matcher, entityClass);
    }

    throw new VMCException(
        HttpStatus.NOT_IMPLEMENTED, "Method not implemented: " + method.getName());
  }

  /**
   * Kiểm tra xem một tên phương thức có phải là một trong các phương thức tiêu chuẩn của repository
   * hay không.
   *
   * @param methodName Tên của phương thức cần kiểm tra.
   * @return {@code true} nếu là phương thức tiêu chuẩn, ngược lại là {@code false}.
   */
  private boolean isStandardMethod(String methodName) {
    return methodName.equals("save")
        || methodName.equals("saveAll")
        || methodName.equals("saveDto")
        || methodName.equals("saveAllDtos")
        || methodName.equals("findById")
        || methodName.equals("findAll")
        || methodName.equals("delete")
        || methodName.equals("count")
        || methodName.equals("findByIdGetDto")
        || methodName.equals("findAllGetDtos");
  }

  /**
   * Xử lý việc thực thi các phương thức repository tiêu chuẩn.
   *
   * @param method Phương thức đã được gọi.
   * @param args Các đối số của phương thức.
   * @return Kết quả của việc thực thi.
   */
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
      Model entity = (Model) args[0];
      if (args.length == 2 && args[1] instanceof RemoveOptions) {
        getPersistenceManager().remove(entity, (RemoveOptions) args[1]);
      } else if (args.length == 1) {
        getPersistenceManager().remove(entity, RemoveOptions.defaults());
      }
      return null;
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

  /**
   * Xử lý logic cho phương thức {@code findByIdGetDto(ID id, Class<D> dtoClass)}.
   *
   * @param <T> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param <ID> Kiểu của khóa chính.
   * @param args Mảng các đối số gồm ID và lớp DTO.
   * @return Một {@link Optional} chứa DTO nếu tìm thấy.
   */
  private <T extends Model, D extends BaseDto<T, D>, ID> Optional<D> handleFindByIdGetDto(
      Object[] args) {
    ID id = (ID) args[0];
    Class<D> dtoClass = (Class<D>) args[1];
    Optional<T> entityOptional =
        VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).findById(id);
    return entityOptional.map(entity -> convertToDto(entity, dtoClass));
  }

  /**
   * Xử lý logic cho phương thức {@code findAllGetDtos(Class<D> dtoClass)}.
   *
   * @param <T> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param args Mảng đối số chứa lớp DTO.
   * @return Một danh sách các DTO.
   */
  private <T extends Model, D extends BaseDto<T, D>> List<D> handleFindAllGetDtos(Object[] args) {
    Class<D> dtoClass = (Class<D>) args[0];
    List<T> entities = VMCQueryBuilder.from(entityClass).with(getAllRelationNames()).get();
    return entities.stream()
        .map(entity -> convertToDto(entity, dtoClass))
        .collect(Collectors.toList());
  }

  /**
   * Xử lý logic cho phương thức {@code findAllGetDtos(int page, int perPage, Class<D> dtoClass)}.
   *
   * @param <T> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param args Mảng đối số chứa thông tin phân trang và lớp DTO.
   * @return Một {@link Paginator} chứa dữ liệu DTO.
   */
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

  /**
   * Chuyển đổi một thực thể sang một DTO.
   *
   * @param <T> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param entity Thực thể cần chuyển đổi.
   * @param dtoClass Lớp của DTO đích.
   * @return Một instance DTO đã được điền dữ liệu.
   * @throws VMCException nếu không thể tạo hoặc chuyển đổi DTO.
   */
  private <T extends Model, D extends BaseDto<T, D>> D convertToDto(T entity, Class<D> dtoClass) {
    try {
      return dtoClass.getDeclaredConstructor().newInstance().toDto(entity);
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to instantiate or convert DTO", e);
    }
  }

  /**
   * Tự động tạo một đối tượng {@link SaveOptions} từ một DTO.
   *
   * <p>Phương thức này duyệt qua các trường của DTO. Nếu một trường là một mối quan hệ (quan hệ
   * lồng) và không phải là null, tên của mối quan hệ đó sẽ được thêm vào {@code SaveOptions} để
   * kích hoạt lưu theo tầng (cascade save).
   *
   * @param dto Đối tượng DTO để kiểm tra.
   * @return Một đối tượng {@code SaveOptions} đã được cấu hình.
   */
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

  /**
   * Lấy tất cả tên của các mối quan hệ từ metadata của thực thể.
   *
   * @return Một mảng chuỗi chứa tên của tất cả các mối quan hệ.
   */
  private String[] getAllRelationNames() {
    EntityMetadata metadata = MetadataCache.getMetadata(entityClass);
    return metadata.getRelations().keySet().toArray(new String[0]);
  }
}
