import{_ as s}from"./CodeBlock-BI-7EVGF.js";import{r as c,c as o,o as i,b as t,d as a}from"./index-BVd1TkhY.js";const n={id:"service-layer",class:"mb-12 space-y-4 scroll-mt-20"},m={__name:"ServiceLayerSection",setup(p){const r=c(`
// Trong UserService.java
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.service.AbstractVMCService;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbstractVMCService<User, String, UserRepository> implements IUserService {

    @Override
    public User create(UserCreateRequestDto requestDto) {
        User user = requestDto.toEntity(); // Gọi hàm toEntity từ DTO
        // Giả sử DTO có chứa danh sách các Role
        // Để lưu cả User và các Role liên quan, ta phải dùng SaveOptions
        SaveOptions options = new SaveOptions().withRelations("roles");
        return this.save(user, options);
    }
    // ... các phương thức khác
}
`);return(l,e)=>(i(),o("section",n,[e[0]||(e[0]=t("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"8. Lớp Service",-1)),e[1]||(e[1]=t("p",null,"Framework cung cấp một lớp `AbstractVMCService` để đơn giản hóa việc tạo các lớp service. Lớp này đã có sẵn các phương thức CRUD cơ bản và quan trọng là phương thức `save(T entity, SaveOptions options)` để thực hiện lưu theo tầng (cascade save).",-1)),a(s,{language:"java",code:r.value},null,8,["code"])]))}};export{m as default};
