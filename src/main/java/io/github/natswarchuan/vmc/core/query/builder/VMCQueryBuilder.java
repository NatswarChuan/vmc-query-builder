package io.github.natswarchuan.vmc.core.query.builder;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.clause.JoinClause;
import io.github.natswarchuan.vmc.core.query.clause.OrderByClause;
import io.github.natswarchuan.vmc.core.query.clause.PreparedQuery;
import io.github.natswarchuan.vmc.core.query.clause.WhereClause;
import io.github.natswarchuan.vmc.core.query.enums.VMCLogicalOperator;
import io.github.natswarchuan.vmc.core.query.enums.VMCSortDirection;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import io.github.natswarchuan.vmc.core.query.helper.QueryResultMapper;
import io.github.natswarchuan.vmc.core.query.helper.SqlBuilder;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Cung cấp một API linh hoạt (fluent API) để xây dựng và thực thi các câu lệnh SQL động.
 *
 * <p>Đây là lớp trung tâm của cơ chế truy vấn, cho phép người dùng tạo ra các truy vấn phức tạp một
 * cách lập trình thông qua việc gọi chuỗi các phương thức (method chaining). Lớp này điều phối các
 * lớp helper như {@link SqlBuilder} để tạo chuỗi SQL và {@link QueryResultMapper} để ánh xạ kết quả
 * trả về thành các đối tượng Entity hoặc DTO.
 *
 * @author NatswarChuan
 */
@Component
@SuppressWarnings("unchecked")
public class VMCQueryBuilder {

  private static GenericQueryExecutorMapper queryExecutor;
  private static VMCPersistenceManager persistenceManager;

  @Autowired private GenericQueryExecutorMapper injectedQueryExecutor;
  @Autowired private VMCPersistenceManager injectedPersistenceManager;

  /**
   * Khởi tạo các phụ thuộc tĩnh sau khi bean được Spring khởi tạo.
   *
   * <p>Phương thức này sử dụng {@code @PostConstruct} để đảm bảo rằng các trường tĩnh {@code
   * queryExecutor} và {@code persistenceManager} được gán giá trị từ các bean được tiêm vào, cho
   * phép các phương thức tĩnh của builder có thể truy cập chúng.
   */
  @PostConstruct
  public void init() {
    VMCQueryBuilder.queryExecutor = injectedQueryExecutor;
    VMCQueryBuilder.persistenceManager = injectedPersistenceManager;
  }

  private boolean disableRecursion = false;
  private Class<? extends Model> modelClass;
  private String fromAlias;
  private final List<String> selectColumns = new ArrayList<>();
  private final List<WhereClause> whereClauses = new ArrayList<>();
  private final List<OrderByClause> orderByClauses = new ArrayList<>();
  private final List<String> groupByColumns = new ArrayList<>();
  private Integer limit;
  private Integer offset;
  private final List<JoinClause> joinClauses = new ArrayList<>();
  private final List<String> withRelations = new ArrayList<>();

  /** Khởi tạo một VMCQueryBuilder trống. */
  public VMCQueryBuilder() {}

  /**
   * Khởi tạo một VMCQueryBuilder với lớp Model và bí danh được chỉ định.
   *
   * @param modelClass Lớp thực thể gốc cho truy vấn.
   * @param alias Bí danh sẽ được sử dụng cho bảng gốc.
   */
  private VMCQueryBuilder(Class<? extends Model> modelClass, String alias) {
    this.modelClass = modelClass;
    this.fromAlias = alias;
  }

  /**
   * Vô hiệu hóa logic truy vấn đệ quy cho instance builder này.
   *
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder disableRecursion() {
    this.disableRecursion = true;
    return this;
  }

  /**
   * Phương thức factory để bắt đầu xây dựng một truy vấn mới.
   *
   * @param modelClass Lớp thực thể gốc (bảng FROM).
   * @param alias Bí danh cho bảng gốc.
   * @return Một instance mới của {@code VMCQueryBuilder}.
   */
  public static VMCQueryBuilder from(Class<? extends Model> modelClass, String alias) {
    return new VMCQueryBuilder(modelClass, alias);
  }

  /**
   * Phương thức factory để bắt đầu xây dựng một truy vấn mới với bí danh mặc định.
   *
   * <p>Bí danh mặc định sẽ là chữ cái đầu tiên của tên lớp thực thể, viết thường.
   *
   * @param modelClass Lớp thực thể gốc.
   * @return Một instance mới của {@code VMCQueryBuilder}.
   */
  public static VMCQueryBuilder from(Class<? extends Model> modelClass) {
    String alias = modelClass.getSimpleName().substring(0, 1).toLowerCase();
    return new VMCQueryBuilder(modelClass, alias);
  }

