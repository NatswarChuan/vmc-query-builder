package io.github.natswarchuan.vmc.core.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

/**
 * Một interface Mapper của MyBatis để thực thi các câu lệnh SQL động một cách chung chung.
 *
 * <p>Interface này là cầu nối cấp thấp giữa framework VMC và cơ sở dữ liệu. Nó cho phép thực thi
 * các chuỗi SQL được xây dựng tại thời điểm chạy, thay vì các câu lệnh tĩnh được định nghĩa trong
 * file XML.
 *
 * <p>Lưu ý: Việc sử dụng {@code ${sql}} có thể tiềm ẩn nguy cơ SQL Injection nếu không được xử lý
 * cẩn thận ở tầng cao hơn. Framework này đảm bảo rằng các giá trị tham số được truyền một cách an
 * toàn thông qua map {@code params}.
 *
 * @author NatswarChuan
 */
@Mapper
public interface GenericQueryExecutorMapper {

  /**
   * Thực thi một câu lệnh SELECT SQL động.
   *
   * @param sql Chuỗi câu lệnh SQL để thực thi.
   * @param params Một Map chứa các tham số cho câu lệnh truy vấn.
   * @return Một danh sách các Map, trong đó mỗi Map đại diện cho một hàng kết quả.
   */
  @Select("${sql}")
  List<Map<String, Object>> execute(
      @Param("sql") String sql, @Param("params") Map<String, Object> params);

  /**
   * Thực thi một câu lệnh INSERT SQL động và trả về khóa tự tăng.
   *
   * <p>Annotation {@code @SelectKey} được sử dụng để lấy ID được tạo tự động (ví dụ:
   * auto-increment) sau khi chèn và đặt nó vào thuộc tính 'id' của map {@code params}.
   *
   * @param sql Chuỗi câu lệnh SQL INSERT để thực thi.
   * @param params Một Map chứa các tham số cho câu lệnh.
   * @return Số hàng đã bị ảnh hưởng.
   */
  @Insert("${sql}")
  @SelectKey(
      statement = "SELECT LAST_INSERT_ID()",
      keyProperty = "params.id",
      before = false,
      resultType = Long.class)
  int insert(@Param("sql") String sql, @Param("params") Map<String, Object> params);

  /**
   * Thực thi một câu lệnh UPDATE SQL động.
   *
   * @param sql Chuỗi câu lệnh SQL UPDATE để thực thi.
   * @param params Một Map chứa các tham số cho câu lệnh.
   * @return Số hàng đã bị ảnh hưởng.
   */
  @Update("${sql}")
  int update(@Param("sql") String sql, @Param("params") Map<String, Object> params);

  /**
   * Thực thi một câu lệnh DELETE SQL động.
   *
   * @param sql Chuỗi câu lệnh SQL DELETE để thực thi.
   * @param params Một Map chứa các tham số cho câu lệnh.
   * @return Số hàng đã bị ảnh hưởng.
   */
  @Delete("${sql}")
  int delete(@Param("sql") String sql, @Param("params") Map<String, Object> params);
}
