package io.github.natswarchuan.vmc.core.exception;

import io.github.natswarchuan.vmc.core.dto.ErrorResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bộ xử lý ngoại lệ toàn cục cho ứng dụng.
 *
 * <p>Lớp này sử dụng {@link RestControllerAdvice} để bắt và xử lý các ngoại lệ được ném ra từ bất
 * kỳ controller nào trong ứng dụng. Nó đảm bảo rằng các lỗi sẽ được chuyển thành một định dạng JSON
 * nhất quán trước khi gửi về cho client.
 *
 * @author NatswarChuan
 */
@RestControllerAdvice
@Slf4j
public class VMCExceptionHandler {

  /**
   * Xử lý các ngoại lệ kiểu {@link VMCException} tùy chỉnh của ứng dụng.
   *
   * @param ex Ngoại lệ {@code VMCException} đã được ném ra.
   * @return Một {@code ResponseEntity} chứa thông tin lỗi và mã trạng thái HTTP tương ứng.
   */
  @ExceptionHandler(VMCException.class)
  public ResponseEntity<ErrorResponse> handleVMCException(VMCException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(ex.getStatus().value(), ex.getMessage(), System.currentTimeMillis());
    log.error("VMCException: {}", ex.getMessage(), ex);
    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  /**
   * Xử lý các ngoại lệ validation từ annotation @Valid.
   *
   * <p>Phương thức này được tự động gọi khi validation của một đối tượng request body thất bại.
   *
   * @param ex Ngoại lệ {@code MethodArgumentNotValidException} chứa chi tiết các lỗi.
   * @return Một {@code ResponseEntity} chứa cấu trúc lỗi validation chi tiết.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", System.currentTimeMillis());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation Failed");

    List<String> errors =
        ex.getBindingResult().getAllErrors().stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.toList());

    body.put("messages", errors);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
}