  /**
   * Chỉ định các cột cần chọn trong mệnh đề SELECT.
   *
   * @param columns Danh sách các cột cần chọn.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder select(String... columns) {
    this.selectColumns.clear();
    this.selectColumns.addAll(Arrays.asList(columns));
    return this;
  }

  /**
   * Thêm một điều kiện vào mệnh đề WHERE, được nối bằng toán tử AND.
   *
   * @param column Tên cột.
   * @param operator Toán tử so sánh.
   * @param value Giá trị để so sánh.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder where(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.AND, column, operator, value));
    return this;
  }

  /**
   * Thêm một điều kiện vào mệnh đề WHERE, được nối bằng toán tử OR.
   *
   * @param column Tên cột.
   * @param operator Toán tử so sánh.
   * @param value Giá trị để so sánh.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder orWhere(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.OR, column, operator, value));
    return this;
  }

  /**
   * Thêm một điều kiện {@code WHERE ... IN (...)} vào truy vấn.
   *
   * @param column Tên cột.
   * @param values Một collection chứa các giá trị để kiểm tra.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder whereIn(String column, Collection<?> values) {
    this.whereClauses.add(
        new WhereClause(VMCLogicalOperator.AND, column, VMCSqlOperator.IN, values));
    return this;
  }

  /**
   * Thêm một hoặc nhiều cột vào mệnh đề GROUP BY.
   *
   * @param columns Các cột để nhóm kết quả.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder groupBy(String... columns) {
    this.groupByColumns.addAll(Arrays.asList(columns));
    return this;
  }

  /**
   * Thêm một mệnh đề LIMIT để giới hạn số lượng bản ghi trả về.
   *
   * @param limit Số lượng bản ghi tối đa.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder limit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Thêm một mệnh đề OFFSET để chỉ định vị trí bắt đầu lấy bản ghi.
   *
   * @param offset Vị trí bắt đầu.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder offset(int offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Thêm một quy tắc sắp xếp vào mệnh đề ORDER BY.
   *
   * @param column Cột cần sắp xếp.
   * @param direction Hướng sắp xếp (ASC hoặc DESC).
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder orderBy(String column, VMCSortDirection direction) {
    this.orderByClauses.add(new OrderByClause(column, direction));
    return this;
  }

  /**
   * Chỉ định các mối quan hệ cần được tải ngay lập tức (eager loading).
   *
   * @param relations Tên các trường (field) đại diện cho mối quan hệ cần tải.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder with(String... relations) {
    this.withRelations.addAll(Arrays.asList(relations));
    return this;
  }

  /**
   * Thêm một mệnh đề JOIN tùy chỉnh vào truy vấn.
   *
   * @param type Loại JOIN (ví dụ: INNER, LEFT).
   * @param table Bảng cần join.
   * @param alias Bí danh cho bảng join.
   * @param first Vế đầu tiên của điều kiện ON.
   * @param operator Toán tử so sánh trong điều kiện ON.
   * @param second Vế thứ hai của điều kiện ON.
   * @return Chính instance builder này để gọi chuỗi.
   */
  public VMCQueryBuilder join(
      VMCSqlJoinType type,
      String table,
      String alias,
      String first,
      String operator,
      String second) {
    this.joinClauses.add(
        new JoinClause(type, table, alias, first, operator, second, null, null, null));
    return this;
  }

