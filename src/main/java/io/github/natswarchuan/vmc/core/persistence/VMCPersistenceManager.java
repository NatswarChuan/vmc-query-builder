package io.github.natswarchuan.vmc.core.persistence;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.handler.CascadeRemoveHandler;
import io.github.natswarchuan.vmc.core.persistence.handler.CrudExecutor;
import io.github.natswarchuan.vmc.core.persistence.handler.RelationshipSynchronizer;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý các thao tác bền bỉ (persistence) cho các thực thể (entity).
 *
 * <p>Lớp này là thành phần trung tâm của tầng persistence, điều phối các handler chuyên biệt để
 * thực hiện việc lưu, xóa, và đồng bộ hóa đồ thị đối tượng (object graph) một cách toàn vẹn. Nó
 * trừu tượng hóa các logic phức tạp như lưu theo tầng (cascading) và quản lý các mối quan hệ.
 *
 * @author NatswarChuan
 */
@Component
public class VMCPersistenceManager {

  private final GenericQueryExecutorMapper queryExecutor;
  private final CrudExecutor crudExecutor;
  private final RelationshipSynchronizer relationshipSynchronizer;
  private final CascadeRemoveHandler cascadeRemoveHandler;

  /**
   * Khởi tạo một instance mới của VMCPersistenceManager.
   *
   * @param queryExecutor Mapper để thực thi các câu lệnh SQL cấp thấp.
   */
  public VMCPersistenceManager(GenericQueryExecutorMapper queryExecutor) {
    this.queryExecutor = queryExecutor;
    this.crudExecutor = new CrudExecutor();
    this.relationshipSynchronizer = new RelationshipSynchronizer(this);
    this.cascadeRemoveHandler = new CascadeRemoveHandler();
  }

  /**
   * Lưu một đồ thị đối tượng bắt đầu từ một thực thể gốc.
   *
   * @param model Thực thể gốc cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu, ví dụ như cascade.
   */
  @Transactional
  public void save(Model model, SaveOptions options) {
    saveGraph(model, options, new IdentityHashMap<>());
  }

  /**
   * Lưu một tập hợp các đồ thị đối tượng.
   *
   * @param models Một {@code Iterable} chứa các thực thể gốc cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   */
  @Transactional
  public void saveAll(Iterable<? extends Model> models, SaveOptions options) {
    if (models == null) {
      return;
    }
    IdentityHashMap<Model, Model> processed = new IdentityHashMap<>();
    for (Model model : models) {
      saveGraph(model, options, processed);
    }
  }

