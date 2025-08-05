package io.github.natswarchuan.vmc.core.persistence.lazy;

import java.util.Collection;
import java.util.Collections;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.VMCSqlOperator;

/**
 * Một triển khai của {@link LazyLoader} để tải dữ liệu cho mối quan hệ One-to-Many.
 *
 * <p>Lớp này xây dựng và thực thi một câu lệnh SQL để lấy về tất cả các thực thể ở phía "nhiều"
 * (many) có khóa ngoại tham chiếu đến thực thể sở hữu (phía "một").
 *
 * @author NatswarChuan
 */
@SuppressWarnings("unchecked")
public class OneToManyLoader implements LazyLoader<Model> {
  private final Model owner;
  private final RelationMetadata relMeta;

  /**
   * Khởi tạo một loader mới cho quan hệ One-to-Many.
   *
   * @param owner Thực thể sở hữu mối quan hệ (phía "một").
   * @param relMeta Siêu dữ liệu của mối quan hệ cần tải.
   */
  public OneToManyLoader(Model owner, RelationMetadata relMeta) {
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

    RelationMetadata inverseRelation = targetMetadata.getRelations().get(relMeta.getMappedBy());
    String foreignKey = inverseRelation.getJoinColumnName();
    String localKey = ownerMetadata.getPrimaryKeyColumnName();
    Object ownerId = owner.getAttribute(localKey);

    if (ownerId == null) return Collections.emptyList();

    return VMCQueryBuilder.from((Class<? extends Model>) relMeta.getTargetEntity())
        .where(foreignKey, VMCSqlOperator.EQUAL, ownerId)
        .get();
  }
}
