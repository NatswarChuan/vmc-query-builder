<template>
  <section id="validation" class="scroll-mt-16">
    <h2 class="text-3xl font-bold text-slate-900">10. Validation Dữ liệu Dựa trên Database</h2>
    <p class="mt-4 text-lg text-slate-700">
      VMC Framework cung cấp một cơ chế validation mạnh mẽ, cho phép bạn định nghĩa các quy tắc kiểm tra dữ liệu
      dựa trên trạng thái của database ngay trong DTO, sử dụng các annotation chuyên dụng.
    </p>

    <article id="validation-field" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">10.1. Validation mức Trường (Field-Level)</h3>
      <p class="mt-2 text-slate-700">
        Validation ở mức trường được dùng để kiểm tra các quy tắc liên quan đến một trường dữ liệu duy nhất.
      </p>

      <h4 class="text-xl font-medium text-slate-800 mt-4">10.1.1. @VMCQueryRule: Kiểm tra Đơn giản</h4>
      <p class="mt-2 text-slate-700">
        Đây là annotation được thiết kế cho các kịch bản kiểm tra phổ biến nhất: <strong>kiểm tra sự tồn tại
          (existence)</strong> hoặc <strong>tính duy nhất (uniqueness)</strong> của một giá trị.
      </p>
      <p class="mt-2 font-semibold text-slate-800">Các thuộc tính chính:</p>
      <ul class="mt-2 list-disc list-inside text-slate-700 space-y-1">
        <li><code>entity</code>: Lớp Entity mục tiêu để thực hiện truy vấn.</li>
        <li><code>field</code>: Tên cột trong database để so sánh với giá trị của trường được validate.</li>
        <li><code>operator</code>: Toán tử so sánh (mặc định là `EQUAL`).</li>
        <li><code>mustNotExist</code>: Logic kiểm tra.
          <ul class="ml-6 list-disc list-inside">
            <li>`true`: Dùng để kiểm tra tính duy nhất. Validation thất bại nếu query tìm thấy bản ghi.</li>
            <li>`false`: Dùng để kiểm tra sự tồn tại. Validation thất bại nếu query không tìm thấy bản ghi.</li>
          </ul>
        </li>
        <li><code>message</code>: Thông báo lỗi.</li>
      </ul>
      <CodeBlock language="java" :code="validationRuleExample" />

      <h4 class="text-xl font-medium text-slate-800 mt-4">10.1.2. @VMCQueryValidation: Kiểm tra Nâng cao với SQL
      </h4>
      <p class="mt-2 text-slate-700">
        Annotation này cung cấp sự linh hoạt tối đa khi bạn cần viết một câu lệnh <strong>SQL tùy chỉnh</strong>
        để validation. Nó có thể được dùng ở cả mức trường và mức lớp.
      </p>
      <p class="mt-2 font-semibold text-slate-800">Các thuộc tính và tham số:</p>
      <ul class="mt-2 list-disc list-inside text-slate-700 space-y-1">
        <li><code>query</code>: Câu lệnh SQL gốc để thực thi.</li>
        <li><code>mustNotExist</code>: Logic kiểm tra. `true` (mặc định) thì validation thất bại nếu query có kết
          quả. `false` thì validation thất bại nếu query không có kết quả.</li>
        <li>Tham số trong query:
          <ul class="ml-6 list-disc list-inside">
            <li><code>:this</code>: (Chỉ dùng ở mức trường) Giá trị của chính trường đang được validate.</li>
            <li><code>:fieldName</code> hoặc <code>:body.fieldName</code>: Giá trị của trường `fieldName` trong
              DTO.</li>
            <li><code>:path.variableName</code>: Giá trị của Path Variable `variableName` từ URL.</li>
            <li><code>:param.parameterName</code>: Giá trị của Request Parameter `parameterName` từ URL.</li>
          </ul>
        </li>
      </ul>
      <CodeBlock language="java" :code="validationQueryExample" />
    </article>

    <article id="validation-class" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">10.2. Validation mức Lớp với @VMCClassValidation</h3>
      <p class="mt-2 text-slate-700">
        <code>@VMCClassValidation</code> được sử dụng khi bạn cần kiểm tra các quy tắc phức tạp liên quan đến
        nhiều trường, nhiều điều kiện và thậm chí nhiều bảng. Nó được đặt ở cấp độ class của DTO và có thể được
        dùng nhiều lần trên cùng một class.
      </p>
      <p class="mt-2 font-semibold text-slate-800">Các thuộc tính chính:</p>
      <ul class="mt-2 list-disc list-inside text-slate-700 space-y-1">
        <li><code>entity</code>: Lớp Entity gốc để bắt đầu truy vấn.</li>
        <li><code>alias</code>: Bí danh cho entity gốc (mặc định là "root").</li>
        <li><code>conditions</code>: Mảng các <code>@VMCFieldCondition</code> để xây dựng mệnh đề `WHERE`.</li>
        <li><code>joins</code>: Mảng các <code>@VMCJoin</code> để join với các bảng khác.</li>
        <li><code>mustNotExist</code>: Logic kiểm tra. <code>true</code> (mặc định) là kiểm tra **sự không tồn
          tại** (lỗi nếu record đã tồn tại). <code>false</code> là kiểm tra **sự tồn tại** (lỗi nếu record không
          tồn tại).</li>
      </ul>

      <h4 class="text-xl font-medium text-slate-800 mt-4">Ví dụ 1: Kiểm tra sự tồn tại của Khóa ngoại</h4>
      <p class="mt-2 text-slate-700">
        Khi tạo một bài viết (Post), cần đảm bảo <code>userId</code> và <code>categoryId</code> được cung cấp là
        hợp lệ.
      </p>
      <CodeBlock language="java" :code="validationClassFkExample" />

      <h4 class="text-xl font-medium text-slate-800 mt-4">Ví dụ 2: Validation Quyền sở hữu với JOIN</h4>
      <p class="mt-2 text-slate-700">
        Kịch bản: Khi cập nhật một bài viết, cần xác thực rằng bài viết (lấy ID từ URL) thực sự thuộc về user (lấy
        thông tin từ body request).
      </p>
      <CodeBlock language="java" :code="validationClassJoinExample" />
      <p class="mt-2 text-slate-700">
        <strong>Luồng hoạt động:</strong> Framework sẽ xây dựng một truy vấn phức hợp tương đương:
        <code>SELECT COUNT(*) FROM posts root JOIN users u ON root.user_id = u.id WHERE root.id = ? AND u.id = ?</code>.
        Giá trị đầu tiên lấy từ path variable, giá trị thứ hai lấy từ trường <code>userId</code> trong request
        body. Nếu không tìm thấy record nào, validation sẽ thất bại.
      </p>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const validationRuleExample = ref(`
// Ví dụ: Kiểm tra email duy nhất khi tạo user
import io.github.natswarchuan.vmc.core.annotation.validation.VMCQueryRule;
import io.github.natswarchuan.vmc.entity.User;

public class UserCreateRequestDto {
    // ...
    @VMCQueryRule(
        entity = User.class,
        field = "email", // So sánh với cột "email" trong bảng users
        mustNotExist = true, // Lỗi nếu email này đã tồn tại
        message = "Email đã tồn tại."
    )
    private String email;
    // ...
}
`);

