package io.github.natswarchuan.vmc.core.query.builder;

import java.util.List;
import lombok.Getter;

/**
 * Một lớp chứa dữ liệu phân trang cho các kết quả truy vấn.
 *
 * <p>Lớp này đóng gói không chỉ dữ liệu của trang hiện tại mà còn cả các thông tin phân trang cần
 * thiết khác như tổng số bản ghi, số trang, v.v., để dễ dàng hiển thị trên giao diện người dùng.
 *
 * @param <T> Kiểu của dữ liệu trong danh sách phân trang.
 * @author NatswarChuan
 */
@Getter
public class Paginator<T> {

  /** Danh sách các mục (item) trên trang hiện tại. */
  private final List<T> data;

  /** Tổng số mục trên tất cả các trang. */
  private final long total;

  /** Số lượng mục trên mỗi trang. */
  private final int perPage;

  /** Số của trang hiện tại (bắt đầu từ 1). */
  private final int currentPage;

  /** Số của trang cuối cùng. */
  private final int lastPage;

  /** Chỉ số của mục đầu tiên trên trang hiện tại (tính từ 1). */
  private final int from;

  /** Chỉ số của mục cuối cùng trên trang hiện tại (tính từ 1). */
  private final int to;

  /**
   * Khởi tạo một đối tượng Paginator mới.
   *
   * @param data Danh sách dữ liệu cho trang hiện tại.
   * @param total Tổng số bản ghi.
   * @param perPage Số bản ghi trên mỗi trang.
   * @param currentPage Số của trang hiện tại.
   */
  public Paginator(List<T> data, long total, int perPage, int currentPage) {
    this.data = data;
    this.total = total;
    this.perPage = perPage > 0 ? perPage : 1;
    this.currentPage = currentPage;
    this.lastPage = (int) Math.ceil((double) this.total / this.perPage);

    if (this.total > 0) {
      this.from = (this.currentPage - 1) * this.perPage + 1;
      this.to = this.from + data.size() - 1;
    } else {
      this.from = 0;
      this.to = 0;
    }
  }
}
