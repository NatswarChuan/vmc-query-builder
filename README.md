# VMC Query Builder Framework

**VMC Query Builder** là một mini-framework cung cấp các chức năng ORM (Object-Relational Mapping) và xây dựng truy vấn SQL một cách linh hoạt, được tích hợp sâu với Spring Boot. Framework này cho phép bạn tương tác với cơ sở dữ liệu quan hệ bằng cách sử dụng các đối tượng Java (Entities) và các interface (Repositories), giúp giảm thiểu việc viết mã SQL thủ công và tăng tốc độ phát triển.

[**Truy cập Tài liệu đầy đủ tại đây**](https://natswarchuan.github.io/vmc-query-builder/)

-----

## Các tính năng chính

  * **Ánh xạ dựa trên Annotation:** Dễ dàng ánh xạ các lớp Java thành các bảng trong cơ sở dữ liệu.
  * **Mô hình Repository:** Cung cấp một lớp trừu tượng cho tầng truy cập dữ liệu, tương tự như Spring Data.
  * **Truy vấn phát sinh (Derived Queries):** Tự động sinh câu lệnh SQL từ tên của phương thức.
  * **Query Builder linh hoạt:** Cung cấp một API fluent để xây dựng các câu lệnh `SELECT` phức tạp.
  * **Hỗ trợ các mối quan hệ:** Quản lý đầy đủ các mối quan hệ `One-to-One`, `One-to-Many`, và `Many-to-Many`.
  * **Lazy Loading & Tùy chọn Thao tác:** Tối ưu hóa hiệu năng và cung cấp các tùy chọn nâng cao cho việc lưu và xóa dữ liệu theo tầng.
  * **Tích hợp Service Layer & DTO:** Cung cấp các lớp cơ sở để giảm mã lặp lại và dễ dàng chuyển đổi dữ liệu.
  * **Validation Truy vấn:** Cung cấp cơ chế validation mạnh mẽ dựa trên dữ liệu trong database.

-----

## Cài đặt & Cấu hình

### 1\. Thêm Dependency (Maven)

Thêm JitPack repository và dependency vào tệp `pom.xml` của bạn:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- Thay ${vmc.version} bằng phiên bản mới nhất -->
    <dependency>
        <groupId>com.github.natswarchuan</groupId>
        <artifactId>vmc-query-builder</artifactId>
        <version>${vmc.version}</version>
    </dependency>
</dependencies>
```

### 2\. Cấu hình DataSource

Trong tệp `application.properties`, cấu hình thông tin kết nối đến cơ sở dữ liệu của bạn:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vmc_db
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

-----

## Hướng dẫn nhanh

### 1\. Định nghĩa một Entity

Tạo một lớp Java kế thừa từ `Model` và sử dụng các annotation để ánh xạ với bảng trong cơ sở dữ liệu.

```java
package io.github.natswarchuan.vmc.entity;

import io.github.natswarchuan.vmc.core.annotation.*;
import io.github.natswarchuan.vmc.core.entity.Model;
import lombok.Data;

@Data
@VMCTable(name = "users")
public class User extends Model {

  @VMCPrimaryKey
  @VMCColumn(name = "id")
  private Long id;

  @VMCColumn(name = "name")
  private String name;

  @VMCColumn(name = "email")
  private String email;
}
```

### 2\. Tạo một Repository

Tạo một interface kế thừa từ `VMCRepository` để thực hiện các thao tác truy vấn. Bạn có thể định nghĩa các phương thức truy vấn phát sinh (derived query methods) ngay tại đây.

```java
package io.github.natswarchuan.vmc.repository;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.entity.User;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends VMCRepository<User, Long> {

  // Tự động tạo truy vấn: SELECT * FROM users WHERE email = ?
  Optional<User> findByEmail(String email);

  // Tự động tạo truy vấn: SELECT * FROM users WHERE name LIKE ?
  List<User> findByNameContaining(String name);
}
```

### 3\. Sử dụng trong Service

Tiêm (inject) `UserRepository` vào Service của bạn và sử dụng các phương thức đã định nghĩa.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    public void processUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // ... xử lý logic
        }
    }
}
```

Để tìm hiểu sâu hơn về các tính năng nâng cao như ánh xạ quan hệ, DTO, validation, và query builder, vui lòng tham khảo [**tài liệu đầy đủ**](https://natswarchuan.github.io/vmc-query-builder/).