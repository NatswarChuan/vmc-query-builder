package io.github.natswarchuan.vmc.core.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.util.BeanUtil;
import io.github.natswarchuan.vmc.core.util.DataConverter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class Model {

  @JsonIgnore
  protected Map<String, Object> attributes = new HashMap<>();

  @JsonIgnore
  private transient Object primaryKey;

  /**
   * Gán trực tiếp một Map các thuộc tính cho đối tượng, đồng thời điền dữ liệu
   * vào các trường (field) tương ứng của lớp con.
   * Phương thức này thay thế map hiện tại.
   */
  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;

    // Lấy metadata để biết cách ánh xạ từ tên cột sang tên trường
    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    Map<String, String> columnToFieldMap =
        metadata.getFieldToColumnMap().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    // Duyệt qua dữ liệu và gán vào các trường tương ứng
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String columnName = entry.getKey();
      String fieldName = columnToFieldMap.get(columnName);

      if (fieldName != null) {
        try {
          Field field = findField(this.getClass(), fieldName);
          field.setAccessible(true);
          // Chuyển đổi kiểu dữ liệu nếu cần (ví dụ: Timestamp -> LocalDateTime)
          Object convertedValue = DataConverter.convertValue(entry.getValue(), field.getType());
          field.set(this, convertedValue);
        } catch (Exception e) {
          // Bỏ qua lỗi nếu không thể set field, dữ liệu vẫn còn trong map
        }
      }
    }
  }

  /**
   * Lấy giá trị của một thuộc tính cụ thể.
   */
  public Object getAttribute(String key) {
    return this.attributes.get(key);
  }

  /**
   * Gán giá trị cho một thuộc tính cụ thể.
   */
  public void setAttribute(String key, Object value) {
    attributes.put(key, value);
  }

  /**
   * Trả về toàn bộ map các thuộc tính, hữu ích cho việc serialization JSON.
   */
  @JsonAnyGetter
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * Lấy giá trị của khóa chính một cách tiện lợi.
   * @return Giá trị của khóa chính.
   */
  @JsonIgnore
  public Object getPrimaryKey() {
    if (this.primaryKey != null) {
      return this.primaryKey;
    }
    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    if (metadata != null) {
      this.primaryKey = this.getAttribute(metadata.getPrimaryKeyColumnName());
    }
    return this.primaryKey;
  }

  /**
   * Thiết lập giá trị cho khóa chính.
   * @param pkValue Giá trị khóa chính cần gán.
   */
  public void setPrimaryKey(Object pkValue) {
    this.primaryKey = pkValue;
    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    if (metadata != null) {
      this.setAttribute(metadata.getPrimaryKeyColumnName(), pkValue);
    }
  }

  /**
   * Tìm một trường trong một lớp hoặc các lớp cha của nó.
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
        "Field '" + fieldName + "' not found in class " + clazz.getName());
  }

  // --- CÁC HÀM HỖ TRỢ TIỆN ÍCH ---

  /**
   * Lưu thực thể hiện tại vào cơ sở dữ liệu.
   */
  public void save() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, new SaveOptions());
  }

  /**
   * Lưu thực thể hiện tại với các tùy chọn tùy chỉnh (ví dụ: cascade).
   */
  public void save(SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, options);
  }

  /**
   * Lưu một tập hợp các thực thể.
   */
  public static <T extends Model> void saveAll(Iterable<T> models) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, new SaveOptions());
  }

  /**
   * Lưu một tập hợp các thực thể với các tùy chọn tùy chỉnh.
   */
  public static <T extends Model> void saveAll(Iterable<T> models, SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, options);
  }

  /**
   * Xóa thực thể hiện tại khỏi cơ sở dữ liệu.
   */
  public void remove() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.remove(this);
  }
}
