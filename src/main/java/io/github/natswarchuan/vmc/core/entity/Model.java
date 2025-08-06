package io.github.natswarchuan.vmc.core.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.natswarchuan.vmc.core.exception.VMCException;
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
import org.springframework.http.HttpStatus;

/**
 * Lớp trừu tượng cơ sở cho tất cả các thực thể (entity) trong framework.
 *
 * <p>Lớp này cung cấp một cơ chế lưu trữ thuộc tính linh hoạt thông qua một {@code Map} và các
 * phương thức tiện ích để tương tác với tầng persistence. Mọi lớp entity muốn được quản lý bởi
 * framework này đều phải kế thừa từ {@code Model}.
 *
 * @author NatswarChuan
 */
public abstract class Model {

  /**
   * Một {@code Map} để lưu trữ dữ liệu thô của thực thể, thường là ánh xạ từ tên cột trong cơ sở dữ
   * liệu sang giá trị. Thuộc tính này được ẩn khỏi quá trình serialization JSON.
   */
  @JsonIgnore protected Map<String, Object> attributes = new HashMap<>();

  /**
   * Lưu trữ giá trị của khóa chính để truy cập nhanh. Được đánh dấu là {@code transient} để không
   * được serialize và {@code JsonIgnore} để Jackson bỏ qua.
   */
  @JsonIgnore private transient Object primaryKey;

  /**
   * Gán một {@code Map} các thuộc tính cho thực thể.
   *
   * <p>Phương thức này không chỉ thay thế map thuộc tính hiện tại mà còn tự động điền dữ liệu vào
   * các trường (field) tương ứng của lớp con bằng cách sử dụng reflection và metadata. Điều này cho
   * phép ánh xạ dữ liệu từ kết quả truy vấn cơ sở dữ liệu vào đối tượng một cách linh hoạt.
   *
   * @param attributes Một {@code Map} chứa dữ liệu, với key là tên cột và value là giá trị tương
   *     ứng.
   */
  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;

    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    Map<String, String> columnToFieldMap =
        metadata.getFieldToColumnMap().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String columnName = entry.getKey();
      String fieldName = columnToFieldMap.get(columnName);

      if (fieldName != null) {
        try {
          Field field = findField(this.getClass(), fieldName);
          field.setAccessible(true);

          Object convertedValue = DataConverter.convertValue(entry.getValue(), field.getType());
          field.set(this, convertedValue);
        } catch (Exception e) {

        }
      }
    }
  }

  /**
   * Lấy giá trị của một thuộc tính cụ thể từ map {@code attributes}.
   *
   * @param key Tên của thuộc tính (thường là tên cột trong cơ sở dữ liệu).
   * @return Giá trị của thuộc tính, hoặc {@code null} nếu không tồn tại.
   */
  public Object getAttribute(String key) {
    return this.attributes.get(key);
  }

  /**
   * Gán giá trị cho một thuộc tính cụ thể trong map {@code attributes}.
   *
   * @param key Tên của thuộc tính.
   * @param value Giá trị cần gán.
   */
  public void setAttribute(String key, Object value) {
    attributes.put(key, value);
  }

  /**
   * Trả về toàn bộ map các thuộc tính.
   *
   * <p>Phương thức này được chú thích bằng {@code @JsonAnyGetter}, cho phép Jackson serialize tất
   * cả các entry trong map này thành các thuộc tính JSON cấp cao nhất.
   *
   * @return Toàn bộ map các thuộc tính của thực thể.
   */
  @JsonAnyGetter
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * Lấy giá trị của khóa chính một cách tiện lợi.
   *
   * <p>Phương thức này sẽ ưu tiên trả về giá trị đã được cache. Nếu chưa có, nó sẽ truy vấn
   * metadata để tìm tên cột khóa chính và lấy giá trị từ map {@code attributes}.
   *
   * @return Giá trị của khóa chính, hoặc {@code null} nếu không tìm thấy.
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
   *
   * <p>Phương thức này cập nhật cả biến cache {@code primaryKey} và giá trị tương ứng trong map
   * {@code attributes}.
   *
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
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó bằng reflection.
   *
   * @param clazz Lớp bắt đầu tìm kiếm.
   * @param fieldName Tên của trường cần tìm.
   * @return Đối tượng {@code Field} nếu tìm thấy.
   * @throws VMCException nếu không tìm thấy trường trong toàn bộ hệ thống phân cấp lớp.
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

  /** Lưu thực thể hiện tại vào cơ sở dữ liệu bằng cách sử dụng các tùy chọn mặc định. */
  public void save() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, new SaveOptions());
  }

  /**
   * Lưu thực thể hiện tại vào cơ sở dữ liệu với các tùy chọn tùy chỉnh.
   *
   * @param options Các tùy chọn để kiểm soát hành vi lưu, ví dụ như cascade.
   */
  public void save(SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, options);
  }

  /**
   * Lưu một tập hợp các thực thể vào cơ sở dữ liệu.
   *
   * @param <T> Kiểu của các thực thể, phải kế thừa từ {@code Model}.
   * @param models Một {@code Iterable} chứa các thực thể cần lưu.
   */
  public static <T extends Model> void saveAll(Iterable<T> models) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, new SaveOptions());
  }

  /**
   * Lưu một tập hợp các thực thể với các tùy chọn tùy chỉnh.
   *
   * @param <T> Kiểu của các thực thể.
   * @param models Một {@code Iterable} chứa các thực thể cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   */
  public static <T extends Model> void saveAll(Iterable<T> models, SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, options);
  }

  /** Xóa thực thể hiện tại khỏi cơ sở dữ liệu. */
  public void remove() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.remove(this);
  }
}