const validationQueryExample = ref(`
// Ví dụ: Kiểm tra email duy nhất khi cập nhật user, URL: /users/{id}
@VMCQueryValidation(
    query = "SELECT id FROM users WHERE email = :this AND id != :path.id",
    message = "Email đã được sử dụng bởi người dùng khác."
)
private String email;
`);

const validationClassFkExample = ref(`
// Trong file: dto/post/request/PostCreateRequestDto.java
import io.github.natswarchuan.vmc.core.annotation.validation.*;
import io.github.natswarchuan.vmc.entity.*;

@VMCClassValidation(
    entity = User.class,
    conditions = {
      @VMCFieldCondition(name = "userId", source = VMCFieldCondition.Source.BODY)
    },
    mustNotExist = false, // Lỗi nếu User KHÔNG tồn tại
    message = "Người dùng không tồn tại."
)
@VMCClassValidation(
    entity = Category.class,
    conditions = {
      @VMCFieldCondition(name = "categoryId", source = VMCFieldCondition.Source.BODY)
    },
    mustNotExist = false, // Lỗi nếu Category KHÔNG tồn tại
    message = "Danh mục không tồn tại."
)
public class PostCreateRequestDto {
    private String userId;
    private String categoryId;
    //...
}
`);

const validationClassJoinExample = ref(`
// Trong file: dto/post/request/PostUpdateRequestDto.java
import io.github.natswarchuan.vmc.core.annotation.validation.*;
import io.github.natswarchuan.vmc.entity.*;

@VMCClassValidation(
    entity = Post.class, // Bảng gốc là Post
    alias = "root",
    joins = {
      @VMCJoin(entity = User.class, alias = "u", from = "root.user_id", to = "u.id")
    },
    conditions = {
      @VMCFieldCondition(name = "id", source = VMCFieldCondition.Source.PATH, alias = "root"),
      @VMCFieldCondition(name = "userId", source = VMCFieldCondition.Source.BODY, alias = "u", column = "id")
    },
    mustNotExist = false, // Phải tìm thấy record thỏa mãn
    message = "Bài viết không tồn tại, hoặc bạn không có quyền chỉnh sửa bài viết này."
)
public class PostUpdateRequestDto {
    private String userId; 
    // ...
}
`);
</script>