  /**
   * Thực thi truy vấn và trả về bản ghi đầu tiên.
   *
   * @param <T> Kiểu của thực thể.
   * @return Thực thể đầu tiên tìm thấy, hoặc {@code null} nếu không có kết quả.
   */
  public <T extends Model> T getFirst() {
    this.limit(1);
    List<T> results = get();
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Thực thi truy vấn và trả về bản ghi đầu tiên dưới dạng {@link Optional}.
   *
   * @param <T> Kiểu của thực thể.
   * @return Một {@code Optional} chứa thực thể đầu tiên nếu có, ngược lại là {@code
   *     Optional.empty()}.
   */
  public <T extends Model> Optional<T> findFirst() {
    return Optional.ofNullable(this.getFirst());
  }

  /**
   * Thực thi truy vấn và trả về kết quả thô dưới dạng danh sách các Map.
   *
   * @return Một {@code List<Map<String, Object>>} đại diện cho các hàng kết quả.
   */
  public List<Map<String, Object>> getRaw() {
    prepareJoinsForWith();
    SqlBuilder sqlBuilder = createSqlBuilder();
    PreparedQuery preparedQuery = sqlBuilder.build();
    return queryExecutor.execute(preparedQuery.getSql(), preparedQuery.getParams());
  }

  /**
   * Thực thi truy vấn và trả về một danh sách các thực thể.
   *
   * <p>Phương thức này có khả năng xử lý các truy vấn đệ quy nếu một mối quan hệ đệ quy được chỉ
   * định trong {@code with()}.
   *
   * @param <T> Kiểu của thực thể.
   * @return Một danh sách các thực thể.
   */
  public <T extends Model> List<T> get() {
    if (!this.disableRecursion) {
      RelationMetadata recursiveChildRel = findRecursiveChildRelation();
      if (recursiveChildRel != null) {
        return getRecursive(recursiveChildRel);
      }
    }
    return getInternal();
  }

  /**
   * Thực thi truy vấn và ánh xạ kết quả thành các thực thể (phiên bản nội bộ).
   *
   * @param <T> Kiểu của thực thể.
   * @return Một danh sách các thực thể.
   */
  private <T extends Model> List<T> getInternal() {
    List<Map<String, Object>> flatResults = getRaw();
    QueryResultMapper mapper = createResultMapper();
    return mapper.processFlatResults(flatResults, this.joinClauses);
  }

  /**
   * Thực thi truy vấn và trả về bản ghi đầu tiên được chuyển đổi thành DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param dtoClass Lớp của DTO đích.
   * @return Một instance DTO, hoặc {@code null} nếu không có kết quả.
   */
  public <D> D getDto(Class<D> dtoClass) {
    this.limit(1);
    List<D> results = getDtos(dtoClass);
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Thực thi truy vấn và trả về bản ghi đầu tiên dưới dạng {@link Optional} của DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param dtoClass Lớp của DTO đích.
   * @return Một {@code Optional} chứa DTO nếu có, ngược lại là rỗng.
   */
  public <D> Optional<D> findDto(Class<D> dtoClass) {
    return Optional.ofNullable(this.getDto(dtoClass));
  }

  /**
   * Thực thi truy vấn và trả về một danh sách các DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param dtoClass Lớp của DTO đích.
   * @return Một danh sách các DTO.
   */
  public <D> List<D> getDtos(Class<D> dtoClass) {
    List<Model> entities = get();
    QueryResultMapper mapper = createResultMapper();
    return mapper.mapEntitiesToDtos(entities, dtoClass);
  }

  /**
   * Thực thi truy vấn để đếm tổng số bản ghi thỏa mãn điều kiện.
   *
   * @return Tổng số bản ghi.
   */
  public long count() {
    prepareJoinsForWith();
    SqlBuilder sqlBuilder = createSqlBuilder();
    PreparedQuery preparedQuery = sqlBuilder.buildCountQuery();
    List<Map<String, Object>> result =
        queryExecutor.execute(preparedQuery.getSql(), preparedQuery.getParams());

    if (result == null || result.isEmpty() || result.get(0) == null || result.get(0).isEmpty()) {
      return 0L;
    }
    Object countValue = result.get(0).get("count");
    return (countValue instanceof Number) ? ((Number) countValue).longValue() : 0L;
  }

  /**
   * Thực thi truy vấn và trả về kết quả dưới dạng một đối tượng {@link Paginator}.
   *
   * @param <T> Kiểu của thực thể.
   * @param page Số trang hiện tại (bắt đầu từ 1).
   * @param perPage Số lượng mục trên mỗi trang.
   * @return Một đối tượng {@code Paginator} chứa dữ liệu và thông tin phân trang.
   */
  public <T extends Model> Paginator<T> paginate(int page, int perPage) {
    long total = this.count();
    if (total == 0) {
      return new Paginator<>(Collections.emptyList(), 0, perPage, page);
    }

    this.limit(perPage).offset((page - 1) * perPage);
    List<T> data = get();

    return new Paginator<>(data, total, perPage, page);
  }

  /**
   * Thực thi truy vấn, phân trang và chuyển đổi kết quả thành DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param page Số trang hiện tại.
   * @param perPage Số lượng mục trên mỗi trang.
   * @param dtoClass Lớp của DTO đích.
   * @return Một đối tượng {@code Paginator} chứa dữ liệu DTO.
   */
  public <D> Paginator<D> paginateDto(int page, int perPage, Class<D> dtoClass) {
    Paginator<Model> entityPaginator = paginate(page, perPage);
    QueryResultMapper mapper = createResultMapper();
    List<D> dtoList = mapper.mapEntitiesToDtos(entityPaginator.getData(), dtoClass);
    return new Paginator<>(dtoList, entityPaginator.getTotal(), perPage, page);
  }

  /**
   * Tìm một thực thể bằng khóa chính của nó.
   *
   * @param <T> Kiểu của thực thể.
   * @param id Giá trị của khóa chính.
   * @return Một {@code Optional} chứa thực thể nếu tìm thấy.
   */
  public <T extends Model> Optional<T> findById(Object id) {
    EntityMetadata metadata = MetadataCache.getMetadata(this.modelClass);
    String pkColumn = metadata.getPrimaryKeyColumnName();
    return (Optional<T>) this.where(pkColumn, VMCSqlOperator.EQUAL, id).findFirst();
  }

  /**
   * Lấy một thực thể bằng khóa chính của nó.
   *
   * @param <T> Kiểu của thực thể.
   * @param id Giá trị của khóa chính.
   * @return Thực thể nếu tìm thấy, ngược lại là {@code null}.
   */
  public <T extends Model> T getById(Object id) {
    return (T) this.findById(id).orElse(null);
  }

  /**
   * Tìm một thực thể bằng khóa chính và chuyển đổi thành DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param dtoClass Lớp của DTO đích.
   * @param id Giá trị của khóa chính.
   * @return Một {@code Optional} chứa DTO nếu tìm thấy.
   */
  public <D> Optional<D> findByIdGetDto(Class<D> dtoClass, Object id) {
    EntityMetadata metadata = MetadataCache.getMetadata(this.modelClass);
    String pkColumn = metadata.getPrimaryKeyColumnName();
    return this.where(pkColumn, VMCSqlOperator.EQUAL, id).findDto(dtoClass);
  }

  /**
   * Lấy một thực thể bằng khóa chính và chuyển đổi thành DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param dtoClass Lớp của DTO đích.
   * @param id Giá trị của khóa chính.
   * @return DTO nếu tìm thấy, ngược lại là {@code null}.
   */
  public <D> D getByIdGetDto(Class<D> dtoClass, Object id) {
    return this.findByIdGetDto(dtoClass, id).orElse(null);
  }

  /**
   * Lưu một thực thể từ một DTO.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dto DTO cần lưu.
   * @param saveOptions Các tùy chọn lưu.
   * @return DTO đã được cập nhật sau khi lưu.
   */
  public <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto, SaveOptions saveOptions) {
    return persistenceManager.saveDto(dto, saveOptions);
  }

  /**
   * Lưu một danh sách các thực thể từ một danh sách các DTO.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dtos Danh sách DTO cần lưu.
   * @param saveOptions Các tùy chọn lưu.
   * @return Danh sách các DTO đã được cập nhật sau khi lưu.
   */
  public <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions saveOptions) {
    return persistenceManager.saveAllDtos(dtos, saveOptions);
  }

