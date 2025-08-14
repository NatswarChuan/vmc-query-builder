<template>
  <section id="anh-xa-quan-he" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">4. Ánh xạ Quan hệ</h2>
    <p>Framework hỗ trợ đầy đủ các mối quan hệ phổ biến trong ORM.</p>

    <article id="onetoone" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">4.1. One-to-One</h3>
      <p>Mỗi `User` có một `UserProfile`. Quan hệ này được định nghĩa bằng <code>@VMCOneToOne</code> và
        <code>@VMCJoinColumn</code>.
      </p>
      <CodeBlock language="java" :code="oneToOneCode" />
    </article>

    <article id="onetomany" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">4.2. One-to-Many / Many-to-One</h3>
      <p>Một `User` có thể viết nhiều `Post`. Quan hệ này được thể hiện bằng <code>@VMCOneToMany</code> và
        <code>@VMCManyToOne</code>.
      </p>
      <CodeBlock language="java" :code="oneToManyCode" />
    </article>

    <article id="manytomany" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">4.3. Many-to-Many</h3>
      <p>Một `User` có thể có nhiều `Role`. Quan hệ này cần bảng trung gian `user_roles` và được định nghĩa bằng
        <code>@VMCManyToMany</code> và <code>@VMCJoinTable</code>.
      </p>
      <CodeBlock language="java" :code="manyToManyCode" />
    </article>

    <article id="lazy-loading" class="scroll-mt-20 pt-4">
      <h3 class="text-2xl font-semibold text-slate-800">4.4. Lazy Loading & Tùy chọn Thao tác</h3>
      <div class="space-y-6">
        <div>
          <h4 class="text-xl font-semibold text-slate-700">Lazy Loading (Tải lười)</h4>
          <p class="mt-2 text-slate-600">Đây là hành vi mặc định cho các mối quan hệ tập hợp
            (<code>One-to-Many</code>, <code>Many-to-Many</code>) để tối ưu hiệu suất. Thay vì tải tất cả các thực
            thể liên quan cùng một lúc, framework sẽ chỉ thực hiện truy vấn để lấy dữ liệu của collection khi bạn
            truy cập vào nó lần đầu tiên (ví dụ: gọi <code>user.getPosts()</code>).</p>
        </div>
        <div>
          <h4 class="text-xl font-semibold text-slate-700">Tùy chọn Lưu (SaveOptions)</h4>
          <p class="mt-2 text-slate-600">Cho phép bạn lưu một thực thể cha và tự động lưu tất cả các
            thực thể con liên quan trong cùng một thao tác (<strong>Cascade Save</strong>). <strong>Quan
              trọng:</strong> Tính năng này không được
            bật mặc định. Để kích hoạt, bạn phải tạo một đối tượng <code>SaveOptions</code>, sử dụng phương thức
            <code>.with("ten_quan_he")</code> để chỉ định các mối quan hệ cần lưu, và truyền nó vào phương thức
            <code>save()</code>.
          </p>
          <CodeBlock language="java" :code="saveOptionsCode" />
        </div>
        <div>
          <h4 class="text-xl font-semibold text-slate-700">Tùy chọn Xóa (RemoveOptions)</h4>
          <p class="mt-2 text-slate-600">Tương tự như `SaveOptions`, `RemoveOptions` cung cấp một cách tường minh
            để kiểm soát hành vi <strong>xóa theo tầng (Cascade Remove)</strong>. Khi bạn xóa một thực thể cha,
            bạn có thể chỉ định để tự động xóa tất cả các thực thể con liên quan.</p>
          <p class="mt-2 text-slate-600">Để kích hoạt, bạn cần tạo một đối tượng <code>RemoveOptions</code>, sử
            dụng phương thức <code>.with("ten_quan_he")</code> để chỉ định các mối quan hệ cần xóa theo, và truyền
            nó vào phương thức <code>remove()</code> hoặc <code>removeById()</code>.</p>
          <CodeBlock language="java" :code="removeOptionsCode" />
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const oneToOneCode = ref(`
// Trong User.java
@VMCOneToOne(mappedBy = "user")
private UserProfile profile;

// Trong UserProfile.java
@VMCOneToOne
@VMCJoinColumn(name = "user_id")
private User user;
`);

const oneToManyCode = ref(`
// Trong User.java
@VMCOneToMany(mappedBy = "user")
private List<Post> posts;

// Trong Post.java
@VMCManyToOne
@VMCJoinColumn(name = "user_id")
private User user;
`);

const manyToManyCode = ref(`
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
`);

const saveOptionsCode = ref(`
// Ví dụ trong UserService
public User createUserAndProfile(UserCreateDto dto) {
    User user = dto.toEntity(); // user có chứa một đối tượng UserProfile mới
    
    // Chỉ định rằng chúng ta muốn lưu cả quan hệ 'profile'
    SaveOptions options = new SaveOptions().with("profile");
    
    // Lưu user, và UserProfile liên quan cũng sẽ được lưu tự động
    return this.save(user, options); 
}
`);

const removeOptionsCode = ref(`
// ... trong UserService
import io.github.natswarchuan.vmc.core.persistence.service.RemoveOptions;

// Ví dụ: Xóa cứng User và tất cả các Post, Profile liên quan
public void deleteUserAndAssociations(Long userId) {
    RemoveOptions options = new RemoveOptions()
                                  .with("posts")    // Xóa các Post liên quan
                                  .with("profile"); // Xóa UserProfile liên quan
                                  
    repository.removeById(userId, options);
}
`);
</script>
