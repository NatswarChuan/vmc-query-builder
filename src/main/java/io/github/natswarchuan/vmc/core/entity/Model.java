package io.github.natswarchuan.vmc.core.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.mapping.EntityMetadata;
import io.github.natswarchuan.vmc.core.mapping.MetadataCache;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;
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
 * <p>Lớp này đóng vai trò là nền tảng cho mọi đối tượng được ánh xạ tới cơ sở dữ liệu. Nó cung cấp
 * một tập hợp các thuộc tính và phương thức chung để quản lý dữ liệu, bao gồm:
 *
 * <ul>
 *   <li>Một {@code Map} để lưu trữ các thuộc tính thô được truy vấn từ cơ sở dữ liệu.
 *   <li>Các phương thức tiện ích để lưu (`save`), xóa (`remove`), và quản lý khóa chính.
 *   <li>Tích hợp với {@link VMCPersistenceManager} để thực hiện các thao tác bền bỉ (persistence).
 * </ul>
 *
 * <p>Mỗi lớp thực thể cụ thể trong ứng dụng nên kế thừa từ lớp này và sử dụng các annotation của
 * framework để định nghĩa ánh xạ tới bảng và các cột tương ứng trong cơ sở dữ liệu.
 *
 * @author NatswarChuan
 */
public abstract class Model {

  /**
   * Một Map để lưu trữ các thuộc tính của thực thể dưới dạng thô (tên cột -> giá trị).
   *
   * <p>Map này được sử dụng để chứa dữ liệu trả về trực tiếp từ cơ sở dữ liệu trước khi chúng được
   * gán cho các trường (field) cụ thể của lớp thực thể. Nó cũng hữu ích trong việc lưu trữ các giá
   * trị từ các cột được join mà không có trường tương ứng trong lớp entity.
   *
   * <p>Annotation {@code @JsonIgnore} ngăn không cho Map này bị serialize như một thuộc tính riêng
   * lẻ trong JSON output. Thay vào đó, các giá trị của nó sẽ được đưa lên cấp cao nhất của đối
   * tượng JSON thông qua phương thức {@link #getAttributes()}.
   */
  @JsonIgnore protected Map<String, Object> attributes = new HashMap<>();

  /**
   * Biến tạm thời để lưu trữ giá trị khóa chính sau khi đã được truy cập lần đầu.
   *
   * <p>Việc cache lại giá trị này giúp cải thiện hiệu năng bằng cách tránh việc phải tìm kiếm và
   * truy cập lại trường khóa chính bằng reflection nhiều lần. Biến này được đánh dấu là {@code
   * transient} để không bị serialize.
   */
  @JsonIgnore private transient Object primaryKey;

  /**
   * Gán một Map các thuộc tính cho thực thể và đồng bộ hóa các trường tương ứng.
   *
   * <p>Phương thức này nhận một Map (thường là kết quả từ một hàng trong cơ sở dữ liệu), lưu nó vào
   * biến {@link #attributes}, và sau đó duyệt qua Map để gán giá trị cho các trường (field) được
   * định nghĩa trong lớp thực thể. Quá trình này sử dụng {@link MetadataCache} để tìm ra ánh xạ
   * giữa tên cột và tên trường, và {@link DataConverter} để chuyển đổi kiểu dữ liệu nếu cần thiết.
   *
   * @param attributes Một Map chứa dữ liệu, với key là tên cột và value là giá trị tương ứng.
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
          // Bỏ qua lỗi một cách có chủ ý. Việc ghi log sẽ tốt hơn.
        }
      }
    }
  }

  /**
   * Lấy giá trị của một thuộc tính từ Map thuộc tính thô.
   *
   * @param key Tên của cột (key) trong Map thuộc tính.
   * @return Giá trị của thuộc tính, hoặc {@code null} nếu không tồn tại.
   */
  public Object getAttribute(String key) {
    return this.attributes.get(key);
  }

  /**
   * Gán giá trị cho một thuộc tính trong Map thuộc tính thô.
   *
   * @param key Tên của cột (key) để gán.
   * @param value Giá trị cần gán.
   */
  public void setAttribute(String key, Object value) {
    attributes.put(key, value);
  }

