<template>
  <section id="dto" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">9. Làm việc với DTO</h2>
    <p>Framework cung cấp các tiện ích để làm việc với Data Transfer Objects (DTOs), giúp tách biệt lớp business
      logic với lớp persistence.</p>

    <article id="dto-mechanism" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">9.1. Cơ chế Chuyển đổi</h3>
      <div class="space-y-4">
        <p>Cơ chế chuyển đổi DTO của framework dựa trên sự tương tác giữa 3 thành phần:</p>
        <ul class="list-decimal list-inside space-y-2 text-slate-600">
          <li><strong>Interface `BaseDto&lt;E, D&gt;`</strong>: Đây là interface mà tất cả các DTO của bạn phải
            triển khai. Nó định nghĩa các phương thức `toEntity()` và `toDto(Model entity)`, buộc bạn phải cung
            cấp logic chuyển đổi.</li>
          <li><strong>Lớp DTO của bạn</strong>: Bạn implement logic chuyển đổi cụ thể trong các phương thức
            `toEntity()` và `toDto()` mà bạn override.</li>
          <li><strong>Lớp `AbstractVMCService`</strong>: Lớp service này cung cấp các phương thức nghiệp vụ cấp
            cao (`createFromDto`, `updateFromDto`, etc.) đóng vai trò điều phối, gọi đến các phương thức
            `toEntity()`/`toDto()` trên đối tượng DTO để hoàn thành công việc.</li>
        </ul>
        <CodeBlock language="java" :code="dtoConversionExample" />
      </div>
    </article>

    <article id="dto-usage" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">9.2. Các hàm Service linh hoạt với DTO</h3>
      <p>`AbstractVMCService` cung cấp sẵn các phương thức được nạp chồng (overloaded) để làm việc trực tiếp với
        DTO. Đặc biệt, nó cho phép bạn nhận một loại DTO làm đầu vào (ví dụ: `RequestDto`) và trả về một loại DTO
        khác làm đầu ra (ví dụ: `ResponseDto`).</p>
      <CodeBlock language="java" :code="dtoServiceFunctions" />
      <h4 class="text-xl font-semibold text-slate-700 mt-4">Ví dụ thực tế trong Controller</h4>
      <p>Với các phương thức trên, code trong Controller của bạn sẽ rất tinh gọn và rõ ràng, tách biệt hoàn toàn
        giữa dữ liệu request và response.</p>
      <CodeBlock language="java" :code="dtoControllerExample" />
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const dtoConversionExample = ref(`
// Trong UserCreateRequestDto.java
@Override
public User toEntity() {
    User user = new User();
    // Sao chép các thuộc tính từ DTO sang Entity
    user.setName(this.name);
    user.setEmail(this.email);
    user.setPassword(this.password); // (Lưu ý: nên mã hóa mật khẩu ở đây)
    return user;
}

// Trong UserResponseDto.java
@Override
public UserResponseDto toDto(Model entity) {
    if (entity instanceof User) {
        User user = (User) entity;
        // Sao chép các thuộc tính từ Entity sang DTO
        this.setId(user.getId());
        this.setName(user.getName());
        this.setEmail(user.getEmail());
    }
    return this;
}
`);

const dtoServiceFunctions = ref(`
// Một vài phương thức nạp chồng có sẵn trong VMCService.java

// Tạo entity từ một DTO đầu vào và trả về một DTO đầu ra khác
<I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(I requestDto, Class<O> responseDtoClass, SaveOptions options);

// Cập nhật entity từ một DTO đầu vào và trả về một DTO đầu ra khác
<I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(ID id, I requestDto, Class<O> responseDtoClass, SaveOptions options);

// Tìm entity bằng ID và tự động chuyển thành DTO
<D extends BaseDto<T, D>> Optional<D> findByIdGetDto(ID id, Class<D> dtoClass);
`);

const dtoControllerExample = ref(`
// Trong UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final IUserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findByIdGetDto(id, UserResponseDto.class)
            .orElseThrow(() -> new VMCException(HttpStatus.NOT_FOUND, "User not found")));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateRequestDto requestDto) {
        // Gọi hàm createFromDto mới, chỉ định DTO trả về là UserResponseDto
        SaveOptions options = new SaveOptions().with("profile").with("roles");
        UserResponseDto responseDto = userService.createFromDto(requestDto, UserResponseDto.class, options);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}
`);
</script>
