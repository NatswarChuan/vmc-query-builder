import{_ as t}from"./CodeBlock-BK92jf-v.js";import{r,c,o as d,b as o,d as n}from"./index-DdSB5D2R.js";const a={id:"cai-dat",class:"mb-12 space-y-4 scroll-mt-20"},g={__name:"InstallationSection",setup(p){const i=r(`
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
`),s=r(`
spring.datasource.url=jdbc:mysql://localhost:3306/vmc_db
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
`);return(l,e)=>(d(),c("section",a,[e[0]||(e[0]=o("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"2. Cài đặt & Cấu hình",-1)),e[1]||(e[1]=o("p",null,"Để bắt đầu, bạn cần tích hợp framework vào dự án Spring Boot của mình.",-1)),e[2]||(e[2]=o("h3",{class:"text-2xl font-semibold text-slate-800 pt-4"},"Bước 1: Thêm Dependency (Maven)",-1)),e[3]||(e[3]=o("p",null,"Đảm bảo `pom.xml` của bạn có các dependency cần thiết. Framework này sử dụng `spring-boot-starter-jdbc`. ",-1)),n(t,{language:"xml",code:i.value},null,8,["code"]),e[4]||(e[4]=o("h3",{class:"text-2xl font-semibold text-slate-800 pt-4"},"Bước 2: Cấu hình DataSource",-1)),e[5]||(e[5]=o("p",null,"Trong file `application.properties`, cấu hình thông tin kết nối đến cơ sở dữ liệu.",-1)),n(t,{language:"properties",code:s.value},null,8,["code"])]))}};export{g as default};
