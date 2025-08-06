package io.github.natswarchuan.vmc.core.persistence.handler;

import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.RelationMetadata;
import io.github.natswarchuan.vmc.core.persistence.mapper.GenericQueryExecutorMapper;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import io.github.natswarchuan.vmc.core.util.DataConverter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Chịu trách nhiệm thực thi các câu lệnh INSERT và UPDATE cấp thấp.
 *
 * <p>Lớp này xây dựng các câu lệnh SQL động và các tham số tương ứng dựa trên metadata của thực
 * thể, sau đó ủy quyền việc thực thi cho {@link GenericQueryExecutorMapper}.
 *
 * @author NatswarChuan
 */
public class CrudExecutor {

  private GenericQueryExecutorMapper queryExecutor;

  /** Khởi tạo một instance mới của CrudExecutor. */
  public CrudExecutor() {}

  /**
   * Lấy instance của {@code GenericQueryExecutorMapper} một cách lười biếng (lazy).
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
   * Thực thi một thao tác INSERT cho một thực thể.
   *
   * <p>Phương thức này xây dựng câu lệnh INSERT dựa trên các trường có giá trị khác null trong thực
   * thể. Sau khi thực thi, nếu cơ sở dữ liệu trả về một khóa chính tự tăng, nó sẽ được gán lại vào
   * thực thể.
   *
   * @param model Thực thể cần chèn vào cơ sở dữ liệu.
   * @param metadata Metadata của thực thể.
   */
  public void insert(Model model, EntityMetadata metadata) {
    try {
      String tableName = metadata.getTableName();
      Map<String, Object> params = new HashMap<>();
      Map<String, String> columnPlaceholders = new LinkedHashMap<>();

      prepareParamsForInsert(model, metadata, params, columnPlaceholders);

      params.put("id", null);
      if (columnPlaceholders.isEmpty()) {

        String pkColumn = metadata.getPrimaryKeyColumnName();
        String insertSql =
            String.format("INSERT INTO %s (%s) VALUES (DEFAULT)", tableName, pkColumn);
        getQueryExecutor().insert(insertSql, params);
      } else {
        String columns = String.join(", ", columnPlaceholders.keySet());
        String values = String.join(", ", columnPlaceholders.values());
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
        getQueryExecutor().insert(sql, params);
      }

      Object generatedId = params.get("id");
      if (generatedId != null) {
        Field pkField = findField(model.getClass(), metadata.getPrimaryKeyFieldName());
        pkField.setAccessible(true);
        pkField.set(model, DataConverter.convertValue(generatedId, pkField.getType()));
      }
    } catch (Exception e) {
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during insert operation.", e);
    }
  }

  /**
   * Thực thi một thao tác UPDATE cho một thực thể.
   *
   * <p>Phương thức này xây dựng câu lệnh UPDATE dựa trên tất cả các trường của thực thể (ngoại trừ
   * khóa chính) và thực thi nó dựa trên giá trị của khóa chính.
   *
   * @param model Thực thể cần cập nhật.
   * @param metadata Metadata của thực thể.
   */
  public void update(Model model, EntityMetadata metadata) {
    try {
      String tableName = metadata.getTableName();
      String pkColumnName = metadata.getPrimaryKeyColumnName();
      Object pkValue = getPrimaryKeyValue(model, metadata);

      if (pkValue == null) {
        throw new VMCException(
            HttpStatus.BAD_REQUEST, "Cannot update entity with null primary key.");
      }

      Map<String, Object> params = new HashMap<>();
      List<String> setClauses = new ArrayList<>();

      prepareParamsForUpdate(model, metadata, params, setClauses);

      if (setClauses.isEmpty()) {
        return;
      }

      params.put("pkValue", pkValue);
      String sql =
          String.format(
              "UPDATE %s SET %s WHERE %s = #{params.pkValue}",
              tableName, String.join(", ", setClauses), pkColumnName);

      getQueryExecutor().update(sql, params);
    } catch (Exception e) {
      throw new VMCException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during update operation.", e);
    }
  }

