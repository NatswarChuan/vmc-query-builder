import{_ as r}from"./CodeBlock-BI-7EVGF.js";import{r as o,c as d,o as l,b as e,e as n,a as i,d as s}from"./index-BVd1TkhY.js";const u={id:"phuong-thuc-truy-van",class:"mb-12 space-y-4 scroll-mt-20"},h={id:"derived-queries",class:"scroll-mt-20 pt-4"},m={id:"vmc-query",class:"scroll-mt-20 pt-4"},E={__name:"QueryMethodsSection",setup(g){const a=o(`
// Trong UserRepository.java

// Tìm tất cả user có tên chứa một chuỗi và sắp xếp theo email giảm dần
// SELECT * FROM users WHERE name LIKE ? ORDER BY email DESC
List<User> findByNameContainingOrderByEmailDesc(String name);

// Đếm số lượng user có email cụ thể
// SELECT COUNT(*) FROM users WHERE email = ?
long countByEmail(String email);

// Tìm user đầu tiên có tuổi lớn hơn một giá trị
// SELECT * FROM users WHERE age > ? LIMIT 1
Optional<User> findFirstByAgeGreaterThan(int age);

// Xóa tất cả user được tạo trước một ngày
// DELETE FROM users WHERE created_at < ?
void deleteByCreatedAtLessThan(LocalDateTime date);

// Tìm DTO của user bằng email
// Tự động chuyển đổi User entity sang UserDto
Optional<UserDto> findDtoByEmail(String email);
`),c=o(`
// Trong UserRepository.java
import io.github.natswarchuan.vmc.core.annotation.VMCQuery;
import io.github.natswarchuan.vmc.core.annotation.VMCParam;

@VMCQuery(value = "SELECT u.* FROM users u " +
        "JOIN user_roles ur ON u.id = ur.user_id " +
        "JOIN roles r ON ur.role_id = r.id " +
        "WHERE r.name = :roleName")
List<User> findUsersByRoleName(@VMCParam("roleName") String roleName);
`);return(p,t)=>(l(),d("section",u,[t[3]||(t[3]=e("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"6. Các phương thức truy vấn",-1)),t[4]||(t[4]=e("p",null,[n("Framework cung cấp hai cách chính để định nghĩa các truy vấn trong interface Repository của bạn: "),e("strong",null,"Truy vấn phát (Derived Queries)"),n(" và "),e("strong",null,"Truy vấn tùy chỉnh (Custom Queries)"),n(". ")],-1)),e("article",h,[t[0]||(t[0]=i('<h3 class="text-2xl font-semibold text-slate-800">6.1. Truy vấn phát sinh (Derived Query Methods)</h3><p>Đây là cách mạnh mẽ nhất để tạo truy vấn mà không cần viết một dòng SQL nào. Framework sẽ tự động phân tích tên phương thức của bạn và tạo ra câu lệnh SQL tương ứng. Cấu trúc của tên phương thức tuân theo mẫu sau:</p><p class="font-mono bg-slate-100 p-2 rounded-md text-sm mt-2"><code>(action)(Dto)?(quantifier)By(criteria)(OrderBy...)</code></p><div class="mt-4 space-y-2"><p><strong>Action:</strong> Tiền tố bắt buộc, xác định hành động của truy vấn. Bao gồm: <code>find</code>, <code>get</code>, <code>count</code>, <code>delete</code>, <code>remove</code>. </p><p><strong>Dto (Tùy chọn):</strong> Nếu có từ khóa `Dto`, phương thức sẽ tự động trả về đối tượng DTO thay vì Entity.</p><p><strong>Quantifier (Tùy chọn):</strong> Giới hạn kết quả trả về. Bao gồm: <code>All</code> (mặc định nếu trả về List), <code>First</code> (trả về một đối tượng duy nhất).</p><p><strong>Criteria:</strong> Phần quan trọng nhất, bắt đầu bằng từ khóa <code>By</code>. Bạn định nghĩa các điều kiện `WHERE` bằng cách kết hợp tên thuộc tính của Entity với các toán tử được hỗ trợ. Các điều kiện có thể được nối với nhau bằng <code>And</code> hoặc <code>Or</code>.</p><p><strong>OrderBy (Tùy chọn):</strong> Sắp xếp kết quả. Cú pháp: <code>OrderBy&lt;PropertyName&gt;Asc|Desc</code>. </p></div><h4 class="text-xl font-semibold text-slate-700 mt-4">Các toán tử điều kiện được hỗ trợ</h4><table class="w-full mt-2 border-collapse text-left"><thead class="bg-slate-100"><tr><th class="p-2 border">Từ khóa</th><th class="p-2 border">Ví dụ</th><th class="p-2 border">SQL tương đương</th></tr></thead><tbody><tr class="border-b"><td class="p-2 border font-mono">Like, Containing</td><td class="p-2 border font-mono">findByNameContaining(String name)</td><td class="p-2 border font-mono">WHERE name LIKE ?</td></tr><tr class="border-b"><td class="p-2 border font-mono">In</td><td class="p-2 border font-mono">findByStatusIn(Collection&lt;String&gt; statuses)</td><td class="p-2 border font-mono">WHERE status IN (...)</td></tr><tr class="border-b"><td class="p-2 border font-mono">Between</td><td class="p-2 border font-mono">findByAgeBetween(int start, int end)</td><td class="p-2 border font-mono">WHERE age BETWEEN ? AND ?</td></tr><tr class="border-b"><td class="p-2 border font-mono">GreaterThan, LessThan, ...Equal</td><td class="p-2 border font-mono">findByStartDateGreaterThanEqual(Date date)</td><td class="p-2 border font-mono">WHERE start_date &gt;= ?</td></tr><tr class="border-b"><td class="p-2 border font-mono">IsNull, IsNotNull</td><td class="p-2 border font-mono">findByEndDateIsNull()</td><td class="p-2 border font-mono">WHERE end_date IS NULL</td></tr><tr class="border-b"><td class="p-2 border font-mono">True, False</td><td class="p-2 border font-mono">findByIsActiveTrue()</td><td class="p-2 border font-mono">WHERE is_active = true</td></tr></tbody></table><h4 class="text-xl font-semibold text-slate-700 mt-4">Ví dụ thực tế</h4>',7)),s(r,{language:"java",code:a.value},null,8,["code"])]),e("article",m,[t[1]||(t[1]=e("h3",{class:"text-2xl font-semibold text-slate-800"},"6.2. Truy vấn tùy chỉnh với @VMCQuery",-1)),t[2]||(t[2]=e("p",null,[n("Đối với các truy vấn phức tạp không thể biểu diễn bằng tên phương thức, bạn có thể sử dụng "),e("code",null,"@VMCQuery"),n(" để viết câu lệnh SQL gốc. Các tham số của phương thức phải được đánh dấu bằng "),e("code",null,"@VMCParam"),n(" để ánh xạ với các tham số được đặt tên trong câu lệnh SQL. ")],-1)),s(r,{language:"java",code:c.value},null,8,["code"])])]))}};export{E as default};
