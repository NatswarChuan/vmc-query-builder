<template>
  <section id="dinh-nghia-entities" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">3. Định nghĩa Entities</h2>
    <p>Entities là các lớp Java đại diện cho các bảng trong cơ sở dữ liệu. Tất cả các entity phải kế thừa từ lớp
      `Model`.</p>
    <div class="bg-blue-100 border-l-4 border-blue-500 text-blue-700 p-4 rounded-md" role="alert">
      <p class="font-bold">Annotation cơ bản</p>
      <ul class="list-disc list-inside mt-2">
        <li><code>@VMCTable(name = "...")</code>: Đánh dấu lớp là entity và chỉ định tên bảng.</li>
        <li><code>@VMCPrimaryKey</code>: Đánh dấu trường là khóa chính.</li>
        <li><code>@VMCColumn(name = "...")</code>: Ánh xạ trường thành cột. Nếu không có `name`, tên cột sẽ được
          lấy từ tên trường.</li>
      </ul>
    </div>
    <h3 class="text-2xl font-semibold text-slate-800 pt-4">Ví dụ: Entity `User`</h3>
    <CodeBlock language="java" :code="userEntityCode" />
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const userEntityCode = ref(`
package io.github.natswarchuan.vmc.entity;

import io.github.natswarchuan.vmc.core.annotation.*;
import io.github.natswarchuan.vmc.core.entity.Model;
import java.time.LocalDateTime;
import lombok.*;

@Data
@VMCTable(name = "users")
public class User extends Model {

  @VMCPrimaryKey(name = "id")
  @VMCColumn(name = "id")
  private Long id;

  @VMCColumn(name = "name")
  private String name;

  @VMCColumn(name = "email")
  private String email;

  @VMCColumn(name = "created_at")
  private LocalDateTime createdAt;

  @VMCColumn(name = "updated_at")
  private LocalDateTime updatedAt;
    // ...
}
`);
</script>
