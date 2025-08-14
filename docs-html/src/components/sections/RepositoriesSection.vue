<template>
  <section id="tao-repositories" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">5. Tạo Repositories</h2>
    <p>Repository là các interface chịu trách nhiệm truy cập dữ liệu, kế thừa từ `VMCRepository&lt;T, ID&gt;`.
      `VMCRepository` đã cung cấp sẵn các phương thức CRUD cơ bản.</p>
    <CodeBlock language="java" :code="repositoryCode" />
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const repositoryCode = ref(`
package io.github.natswarchuan.vmc.repository;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends VMCRepository<User, Long > {
  List<User> findByNameContaining(String name);
}
`);
</script>