  /**
   * Tạo một instance của {@link SqlBuilder} với trạng thái hiện tại của query builder.
   *
   * @return Một instance mới của {@code SqlBuilder}.
   */
  private SqlBuilder createSqlBuilder() {
    return new SqlBuilder(
        modelClass,
        fromAlias,
        selectColumns,
        whereClauses,
        orderByClauses,
        groupByColumns,
        limit,
        offset,
        joinClauses,
        withRelations);
  }

  /**
   * Tạo một instance của {@link QueryResultMapper} cho truy vấn hiện tại.
   *
   * @return Một instance mới của {@code QueryResultMapper}.
   */
  private QueryResultMapper createResultMapper() {
    return new QueryResultMapper(modelClass, fromAlias, withRelations);
  }

  /**
   * Tự động thêm các mệnh đề JOIN cần thiết dựa trên các mối quan hệ được chỉ định trong {@code
   * with()}.
   */
  private void prepareJoinsForWith() {
    if (withRelations.isEmpty()) return;
    EntityMetadata mainMetadata = MetadataCache.getMetadata(modelClass);

    for (String relationName : withRelations) {
      if (joinClauses.stream()
          .anyMatch(jc -> jc != null && relationName.equals(jc.getRelationName()))) continue;

      RelationMetadata relMeta = mainMetadata.getRelations().get(relationName);
      if (relMeta == null)
        throw new VMCException(
            HttpStatus.BAD_REQUEST,
            "Relation '" + relationName + "' not found in " + modelClass.getSimpleName());

      if (relMeta.getTargetEntity().equals(modelClass)) {
        continue;
      }

      String relationAlias = relationName.toLowerCase();
      EntityMetadata targetMetadata = MetadataCache.getMetadata(relMeta.getTargetEntity());

      switch (relMeta.getType()) {
        case MANY_TO_ONE:
        case ONE_TO_ONE:
          if (relMeta.getJoinColumnName() != null) {
            joinClauses.add(
                new JoinClause(
                    VMCSqlJoinType.LEFT_JOIN,
                    targetMetadata.getTableName(),
                    relationAlias,
                    fromAlias + "." + relMeta.getJoinColumnName(),
                    "=",
                    relationAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
                    (Class<? extends Model>) relMeta.getTargetEntity(),
                    relationName,
                    relMeta.getType()));
          } else {
            RelationMetadata inverseRelation =
                targetMetadata.getRelations().get(relMeta.getMappedBy());
            joinClauses.add(
                new JoinClause(
                    VMCSqlJoinType.LEFT_JOIN,
                    targetMetadata.getTableName(),
                    relationAlias,
                    fromAlias + "." + mainMetadata.getPrimaryKeyColumnName(),
                    "=",
                    relationAlias + "." + inverseRelation.getJoinColumnName(),
                    (Class<? extends Model>) relMeta.getTargetEntity(),
                    relationName,
                    relMeta.getType()));
          }
          break;
        case ONE_TO_MANY:
          RelationMetadata inverseRelation =
              targetMetadata.getRelations().get(relMeta.getMappedBy());
          joinClauses.add(
              new JoinClause(
                  VMCSqlJoinType.LEFT_JOIN,
                  targetMetadata.getTableName(),
                  relationAlias,
                  fromAlias + "." + mainMetadata.getPrimaryKeyColumnName(),
                  "=",
                  relationAlias + "." + inverseRelation.getJoinColumnName(),
                  (Class<? extends Model>) relMeta.getTargetEntity(),
                  relationName,
                  RelationMetadata.RelationType.ONE_TO_MANY));
          break;
        case MANY_TO_MANY:
          JoinTableMetadata joinTable;
          String pivotAlias = relationName + "_pivot";
          String joinColumnForThisSide;
          String joinColumnForOtherSide;

          if (relMeta.isOwningSide()) {
            joinTable = relMeta.getJoinTable();
            joinColumnForThisSide = joinTable.getJoinColumn();
            joinColumnForOtherSide = joinTable.getInverseJoinColumn();
          } else {
            EntityMetadata inverseSideEntityMetadata =
                MetadataCache.getMetadata(relMeta.getTargetEntity());
            RelationMetadata inverseSideRelMeta =
                inverseSideEntityMetadata.getRelations().get(relMeta.getMappedBy());
            if (inverseSideRelMeta == null || inverseSideRelMeta.getJoinTable() == null) {
              throw new VMCException(
                  HttpStatus.INTERNAL_SERVER_ERROR,
                  "Invalid ManyToMany mapping for relation '"
                      + relationName
                      + "'. The owning side must define @VMCJoinTable.");
            }
            joinTable = inverseSideRelMeta.getJoinTable();
            joinColumnForThisSide = joinTable.getInverseJoinColumn();
            joinColumnForOtherSide = joinTable.getJoinColumn();
          }

          joinClauses.add(
              new JoinClause(
                  VMCSqlJoinType.LEFT_JOIN,
                  joinTable.getTableName(),
                  pivotAlias,
                  fromAlias + "." + mainMetadata.getPrimaryKeyColumnName(),
                  "=",
                  pivotAlias + "." + joinColumnForThisSide,
                  null,
                  null,
                  null));
          joinClauses.add(
              new JoinClause(
                  VMCSqlJoinType.LEFT_JOIN,
                  targetMetadata.getTableName(),
                  relationAlias,
                  pivotAlias + "." + joinColumnForOtherSide,
                  "=",
                  relationAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
                  (Class<? extends Model>) relMeta.getTargetEntity(),
                  relationName,
                  RelationMetadata.RelationType.MANY_TO_MANY));
          break;
      }
    }
  }

