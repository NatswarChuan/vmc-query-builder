package io.github.natswarchuan.vmc.core.util;

import java.lang.reflect.Field;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

import io.github.natswarchuan.vmc.core.exception.VMCException;

/**
 * Cung cấp các phương thức tiện ích để chuyển đổi kiểu dữ liệu.
 *
 * <p>Lớp này chứa logic để xử lý sự không tương thích về kiểu dữ liệu giữa các giá trị trả về từ
 * JDBC (ví dụ: {@code Timestamp}) và các kiểu dữ liệu hiện đại trong Java (ví dụ: {@code
 * LocalDateTime}).
 *
 * @author NatswarChuan
 */
public class DataConverter {

  /**
   * Chuyển đổi một giá trị sang một kiểu đích cụ thể.
   *
   * <p>Phương thức này xử lý các trường hợp chuyển đổi phổ biến như từ các kiểu số (Number) sang
   * các kiểu nguyên thủy khác, hoặc từ các kiểu ngày giờ của {@code java.sql} sang các kiểu của
   * {@code java.time}.
   *
   * @param value Giá trị gốc cần chuyển đổi.
   * @param targetType Lớp của kiểu dữ liệu đích.
   * @return Đối tượng đã được chuyển đổi, hoặc giá trị gốc nếu không cần chuyển đổi.
   */
  public static Object convertValue(Object value, Class<?> targetType) {
    if (value == null) {
      return null;
    }
    if (targetType.isAssignableFrom(value.getClass())) {
      return value;
    }
    if (value instanceof Number) {
      Number numberValue = (Number) value;
      if (targetType == Long.class || targetType == long.class) return numberValue.longValue();
      if (targetType == Integer.class || targetType == int.class) return numberValue.intValue();
      if (targetType == Double.class || targetType == double.class)
        return numberValue.doubleValue();
      if (targetType == Float.class || targetType == float.class) return numberValue.floatValue();
      if (targetType == Short.class || targetType == short.class) return numberValue.shortValue();
      if (targetType == Byte.class || targetType == byte.class) return numberValue.byteValue();
    }
    if (value instanceof Timestamp && targetType == LocalDateTime.class) {
      return ((Timestamp) value).toLocalDateTime();
    }
    if (value instanceof java.sql.Date && targetType == LocalDate.class) {
      return ((java.sql.Date) value).toLocalDate();
    }
    if (value instanceof Time && targetType == LocalTime.class) {
      return ((Time) value).toLocalTime();
    }
    return value;
  }

  /**
   * Ánh xạ một hàng dữ liệu từ một {@link Map} sang một đối tượng Java (POJO).
   *
   * <p>Phương thức này sử dụng reflection để duyệt qua các trường của lớp đích, tìm các giá trị
   * tương ứng trong Map (hỗ trợ cả camelCase và snake_case), chuyển đổi kiểu dữ liệu nếu cần, và
   * gán giá trị cho các trường của đối tượng.
   *
   * @param row Một Map đại diện cho một hàng dữ liệu, với key là tên cột.
   * @param targetClass Lớp của đối tượng đích cần tạo và điền dữ liệu.
   * @param <T> Kiểu của đối tượng đích.
   * @return Một instance mới của lớp đích với các trường đã được điền dữ liệu.
   * @throws VMCException nếu có lỗi xảy ra trong quá trình reflection hoặc khởi tạo.
   */
  public static <T> T mapRowToObject(Map<String, Object> row, Class<T> targetClass) {
    try {
      T instance = targetClass.getDeclaredConstructor().newInstance();
      for (Field field : targetClass.getDeclaredFields()) {
        String fieldName = field.getName();
        String snakeCaseFieldName = toSnakeCase(fieldName);

        Object value = row.get(fieldName);
        if (value == null) {
          value = row.get(snakeCaseFieldName);
        }
        if (value == null) {
          for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(fieldName)
                || entry.getKey().equalsIgnoreCase(snakeCaseFieldName)) {
              value = entry.getValue();
              break;
            }
          }
        }

        if (value != null) {
          field.setAccessible(true);
          Object convertedValue = convertValue(value, field.getType());
          field.set(instance, convertedValue);
        }
      }
      return instance;
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.BAD_REQUEST,
          "Không thể ánh xạ kết quả sang đối tượng: " + targetClass.getSimpleName(),
          e);
    }
  }

  /**
   * Chuyển đổi một chuỗi từ dạng camelCase sang snake_case.
   *
   * <p>Ví dụ: "userName" -> "user_name".
   *
   * @param camelCase Chuỗi đầu vào dạng camelCase.
   * @return Chuỗi đã được chuyển đổi sang snake_case.
   */
  private static String toSnakeCase(String camelCase) {
    if (camelCase == null) return null;
    return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
  }
}
