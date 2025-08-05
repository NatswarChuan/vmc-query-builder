package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.Collection;
import java.util.Collections;
import org.springframework.http.HttpStatus;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.JoinTableMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlJoinType;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

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
  @SuppressWarnings("unused")
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

    if (joinTable == null) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Không thể xác định siêu dữ liệu JoinTable cho quan hệ ManyToMany trên trường '"
              + relMeta.getFieldName()
              + "' trong thực thể "
              + owner.getClass().getSimpleName());
    }

    Object ownerPkValue = owner.getAttribute(ownerMetadata.getPrimaryKeyColumnName());
    if (ownerPkValue == null) {
      return Collections.emptyList();
    }

    Class<? extends Model> targetClass = (Class<? extends Model>) relMeta.getTargetEntity();
    String targetAlias = targetClass.getSimpleName().substring(0, 1).toLowerCase();
    String pivotAlias = relMeta.getFieldName() + "_pivot";

    VMCQueryBuilder query = VMCQueryBuilder.from(targetClass, targetAlias);

    query.join(
        VMCSqlJoinType.JOIN,
        joinTable.getTableName(),
        pivotAlias,
        targetAlias + "." + targetMetadata.getPrimaryKeyColumnName(),
        "=",
        pivotAlias + "." + pivotColumnForTarget);

    query.where(pivotAlias + "." + pivotColumnForOwner, VMCSqlOperator.EQUAL, ownerPkValue);

    return query.get();
  }
}