  /**
   * Ánh xạ một hàng kết quả thành một instance Model.
   *
   * @param modelClass Lớp của model cần tạo.
   * @param row Dữ liệu của hàng.
   * @param alias Bí danh của bảng.
   * @return Một instance Model.
   */
  public Model mapRowToModel(
      Class<? extends Model> modelClass, Map<String, Object> row, String alias) {
    return createResultMapper().mapRowToModel(modelClass, row, alias);
  }

  /**
   * Ánh xạ một danh sách các thực thể sang một danh sách các DTO.
   *
   * @param <D> Kiểu của DTO.
   * @param entities Danh sách thực thể.
   * @param dtoClass Lớp của DTO đích.
   * @return Một danh sách các DTO.
   */
  public <D> List<D> mapEntitiesToDtos(List<? extends Model> entities, Class<D> dtoClass) {
    return createResultMapper().mapEntitiesToDtos(entities, dtoClass);
  }

  /**
   * Thực thi và xử lý một truy vấn đệ quy.
   *
   * @param <T> Kiểu của thực thể.
   * @param recursiveChildRel Metadata của mối quan hệ đệ quy (phía con).
   * @return Một danh sách các thực thể gốc đã được xây dựng thành cây.
   */
  private <T extends Model> List<T> getRecursive(RelationMetadata recursiveChildRel) {
    Set<Object> allIdsInHierarchy = getRecursiveIds(recursiveChildRel);
    if (allIdsInHierarchy.isEmpty()) {
      return Collections.emptyList();
    }

    VMCQueryBuilder fullTreeBuilder = VMCQueryBuilder.from(this.modelClass, this.fromAlias);
    fullTreeBuilder.whereIn(
        fromAlias + "." + MetadataCache.getMetadata(modelClass).getPrimaryKeyColumnName(),
        allIdsInHierarchy);

    fullTreeBuilder.with(this.withRelations.toArray(new String[0]));

    List<T> flatList = fullTreeBuilder.getInternal();

    QueryResultMapper mapper = createResultMapper();
    return mapper.buildTree(flatList, recursiveChildRel);
  }

