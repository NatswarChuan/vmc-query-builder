<template>
  <section id="query-builder" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">7. Sử dụng VMCQueryBuilder</h2>
    <p>`VMCQueryBuilder` là một công cụ mạnh mẽ để xây dựng các câu lệnh `SELECT` động. Truy vấn phải được bắt đầu
      bằng phương thức tĩnh `VMCQueryBuilder.from(YourEntity.class)`.</p>
    <p>Ví dụ thực tế từ `UserService.findWithQueryBuilder`:</p>
    <CodeBlock language="java" :code="queryBuilderExample" />
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const queryBuilderExample = ref(`
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.*;

// ... trong UserService.java
  public List<User> findWithQueryBuilder(String roleName) {
    return VMCQueryBuilder.from(User.class, "u")
        .join(VMCSqlJoinType.INNER_JOIN, "user_roles", "ur", "u.id", "=", "ur.user_id")
        .join(VMCSqlJoinType.INNER_JOIN, "roles", "r", "ur.role_id", "=", "r.id")
        .where("r.name", VMCSqlOperator.LIKE, roleName)
        .get();
}
`);
</script>
