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
 * Cung cấp một API linh hoạt (fluent API) để xây dựng và thực thi các câu lệnh SQL. Lớp này điều
 * phối các helper để xây dựng SQL, thực thi và ánh xạ kết quả.
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

  @PostConstruct
  public void init() {
    VMCQueryBuilder.queryExecutor = injectedQueryExecutor;
    VMCQueryBuilder.persistenceManager = injectedPersistenceManager;
  }

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

  public VMCQueryBuilder() {}

  private VMCQueryBuilder(Class<? extends Model> modelClass, String alias) {
    this.modelClass = modelClass;
    this.fromAlias = alias;
  }

  public static VMCQueryBuilder from(Class<? extends Model> modelClass, String alias) {
    return new VMCQueryBuilder(modelClass, alias);
  }

  public static VMCQueryBuilder from(Class<? extends Model> modelClass) {
    String alias = modelClass.getSimpleName().substring(0, 1).toLowerCase();
    return new VMCQueryBuilder(modelClass, alias);
  }

  public VMCQueryBuilder select(String... columns) {
    this.selectColumns.clear();
    this.selectColumns.addAll(Arrays.asList(columns));
    return this;
  }

  public VMCQueryBuilder where(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.AND, column, operator, value));
    return this;
  }

  public VMCQueryBuilder orWhere(String column, VMCSqlOperator operator, Object value) {
    this.whereClauses.add(new WhereClause(VMCLogicalOperator.OR, column, operator, value));
    return this;
  }

  public VMCQueryBuilder whereIn(String column, Collection<?> values) {
    this.whereClauses.add(
        new WhereClause(VMCLogicalOperator.AND, column, VMCSqlOperator.IN, values));
    return this;
  }

  public VMCQueryBuilder groupBy(String... columns) {
    this.groupByColumns.addAll(Arrays.asList(columns));
    return this;
  }

  public VMCQueryBuilder limit(int limit) {
    this.limit = limit;
    return this;
  }

  public VMCQueryBuilder offset(int offset) {
    this.offset = offset;
    return this;
  }

  public VMCQueryBuilder orderBy(String column, VMCSortDirection direction) {
    this.orderByClauses.add(new OrderByClause(column, direction));
    return this;
  }

  public VMCQueryBuilder with(String... relations) {
    this.withRelations.addAll(Arrays.asList(relations));
    return this;
  }

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

  public <T extends Model> T getFirst() {
    this.limit(1);
    List<T> results = get();
    return results.isEmpty() ? null : results.get(0);
  }

  public <T extends Model> Optional<T> findFirst() {
    return Optional.ofNullable(this.getFirst());
  }

  public List<Map<String, Object>> getRaw() {
    prepareJoinsForWith();
    SqlBuilder sqlBuilder = createSqlBuilder();
    PreparedQuery preparedQuery = sqlBuilder.build();
    return queryExecutor.execute(preparedQuery.getSql(), preparedQuery.getParams());
  }

  public <T extends Model> List<T> get() {
    RelationMetadata recursiveChildRel = findRecursiveChildRelation();
    if (recursiveChildRel != null) {
      return getRecursive(recursiveChildRel);
    }
    return getInternal();
  }

  private <T extends Model> List<T> getInternal() {
    List<Map<String, Object>> flatResults = getRaw();
    QueryResultMapper mapper = createResultMapper();
    return mapper.processFlatResults(flatResults, this.joinClauses);
  }

  public <D> D getDto(Class<D> dtoClass) {
    this.limit(1);
    List<D> results = getDtos(dtoClass);
    return results.isEmpty() ? null : results.get(0);
  }

  public <D> Optional<D> findDto(Class<D> dtoClass) {
    return Optional.ofNullable(this.getDto(dtoClass));
  }

  public <D> List<D> getDtos(Class<D> dtoClass) {
    List<Model> entities = get();
    QueryResultMapper mapper = createResultMapper();
    return mapper.mapEntitiesToDtos(entities, dtoClass);
  }

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

  public <T extends Model> Paginator<T> paginate(int page, int perPage) {
    long total = this.count();
    if (total == 0) {
      return new Paginator<>(Collections.emptyList(), 0, perPage, page);
    }

    this.limit(perPage).offset((page - 1) * perPage);
    List<T> data = get();

    return new Paginator<>(data, total, perPage, page);
  }

  public <D> Paginator<D> paginateDto(int page, int perPage, Class<D> dtoClass) {
    Paginator<Model> entityPaginator = paginate(page, perPage);
    QueryResultMapper mapper = createResultMapper();
    List<D> dtoList = mapper.mapEntitiesToDtos(entityPaginator.getData(), dtoClass);
    return new Paginator<>(dtoList, entityPaginator.getTotal(), perPage, page);
  }

  public <T extends Model> Optional<T> findById(Object id) {
    EntityMetadata metadata = MetadataCache.getMetadata(this.modelClass);
    String pkColumn = metadata.getPrimaryKeyColumnName();
    return (Optional<T>) this.where(pkColumn, VMCSqlOperator.EQUAL, id).findFirst();
  }

  public <T extends Model> T getById(Object id) {
    return (T) this.findById(id).orElse(null);
  }

  public <D> Optional<D> findByIdGetDto(Class<D> dtoClass, Object id) {
    EntityMetadata metadata = MetadataCache.getMetadata(this.modelClass);
    String pkColumn = metadata.getPrimaryKeyColumnName();
    return this.where(pkColumn, VMCSqlOperator.EQUAL, id).findDto(dtoClass);
  }

  public <D> D getByIdGetDto(Class<D> dtoClass, Object id) {
    return this.findByIdGetDto(dtoClass, id).orElse(null);
  }

  public <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto, SaveOptions saveOptions) {
    return persistenceManager.saveDto(dto, saveOptions);
  }

  public <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions saveOptions) {
    return persistenceManager.saveAllDtos(dtos, saveOptions);
  }

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

  private QueryResultMapper createResultMapper() {
    return new QueryResultMapper(modelClass, fromAlias, withRelations);
  }

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

  public Model mapRowToModel(
      Class<? extends Model> modelClass, Map<String, Object> row, String alias) {
    return createResultMapper().mapRowToModel(modelClass, row, alias);
  }

  public <D> List<D> mapEntitiesToDtos(List<? extends Model> entities, Class<D> dtoClass) {
    return createResultMapper().mapEntitiesToDtos(entities, dtoClass);
  }

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