  /**
   * Lấy tất cả các thuộc tính thô.
   *
   * <p>Phương thức này được chú thích bằng {@code @JsonAnyGetter}, cho phép Jackson serialize tất
   * cả các entry trong Map {@link #attributes} thành các thuộc tính ở cấp cao nhất của đối tượng
   * JSON. Điều này rất hữu ích khi trả về các kết quả từ các truy vấn có các cột được tính toán
   * hoặc join động.
   *
   * @return Map chứa các thuộc tính của thực thể.
   */
  @JsonAnyGetter
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * Lấy giá trị của khóa chính cho thực thể này.
   *
   * <p>Phương thức này hoạt động theo cơ chế lazy-loading và caching. Lần đầu tiên được gọi, nó sẽ
   * sử dụng reflection để tìm trường được chú thích bằng {@code @VMCPrimaryKey}, lấy giá trị của
   * nó, cache lại và trả về. Các lần gọi tiếp theo sẽ trả về giá trị đã được cache. Nếu việc truy
   * cập trường thất bại, nó sẽ thử lấy giá trị từ Map thuộc tính như một phương án dự phòng.
   *
   * @return Giá trị của khóa chính.
   */
  @JsonIgnore
  public Object getPrimaryKey() {
    if (this.primaryKey != null) {
      return this.primaryKey;
    }
    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    if (metadata != null) {
      try {
        Field pkField = findField(this.getClass(), metadata.getPrimaryKeyFieldName());
        pkField.setAccessible(true);
        this.primaryKey = pkField.get(this);
      } catch (Exception e) {
        // Dự phòng lấy từ map attributes nếu truy cập trường thất bại
        this.primaryKey = this.getAttribute(metadata.getPrimaryKeyColumnName());
      }
    }
    return this.primaryKey;
  }

  /**
   * Thiết lập giá trị cho khóa chính.
   *
   * <p>Phương thức này cập nhật đồng bộ giá trị khóa chính ở ba nơi: biến cache, Map thuộc tính
   * thô, và trường (field) khóa chính thực sự trong đối tượng. Điều này đảm bảo tính nhất quán của
   * trạng thái đối tượng.
   *
   * @param pkValue Giá trị khóa chính cần gán.
   * @throws VMCException nếu không thể tìm thấy hoặc gán giá trị cho trường khóa chính.
   */
  public void setPrimaryKey(Object pkValue) {
    this.primaryKey = pkValue;
    EntityMetadata metadata = MetadataCache.getMetadata(this.getClass());
    if (metadata != null) {
      this.setAttribute(metadata.getPrimaryKeyColumnName(), pkValue);

      try {
        Field pkField = findField(this.getClass(), metadata.getPrimaryKeyFieldName());
        pkField.setAccessible(true);
        Object convertedValue = DataConverter.convertValue(pkValue, pkField.getType());
        pkField.set(this, convertedValue);
      } catch (Exception e) {
        throw new VMCException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to set primary key field '"
                + metadata.getPrimaryKeyFieldName()
                + "' on entity "
                + this.getClass().getSimpleName(),
            e);
      }
    }
  }

  /**
   * Tìm một trường (field) trong một lớp hoặc các lớp cha của nó.
   *
   * <p>Phương thức này duyệt ngược lên cây kế thừa của lớp để tìm một trường theo tên. Điều này cần
   * thiết vì các trường có thể được định nghĩa trong các lớp cha.
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

  /**
   * Lưu thực thể hiện tại vào cơ sở dữ liệu với các tùy chọn mặc định.
   *
   * <p>Đây là một phương thức tiện ích, nó ủy quyền việc lưu cho {@link VMCPersistenceManager}.
   */
  public void save() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, new SaveOptions());
  }

  /**
   * Lưu thực thể hiện tại vào cơ sở dữ liệu với các tùy chọn tùy chỉnh.
   *
   * @param options Các tùy chọn để kiểm soát hành vi lưu, ví dụ như cascade.
   * @see SaveOptions
   */
  public void save(SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.save(this, options);
  }

  /**
   * Lưu một tập hợp các thực thể với các tùy chọn mặc định.
   *
   * @param models Một {@link Iterable} chứa các thực thể cần lưu.
   * @param <T> Kiểu của thực thể, phải kế thừa từ {@code Model}.
   */
  public static <T extends Model> void saveAll(Iterable<T> models) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, new SaveOptions());
  }

  /**
   * Lưu một tập hợp các thực thể với các tùy chọn tùy chỉnh.
   *
   * @param models Một {@link Iterable} chứa các thực thể cần lưu.
   * @param options Các tùy chọn để kiểm soát hành vi lưu.
   * @param <T> Kiểu của thực thể.
   */
  public static <T extends Model> void saveAll(Iterable<T> models, SaveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.saveAll(models, options);
  }

  /**
   * Xóa thực thể hiện tại khỏi cơ sở dữ liệu.
   *
   * <p>Phương thức này ủy quyền việc xóa cho {@link VMCPersistenceManager}.
   */
  public void remove() {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.remove(this);
  }

  /**
   * Xóa thực thể hiện tại khỏi cơ sở dữ liệu với các tùy chọn tùy chỉnh.
   *
   * @param options Các tùy chọn để kiểm soát hành vi xóa.
   * @see RemoveOptions
   */
  public void remove(RemoveOptions options) {
    VMCPersistenceManager manager = BeanUtil.getBean(VMCPersistenceManager.class);
    manager.remove(this, options);
  }
}
