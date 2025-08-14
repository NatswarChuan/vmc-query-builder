<template>
  <section id="cai-dat" class="mb-12 space-y-4 scroll-mt-20">
    <h2 class="text-3xl font-bold text-slate-900 border-b pb-2">2. Cài đặt & Cấu hình</h2>
    <p>Để bắt đầu, bạn cần tích hợp framework vào dự án Spring Boot của mình.</p>
    <h3 class="text-2xl font-semibold text-slate-800 pt-4">Bước 1: Thêm Dependency (Maven)</h3>
    <p>Đảm bảo `pom.xml` của bạn có các dependency cần thiết. Framework này sử dụng `spring-boot-starter-jdbc`.
    </p>
    <CodeBlock language="xml" :code="mavenConfig" />

    <h3 class="text-2xl font-semibold text-slate-800 pt-4">Bước 2: Cấu hình DataSource</h3>
    <p>Trong file `application.properties`, cấu hình thông tin kết nối đến cơ sở dữ liệu.</p>
    <CodeBlock language="properties" :code="appProperties" />
  </section>
</template>

<script setup>
import { ref } from 'vue';
import CodeBlock from '../CodeBlock.vue';

const mavenConfig = ref(`
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<properties>
 <!-- Phiên bản mới nhất lấy tại "https://jitpack.io/#natswarchuan/vmc-query-builder" -->
    <vmc.version>\${Phiên bản mới nhất}</vmc.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
<dependencies>
    <!-- Thư viện VMC Query Builder -->
    <dependency>
        <groupId>com.github.natswarchuan</groupId>
        <artifactId>vmc-query-builder</artifactId>
        <version>\${vmc.version}</version>
    </dependency>

    <!-- Spring Boot Starter cho việc xây dựng ứng dụng web, bao gồm RESTful API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter cho việc xác thực dữ liệu (validation) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Cung cấp trình kết nối JDBC cho MySQL để ứng dụng có thể giao tiếp với cơ sở dữ liệu MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- lombok cho việc tạo các getter, setter và constructor đơn giản -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>\${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
`);

const appProperties = ref(`
spring.datasource.url=jdbc:mysql://localhost:3306/vmc_db
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
`);
</script>
