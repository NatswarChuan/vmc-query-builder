package io.github.natswarchuan.vmc.core.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

import io.github.natswarchuan.vmc.core.annotation.*;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;

/**
 * Một bộ đệm (cache) để lưu trữ siêu dữ liệu (metadata) của các thực thể.
 *
 * <p>Lớp này chịu trách nhiệm phân tích các annotation trên các lớp thực thể, tạo ra đối tượng
 * {@link EntityMetadata} và lưu trữ chúng vào cache để tăng hiệu năng. Việc này giúp tránh phải
 * phân tích lại các annotation mỗi khi có yêu cầu. Cache này là an toàn cho luồng (thread-safe).
 * 
 * @author NatswarChuan
 */
public class MetadataCache {
  private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

  /**
   * Lấy siêu dữ liệu cho một lớp thực thể từ cache.
   *
   * <p>Nếu siêu dữ liệu chưa có trong cache, phương thức này sẽ phân tích lớp thực thể, tạo siêu dữ
   * liệu mới, lưu vào cache và sau đó trả về.
   *
   * @param modelClass Lớp thực thể cần lấy siêu dữ liệu.
   * @return Đối tượng {@link EntityMetadata} tương ứng với lớp thực thể.
   * @throws VMCException nếu lớp không phải là một thực thể hợp lệ (ví dụ: thiếu {@code @VMCTable}
   *     hoặc {@code @VMCPrimaryKey}).
   */
  public static EntityMetadata getMetadata(Class<?> modelClass) {
    return CACHE.computeIfAbsent(
        modelClass,
        clazz -> {
          VMCTable tableAnnotation = clazz.getAnnotation(VMCTable.class);
          if (tableAnnotation == null) {
            throw new VMCException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Lớp " + clazz.getSimpleName() + " phải có annotation @VMCTable.");
          }
          String tableName = tableAnnotation.name();
          Map<String, String> fieldToColumnMap = new HashMap<>();
          Map<String, RelationMetadata> relations = new HashMap<>();

          final AtomicReference<String> pkFieldNameRef = new AtomicReference<>();
          final AtomicReference<String> pkColumnNameRef = new AtomicReference<>();

          Stream<Field> fieldStream = Stream.empty();
          Class<?> currentClass = clazz;
          while (currentClass != null
              && !currentClass.equals(Object.class)
              && !currentClass.equals(Model.class)) {
            fieldStream =
                Stream.concat(fieldStream, Arrays.stream(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
          }

          fieldStream.forEach(
              field -> {
                if (field.isAnnotationPresent(VMCPrimaryKey.class)) {
                  VMCPrimaryKey pkAnn = field.getAnnotation(VMCPrimaryKey.class);
                  pkFieldNameRef.set(field.getName());
                  pkColumnNameRef.set(pkAnn.name());
                  fieldToColumnMap.put(field.getName(), pkAnn.name());
                } else if (field.isAnnotationPresent(VMCColumn.class)) {
                  fieldToColumnMap.put(
                      field.getName(), field.getAnnotation(VMCColumn.class).name());
                }

                VMCJoinColumn joinColumnAnn = field.getAnnotation(VMCJoinColumn.class);
                String joinColumnName = (joinColumnAnn != null) ? joinColumnAnn.name() : null;
                boolean isNullable = (joinColumnAnn != null) ? joinColumnAnn.nullable() : true;
                boolean orphanRemoval = field.isAnnotationPresent(VMCOrphanRemoval.class);

                if (field.isAnnotationPresent(VMCOneToOne.class)) {
                  VMCOneToOne ann = field.getAnnotation(VMCOneToOne.class);
                  relations.put(
                      field.getName(),
                      RelationMetadata.builder()
                          .fieldName(field.getName())
                          .type(RelationMetadata.RelationType.ONE_TO_ONE)
                          .targetEntity((Class<?>) field.getType())
                          .mappedBy(ann.mappedBy())
                          .joinColumnName(joinColumnName)
                          .foreignKeyNullable(isNullable)
                          .orphanRemoval(orphanRemoval)
                          .build());
                } else if (field.isAnnotationPresent(VMCOneToMany.class)) {
                  VMCOneToMany ann = field.getAnnotation(VMCOneToMany.class);
                  relations.put(
                      field.getName(),
                      RelationMetadata.builder()
                          .fieldName(field.getName())
                          .type(RelationMetadata.RelationType.ONE_TO_MANY)
                          .targetEntity(getGenericType(field))
                          .mappedBy(ann.mappedBy())
                          .orphanRemoval(orphanRemoval)
                          .build());
                } else if (field.isAnnotationPresent(VMCManyToOne.class)) {
                  relations.put(
                      field.getName(),
                      RelationMetadata.builder()
                          .fieldName(field.getName())
                          .type(RelationMetadata.RelationType.MANY_TO_ONE)
                          .targetEntity((Class<?>) field.getType())
                          .joinColumnName(joinColumnName)
                          .foreignKeyNullable(isNullable)
                          .build());
                } else if (field.isAnnotationPresent(VMCManyToMany.class)) {
                  VMCManyToMany ann = field.getAnnotation(VMCManyToMany.class);
                  relations.put(
                      field.getName(),
                      RelationMetadata.builder()
                          .fieldName(field.getName())
                          .type(RelationMetadata.RelationType.MANY_TO_MANY)
                          .targetEntity(getGenericType(field))
                          .mappedBy(ann.mappedBy())
                          .joinTable(getJoinTableMetadata(field))
                          .build());
                }
              });

          String pkFieldName = pkFieldNameRef.get();
          if (pkFieldName == null) {
            throw new VMCException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Thực thể " + clazz.getSimpleName() + " phải có một @VMCPrimaryKey.");
          }

          return new EntityMetadata(
              tableName, fieldToColumnMap, pkFieldName, pkColumnNameRef.get(), relations);
        });
  }

  /**
   * Phân tích và trích xuất siêu dữ liệu từ annotation {@link VMCJoinTable}.
   *
   * @param field Trường có chứa annotation.
   * @return Một đối tượng {@link JoinTableMetadata} hoặc {@code null} nếu không có annotation.
   */
  private static JoinTableMetadata getJoinTableMetadata(Field field) {
    if (!field.isAnnotationPresent(VMCJoinTable.class)) return null;
    VMCJoinTable ann = field.getAnnotation(VMCJoinTable.class);
    return JoinTableMetadata.builder()
        .tableName(ann.name())
        .joinColumn(ann.joinColumns()[0].name())
        .inverseJoinColumn(ann.inverseJoinColumns()[0].name())
        .build();
  }

  /**
   * Lấy kiểu dữ liệu generic của một trường là Collection (ví dụ: List&lt;User&gt; -> User.class).
   *
   * @param field Trường cần phân tích.
   * @return Lớp của kiểu generic, hoặc {@code null} nếu không phải là Collection.
   */
  private static Class<?> getGenericType(Field field) {
    if (Collection.class.isAssignableFrom(field.getType())) {
      ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
      return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }
    return null;
  }
}
