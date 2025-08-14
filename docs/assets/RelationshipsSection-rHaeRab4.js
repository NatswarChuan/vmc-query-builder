import{_ as o}from"./CodeBlock-BI-7EVGF.js";import{r as s,c as d,o as h,b as n,d as l,e as t,a as m}from"./index-BVd1TkhY.js";const p={id:"anh-xa-quan-he",class:"mb-12 space-y-4 scroll-mt-20"},v={id:"onetoone",class:"scroll-mt-20 pt-4"},g={id:"onetomany",class:"scroll-mt-20 pt-4"},y={id:"manytomany",class:"scroll-mt-20 pt-4"},M={id:"lazy-loading",class:"scroll-mt-20 pt-4"},x={class:"space-y-6"},f={__name:"RelationshipsSection",setup(C){const a=s(`
// Trong User.java
@VMCOneToOne(mappedBy = "user")
private UserProfile profile;

// Trong UserProfile.java
@VMCOneToOne
@VMCJoinColumn(name = "user_id")
private User user;
`),i=s(`
// Trong User.java
@VMCOneToMany(mappedBy = "user")
private List<Post> posts;

// Trong Post.java
@VMCManyToOne
@VMCJoinColumn(name = "user_id")
private User user;
`),r=s(`
// Trong User.java
@VMCManyToMany
@VMCJoinTable(
    name = "user_roles",
    joinColumns = @VMCJoinColumn(name = "user_id"),
    inverseJoinColumns = @VMCJoinColumn(name = "role_id"))
private Set<Role> roles;

// Trong Role.java
@VMCManyToMany(mappedBy = "roles")
private Set<User> users;
`),c=s(`
// Ví dụ trong UserService
public User createUserAndProfile(UserCreateDto dto) {
    User user = dto.toEntity(); // user có chứa một đối tượng UserProfile mới
    
    // Chỉ định rằng chúng ta muốn lưu cả quan hệ 'profile'
    SaveOptions options = new SaveOptions().with("profile");
    
    // Lưu user, và UserProfile liên quan cũng sẽ được lưu tự động
    return this.save(user, options); 
}
`),u=s(`
// ... trong UserService
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;

// Ví dụ: Xóa cứng User và tất cả các Post, Profile liên quan
public void deleteUserAndAssociations(Long userId) {
    RemoveOptions options = new RemoveOptions()
                                  .with("posts")    // Xóa các Post liên quan
                                  .with("profile"); // Xóa UserProfile liên quan
                                  
    repository.removeById(userId, options);
}
`);return(b,e)=>(h(),d("section",p,[e[11]||(e[11]=n("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"4. Ánh xạ Quan hệ",-1)),e[12]||(e[12]=n("p",null,"Framework hỗ trợ đầy đủ các mối quan hệ phổ biến trong ORM.",-1)),n("article",v,[e[0]||(e[0]=n("h3",{class:"text-2xl font-semibold text-slate-800"},"4.1. One-to-One",-1)),e[1]||(e[1]=n("p",null,[t("Mỗi `User` có một `UserProfile`. Quan hệ này được định nghĩa bằng "),n("code",null,"@VMCOneToOne"),t(" và "),n("code",null,"@VMCJoinColumn"),t(". ")],-1)),l(o,{language:"java",code:a.value},null,8,["code"])]),n("article",g,[e[2]||(e[2]=n("h3",{class:"text-2xl font-semibold text-slate-800"},"4.2. One-to-Many / Many-to-One",-1)),e[3]||(e[3]=n("p",null,[t("Một `User` có thể viết nhiều `Post`. Quan hệ này được thể hiện bằng "),n("code",null,"@VMCOneToMany"),t(" và "),n("code",null,"@VMCManyToOne"),t(". ")],-1)),l(o,{language:"java",code:i.value},null,8,["code"])]),n("article",y,[e[4]||(e[4]=n("h3",{class:"text-2xl font-semibold text-slate-800"},"4.3. Many-to-Many",-1)),e[5]||(e[5]=n("p",null,[t("Một `User` có thể có nhiều `Role`. Quan hệ này cần bảng trung gian `user_roles` và được định nghĩa bằng "),n("code",null,"@VMCManyToMany"),t(" và "),n("code",null,"@VMCJoinTable"),t(". ")],-1)),l(o,{language:"java",code:r.value},null,8,["code"])]),n("article",M,[e[10]||(e[10]=n("h3",{class:"text-2xl font-semibold text-slate-800"},"4.4. Lazy Loading & Tùy chọn Thao tác",-1)),n("div",x,[e[9]||(e[9]=n("div",null,[n("h4",{class:"text-xl font-semibold text-slate-700"},"Lazy Loading (Tải lười)"),n("p",{class:"mt-2 text-slate-600"},[t("Đây là hành vi mặc định cho các mối quan hệ tập hợp ("),n("code",null,"One-to-Many"),t(", "),n("code",null,"Many-to-Many"),t(") để tối ưu hiệu suất. Thay vì tải tất cả các thực thể liên quan cùng một lúc, framework sẽ chỉ thực hiện truy vấn để lấy dữ liệu của collection khi bạn truy cập vào nó lần đầu tiên (ví dụ: gọi "),n("code",null,"user.getPosts()"),t(").")])],-1)),n("div",null,[e[6]||(e[6]=n("h4",{class:"text-xl font-semibold text-slate-700"},"Tùy chọn Lưu (SaveOptions)",-1)),e[7]||(e[7]=n("p",{class:"mt-2 text-slate-600"},[t("Cho phép bạn lưu một thực thể cha và tự động lưu tất cả các thực thể con liên quan trong cùng một thao tác ("),n("strong",null,"Cascade Save"),t("). "),n("strong",null,"Quan trọng:"),t(" Tính năng này không được bật mặc định. Để kích hoạt, bạn phải tạo một đối tượng "),n("code",null,"SaveOptions"),t(", sử dụng phương thức "),n("code",null,'.with("ten_quan_he")'),t(" để chỉ định các mối quan hệ cần lưu, và truyền nó vào phương thức "),n("code",null,"save()"),t(". ")],-1)),l(o,{language:"java",code:c.value},null,8,["code"])]),n("div",null,[e[8]||(e[8]=m('<h4 class="text-xl font-semibold text-slate-700">Tùy chọn Xóa (RemoveOptions)</h4><p class="mt-2 text-slate-600">Tương tự như `SaveOptions`, `RemoveOptions` cung cấp một cách tường minh để kiểm soát hành vi <strong>xóa theo tầng (Cascade Remove)</strong>. Khi bạn xóa một thực thể cha, bạn có thể chỉ định để tự động xóa tất cả các thực thể con liên quan.</p><p class="mt-2 text-slate-600">Để kích hoạt, bạn cần tạo một đối tượng <code>RemoveOptions</code>, sử dụng phương thức <code>.with(&quot;ten_quan_he&quot;)</code> để chỉ định các mối quan hệ cần xóa theo, và truyền nó vào phương thức <code>remove()</code> hoặc <code>removeById()</code>.</p>',3)),l(o,{language:"java",code:u.value},null,8,["code"])])])])]))}};export{f as default};
