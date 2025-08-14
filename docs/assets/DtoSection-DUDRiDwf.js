import{_ as s}from"./CodeBlock-BK92jf-v.js";import{r as n,c,o as p,b as e,d as o,e as r}from"./index-DdSB5D2R.js";const u={id:"dto",class:"mb-12 space-y-4 scroll-mt-20"},d={id:"dto-mechanism",class:"scroll-mt-20 pt-4"},h={class:"space-y-4"},D={id:"dto-usage",class:"scroll-mt-20 pt-4"},T={__name:"DtoSection",setup(v){const i=n(`
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
`),l=n(`
// Một vài phương thức nạp chồng có sẵn trong VMCService.java

// Tạo entity từ một DTO đầu vào và trả về một DTO đầu ra khác
<I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(I requestDto, Class<O> responseDtoClass, SaveOptions options);

// Cập nhật entity từ một DTO đầu vào và trả về một DTO đầu ra khác
<I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(ID id, I requestDto, Class<O> responseDtoClass, SaveOptions options);

// Tìm entity bằng ID và tự động chuyển thành DTO
<D extends BaseDto<T, D>> Optional<D> findByIdGetDto(ID id, Class<D> dtoClass);
`),a=n(`
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
`);return(m,t)=>(p(),c("section",u,[t[7]||(t[7]=e("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"9. Làm việc với DTO",-1)),t[8]||(t[8]=e("p",null,"Framework cung cấp các tiện ích để làm việc với Data Transfer Objects (DTOs), giúp tách biệt lớp business logic với lớp persistence.",-1)),e("article",d,[t[2]||(t[2]=e("h3",{class:"text-2xl font-semibold text-slate-800"},"9.1. Cơ chế Chuyển đổi",-1)),e("div",h,[t[0]||(t[0]=e("p",null,"Cơ chế chuyển đổi DTO của framework dựa trên sự tương tác giữa 3 thành phần:",-1)),t[1]||(t[1]=e("ul",{class:"list-decimal list-inside space-y-2 text-slate-600"},[e("li",null,[e("strong",null,"Interface `BaseDto<E, D>`"),r(": Đây là interface mà tất cả các DTO của bạn phải triển khai. Nó định nghĩa các phương thức `toEntity()` và `toDto(Model entity)`, buộc bạn phải cung cấp logic chuyển đổi.")]),e("li",null,[e("strong",null,"Lớp DTO của bạn"),r(": Bạn implement logic chuyển đổi cụ thể trong các phương thức `toEntity()` và `toDto()` mà bạn override.")]),e("li",null,[e("strong",null,"Lớp `AbstractVMCService`"),r(": Lớp service này cung cấp các phương thức nghiệp vụ cấp cao (`createFromDto`, `updateFromDto`, etc.) đóng vai trò điều phối, gọi đến các phương thức `toEntity()`/`toDto()` trên đối tượng DTO để hoàn thành công việc.")])],-1)),o(s,{language:"java",code:i.value},null,8,["code"])])]),e("article",D,[t[3]||(t[3]=e("h3",{class:"text-2xl font-semibold text-slate-800"},"9.2. Các hàm Service linh hoạt với DTO",-1)),t[4]||(t[4]=e("p",null,"`AbstractVMCService` cung cấp sẵn các phương thức được nạp chồng (overloaded) để làm việc trực tiếp với DTO. Đặc biệt, nó cho phép bạn nhận một loại DTO làm đầu vào (ví dụ: `RequestDto`) và trả về một loại DTO khác làm đầu ra (ví dụ: `ResponseDto`).",-1)),o(s,{language:"java",code:l.value},null,8,["code"]),t[5]||(t[5]=e("h4",{class:"text-xl font-semibold text-slate-700 mt-4"},"Ví dụ thực tế trong Controller",-1)),t[6]||(t[6]=e("p",null,"Với các phương thức trên, code trong Controller của bạn sẽ rất tinh gọn và rõ ràng, tách biệt hoàn toàn giữa dữ liệu request và response.",-1)),o(s,{language:"java",code:a.value},null,8,["code"])])]))}};export{T as default};