  /**
   * Chuẩn bị các tham số và placeholder cho câu lệnh INSERT.
   *
   * @param model Thực thể nguồn.
   * @param metadata Metadata của thực thể.
   * @param params Map để lưu trữ các giá trị tham số.
   * @param columnPlaceholders Map để lưu trữ tên cột và placeholder tương ứng.
   * @throws Exception nếu có lỗi reflection.
   */
  private void prepareParamsForInsert(
      Model model,
      EntityMetadata metadata,
      Map<String, Object> params,
      Map<String, String> columnPlaceholders)
      throws Exception {
    for (Map.Entry<String, String> entry : metadata.getFieldToColumnMap().entrySet()) {
      String fieldName = entry.getKey();
      if (fieldName.equals(metadata.getPrimaryKeyFieldName())) {
        continue;
      }

      Field field = findField(model.getClass(), fieldName);
      field.setAccessible(true);
      Object value = field.get(model);
      if (value != null) {
        columnPlaceholders.put(entry.getValue(), "#{params." + fieldName + "}");
        params.put(fieldName, value);
      }
    }

    for (RelationMetadata relMeta : metadata.getRelations().values()) {
      if (relMeta.isOwningSideOfAssociation()) {
        Field field = findField(model.getClass(), relMeta.getFieldName());
        field.setAccessible(true);
        Object relatedObject = field.get(model);
        if (relatedObject instanceof Model) {
          Object fkValue = getPrimaryKeyValue((Model) relatedObject);
          if (fkValue != null) {
            String paramName = relMeta.getFieldName() + "_fk";
            columnPlaceholders.put(relMeta.getJoinColumnName(), "#{params." + paramName + "}");
            params.put(paramName, fkValue);
          }
        }
      }
    }
  }

  /**
   * Chuẩn bị các tham số và mệnh đề SET cho câu lệnh UPDATE.
   *
   * @param model Thực thể nguồn.
   * @param metadata Metadata của thực thể.
   * @param params Map để lưu trữ các giá trị tham số.
   * @param setClauses Danh sách để lưu trữ các mệnh đề "cột = giá trị".
   * @throws Exception nếu có lỗi reflection.
   */
  private void prepareParamsForUpdate(
      Model model, EntityMetadata metadata, Map<String, Object> params, List<String> setClauses)
      throws Exception {
    for (Map.Entry<String, String> entry : metadata.getFieldToColumnMap().entrySet()) {
      String fieldName = entry.getKey();
      if (fieldName.equals(metadata.getPrimaryKeyFieldName())) {
        continue;
      }

      Field field = findField(model.getClass(), fieldName);
      field.setAccessible(true);
      Object value = field.get(model);
      setClauses.add(String.format("%s = #{params.%s}", entry.getValue(), fieldName));
      params.put(fieldName, value);
    }

    for (RelationMetadata relMeta : metadata.getRelations().values()) {
      if (relMeta.isOwningSideOfAssociation()) {
        Field field = findField(model.getClass(), relMeta.getFieldName());
        field.setAccessible(true);
        Object relatedObject = field.get(model);
        Object fkValue =
            (relatedObject instanceof Model) ? getPrimaryKeyValue((Model) relatedObject) : null;
        String paramName = relMeta.getFieldName() + "_fk";
        setClauses.add(String.format("%s = #{params.%s}", relMeta.getJoinColumnName(), paramName));
        params.put(paramName, fkValue);
      }
    }
  }

  /**
   * Lấy giá trị khóa chính của một thực thể mà không cần truyền metadata.
   *
   * @param model Thực thể cần lấy khóa chính.
   * @return Giá trị của khóa chính.
   * @throws Exception nếu có lỗi.
   */
  private Object getPrimaryKeyValue(Model model) throws Exception {
    EntityMetadata metadata = EntityMetadata.of(getUnproxiedClass(model.getClass()));
    return getPrimaryKeyValue(model, metadata);
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