  /**
   * Lấy tất cả các ID trong một hệ thống phân cấp đệ quy bằng cách sử dụng Common Table Expression
   * (CTE).
   *
   * @param recursiveChildRel Metadata của mối quan hệ đệ quy.
   * @return Một {@code Set} chứa tất cả các ID.
   */
  private Set<Object> getRecursiveIds(RelationMetadata recursiveChildRel) {
    EntityMetadata metadata = MetadataCache.getMetadata(modelClass);
    RelationMetadata parentRel =
        metadata.getRelations().values().stream()
            .filter(r -> recursiveChildRel.getMappedBy().equals(r.getFieldName()))
            .findFirst()
            .orElseThrow(
                () ->
                    new VMCException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Recursive relationship misconfigured. Could not find parent relation for "
                            + recursiveChildRel.getFieldName()));

    String tableName = metadata.getTableName();
    String pkName = metadata.getPrimaryKeyColumnName();
    String fkName = parentRel.getJoinColumnName();
    String cteName = tableName + "_cte";

    SqlBuilder tempBuilder = createSqlBuilder();
    StringBuilder whereSqlBuilder = new StringBuilder();
    Map<String, Object> params = tempBuilder.buildWhereClause(whereSqlBuilder, "anchor");

    String recursiveSql =
        String.format(
            "WITH RECURSIVE %s AS ("
                + "SELECT * FROM %s AS anchor %s"
                + " UNION "
                + "SELECT t.* FROM %s t JOIN %s ON t.%s = %s.%s"
                + ") SELECT %s FROM %s",
            cteName,
            tableName,
            whereSqlBuilder.toString(),
            tableName,
            cteName,
            fkName,
            cteName,
            pkName,
            pkName,
            cteName);

    List<Map<String, Object>> idRows = queryExecutor.execute(recursiveSql, params);
    return idRows.stream()
        .map(row -> row.get(pkName))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Tìm metadata của mối quan hệ đệ quy (tự tham chiếu) trong danh sách {@code withRelations}.
   *
   * @return Metadata của mối quan hệ đệ quy, hoặc {@code null} nếu không có.
   */
  private RelationMetadata findRecursiveChildRelation() {
    EntityMetadata mainMetadata = MetadataCache.getMetadata(modelClass);
    for (String relationName : withRelations) {
      RelationMetadata relMeta = mainMetadata.getRelations().get(relationName);
      if (relMeta != null
          && relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY
          && relMeta.getTargetEntity().equals(modelClass)) {
        return relMeta;
      }
    }
    return null;
  }
}
