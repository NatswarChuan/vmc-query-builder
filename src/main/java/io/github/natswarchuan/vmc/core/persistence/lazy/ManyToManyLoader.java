package io.github.natswarchuan.vmc.core.persistence.lazy;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Một triển khai của {@link LazyLoader} để tải dữ liệu cho mối quan hệ Many-to-Many.
 *
 * <p>Lớp này xây dựng và thực thi một câu lệnh SQL phức tạp, liên kết (join) qua bảng trung gian
 * (pivot table) để lấy về các thực thể liên quan.
 *
 * @author NatswarChuan
 */
@SuppressWarnings("unchecked")
public class ManyToManyLoader implements LazyLoader<Model> {
  private final Model owner;
  private final RelationMetadata relMeta;

  /**
   * Khởi tạo một loader mới cho quan hệ Many-to-Many.
   *
   * @param owner Thực thể sở hữu mối quan hệ.
   * @param relMeta Siêu dữ liệu của mối quan hệ cần tải.
   */
  public ManyToManyLoader(Model owner, RelationMetadata relMeta) {
    this.owner = owner;
    this.relMeta = relMeta;
  }

  /**
   * Tải collection các thực thể liên quan.
   *
   * @return Một collection các thực thể liên quan.
   */
  @Override
  public Collection<Model> load() {
    EntityMetadata ownerMetadata = MetadataCache.getMetadata(owner.getClass());
    EntityMetadata targetMetadata = MetadataCache.getMetadata(relMeta.getTargetEntity());

    JoinTableMetadata joinTable;
    String pivotColumnForOwner;
    String pivotColumnForTarget;

    if (relMeta.getMappedBy() == null || relMeta.getMappedBy().isEmpty()) {
      joinTable = relMeta.getJoinTable();
      pivotColumnForOwner = joinTable.getJoinColumn();
      pivotColumnForTarget = joinTable.getInverseJoinColumn();
    } else {
      EntityMetadata owningSideEntityMetadata =
          MetadataCache.getMetadata(relMeta.getTargetEntity());
      RelationMetadata owningSideRelMeta =
          owningSideEntityMetadata.getRelations().get(relMeta.getMappedBy());
      joinTable = owningSideRelMeta.getJoinTable();

      pivotColumnForOwner = joinTable.getInverseJoinColumn();
      pivotColumnForTarget = joinTable.getJoinColumn();
    }

    Object ownerPkValue = owner.getAttribute(ownerMetadata.getPrimaryKeyColumnName());
    if (ownerPkValue == null) {
      return Collections.emptyList();
    }

    Class<? extends Model> targetClass = (Class<? extends Model>) relMeta.getTargetEntity();
    String targetAlias = targetClass.getSimpleName().substring(0, 1).toLowerCase();
    String pivotAlias = relMeta.getFieldName() + "_pivot";

    VMCQueryBuilder query = VMCQueryBuilder.from(targetClass, targetAlias).disableRecursion();

    query.join(
        VMCSqlJoinType.JOIN,
        joinTable.getTableName(),
        pivotAlias,
        targetAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
        "=",
        pivotAlias + "." + pivotColumnForTarget);

    query.where(pivotAlias + "." + pivotColumnForOwner, VMCSqlOperator.EQUAL, ownerPkValue);

    Collection<Model> initialCollection = query.get();

    if (initialCollection.isEmpty()) {
      return initialCollection;
    }

    RelationMetadata childrenRel = targetMetadata.getRelations().get("children");
    if (childrenRel != null && childrenRel.getTargetEntity().equals(targetClass)) {

      Set<Object> parentIds =
          initialCollection.stream().map(Model::getPrimaryKey).collect(Collectors.toSet());

      RelationMetadata parentRel = targetMetadata.getRelations().get(childrenRel.getMappedBy());
      String fkColumn = parentRel.getJoinColumnName();

      List<Model> allChildren =
          VMCQueryBuilder.from(targetClass).whereIn(fkColumn, parentIds).get();

      Map<Object, List<Model>> childrenByParentId =
          allChildren.stream()
              .collect(Collectors.groupingBy(child -> child.getAttribute(fkColumn)));

      initialCollection.forEach(
          parent -> {
            try {
              Object parentId = parent.getPrimaryKey();
              List<Model> children = childrenByParentId.getOrDefault(parentId, new ArrayList<>());

              Field childrenField = findField(parent.getClass(), "children");
              childrenField.setAccessible(true);

              if (List.class.isAssignableFrom(childrenField.getType())) {
                childrenField.set(parent, new ArrayList<>(children));
              } else if (Set.class.isAssignableFrom(childrenField.getType())) {
                childrenField.set(parent, new HashSet<>(children));
              }

            } catch (Exception e) {
              throw new VMCException(
                  HttpStatus.INTERNAL_SERVER_ERROR, "Failed to attach children to entity", e);
            }
          });
    }

    return initialCollection;
  }

  /**
   * Finds a field in a class or its superclasses.
   *
   * @param clazz The class to search in.
   * @param fieldName The name of the field to find.
   * @return The found Field object.
   * @throws NoSuchFieldException if the field is not found.
   */
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
        "Field '" + fieldName + "' not found in class hierarchy for " + clazz.getName());
  }
}