  /**
   * Lưu một thực thể từ một đối tượng DTO.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dto DTO chứa dữ liệu cần lưu.
   * @param options Các tùy chọn lưu.
   * @return DTO đã được cập nhật với dữ liệu từ thực thể đã lưu.
   */
  @Transactional
  public <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto, SaveOptions options) {
    if (dto == null) {
      return null;
    }
    E entity = dto.toEntity();
    save(entity, options);
    return (D) dto.toDto(entity);
  }

  /**
   * Lưu một tập hợp các thực thể từ một {@code Iterable} các DTO.
   *
   * @param <E> Kiểu của Entity.
   * @param <D> Kiểu của DTO.
   * @param dtos Một {@code Iterable} các DTO cần lưu.
   * @param options Các tùy chọn lưu.
   * @return Một danh sách các DTO đã được cập nhật.
   */
  @Transactional
  public <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions options) {
    if (dtos == null) {
      return Collections.emptyList();
    }
    List<Model> entitiesToSave = new ArrayList<>();
    List<D> originalDtos = new ArrayList<>();
    dtos.forEach(
        dto -> {
          originalDtos.add(dto);
          entitiesToSave.add(dto.toEntity());
        });

    saveAll(entitiesToSave, options);

    List<D> savedDtos = new ArrayList<>();
    for (int i = 0; i < entitiesToSave.size(); i++) {
      savedDtos.add((D) originalDtos.get(i).toDto(entitiesToSave.get(i)));
    }
    return savedDtos;
  }

  /**
   * Xóa một thực thể khỏi cơ sở dữ liệu.
   *
   * <p>Phương thức này sẽ xử lý các hành động xóa theo tầng trước, sau đó mới xóa bản thân thực
   * thể.
   *
   * @param model Thực thể cần xóa.
   */
  @Transactional
  public void remove(Model model) {
    if (model == null) {
      return;
    }
    try {
      EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));
      Object pkValue = getPrimaryKeyValue(model, metadata);
      if (pkValue == null) {
        return;
      }

      cascadeRemoveHandler.handleCascades(model);

      String sql =
          String.format(
              "DELETE FROM %s WHERE %s = #{params.pkValue}",
              metadata.getTableName(), metadata.getPrimaryKeyColumnName());
      queryExecutor.delete(sql, Map.of("pkValue", pkValue));

    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error when deleting entity: " + model.getClass().getSimpleName(),
          e);
    }
  }

  /**
   * Lưu một đồ thị đối tượng một cách đệ quy.
   *
   * <p>Đây là phương thức cốt lõi của logic lưu. Nó thực hiện theo thứ tự sau:
   *
   * <ol>
   *   <li>Kiểm tra xem đối tượng đã được xử lý chưa để tránh vòng lặp.
   *   <li>Lưu các thực thể ở phía "sở hữu" của các mối quan hệ To-One để đảm bảo khóa ngoại tồn
   *       tại.
   *   <li>Lưu (INSERT hoặc UPDATE) thực thể hiện tại.
   *   <li>Đánh dấu thực thể hiện tại là đã xử lý.
   *   <li>Đồng bộ hóa các mối quan hệ To-Many và phía nghịch đảo của To-One nếu được chỉ định trong
   *       {@code SaveOptions}.
   * </ol>
   *
   * @param model Thực thể hiện tại trong đồ thị cần lưu.
   * @param options Các tùy chọn lưu.
   * @param processedEntities Một {@code IdentityHashMap} để theo dõi các thực thể đã được xử lý.
   */
  @Transactional
  public void saveGraph(Model model, SaveOptions options, Map<Model, Model> processedEntities) {
    if (model == null || processedEntities.containsKey(model)) {
      return;
    }

    try {
      EntityMetadata metadata = MetadataCache.getMetadata(getUnproxiedClass(model.getClass()));

      for (RelationMetadata relMeta : metadata.getRelations().values()) {
        if (relMeta.isOwningSideOfAssociation()) {
          Field field = findField(model.getClass(), relMeta.getFieldName());
          field.setAccessible(true);
          Object relatedValue = field.get(model);
          if (relatedValue instanceof Model) {
            saveGraph((Model) relatedValue, options, processedEntities);
          }
        }
      }

      Object pkValue = getPrimaryKeyValue(model, metadata);
      boolean isNew =
          (pkValue == null || (pkValue instanceof Number && ((Number) pkValue).longValue() == 0));
      if (isNew) {
        crudExecutor.insert(model, metadata);
      } else {
        crudExecutor.update(model, metadata);
      }
      processedEntities.put(model, model);

      Set<String> relationsToCascade = options.getRelationsToCascade();
      for (String relationName : relationsToCascade) {
        RelationMetadata relMeta = metadata.getRelations().get(relationName);
        if (relMeta == null) {
          continue;
        }

        Field field = findField(model.getClass(), relationName);
        field.setAccessible(true);
        Object relatedValue = field.get(model);
        if (relatedValue == null) {
          continue;
        }

        if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_ONE) {
          if (relMeta.isInverseSide()) {
            Model relatedModel = (Model) relatedValue;
            saveGraph(relatedModel, options, processedEntities);
          }
        } else if (relMeta.getType() == RelationMetadata.RelationType.ONE_TO_MANY) {
          if (relMeta.isInverseSide()) {
            relationshipSynchronizer.synchronizeOneToMany(
                model, relMeta, (Collection<?>) relatedValue, options, processedEntities);
          }
        } else if (relMeta.getType() == RelationMetadata.RelationType.MANY_TO_MANY) {
          relationshipSynchronizer.synchronizeManyToMany(
              model, relMeta, (Collection<?>) relatedValue, options, processedEntities);
        }
      }
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error during save graph operation.", e);
    }
  }

  /**
   * Lấy giá trị khóa chính của một thực thể bằng reflection.
   *
   * @param model Thực thể cần lấy khóa chính.
   * @param metadata Metadata của thực thể.
   * @return Giá trị của khóa chính.
   * @throws Exception nếu có lỗi khi truy cập trường.
   */
  private Object getPrimaryKeyValue(Model model, EntityMetadata metadata) throws Exception {
    Field pkField = findField(model.getClass(), metadata.getPrimaryKeyFieldName());
    pkField.setAccessible(true);
    return pkField.get(model);
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó.
   *
   * @param clazz Lớp bắt đầu tìm kiếm.
   * @param fieldName Tên của trường cần tìm.
   * @return Đối tượng {@code Field} nếu tìm thấy.
   * @throws VMCException nếu không tìm thấy trường.
   */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null && !current.equals(Object.class)) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new VMCException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }

  /**
   * Lấy lớp thực sự của một đối tượng có thể là proxy của CGLIB.
   *
   * @param clazz Lớp cần kiểm tra.
   * @return Lớp cha nếu là proxy, ngược lại trả về chính nó.
   */
  private Class<?> getUnproxiedClass(Class<?> clazz) {
    if (clazz.getName().contains("$$EnhancerByCGLIB$$")) {
      return clazz.getSuperclass();
    }
    return clazz;
  }
}
