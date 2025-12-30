# ỨNG DỤNG QUẢN LÝ NGƯỜI DÙNG ORACLE DATABASE

Đồ án môn Bảo mật Cơ sở Dữ liệu - Năm học 2024-2025

## GIỚI THIỆU

Ứng dụng web quản lý người dùng Oracle Database với các tính năng:
- Quản lý User, Role, Profile, Privilege
- Áp dụng Virtual Private Database (VPD)
- Áp dụng RBAC, Audit, FGA
- Oracle quyết định bảo mật, không hardcode

## CÔNG NGHỆ

**Backend:**
- Spring Boot 3.2.0
- Spring MVC + Thymeleaf
- Spring JDBC
- Oracle JDBC Driver

**Frontend:**
- Thymeleaf Template Engine
- Bootstrap 5.3
- Google Fonts (Inter)

**Database:**
- Oracle Database 19c Enterprise Edition
- Pluggable Database (ORCLPDB)

**Security:**
- BCrypt Password Hashing
- Oracle User/Role/Profile Management
- VPD (Row-Level Security)
- Unified Auditing + FGA

## CÀI ĐẶT

### Yêu cầu

- Oracle Database 19c
- JDK 17+
- Gradle 8.x

### Các bước

**1. Setup Oracle Database:**

```bash
# Chạy script setup
sqlplus / as sysdba @SETUP_COMPLETE.sql
```

**2. Cấu hình application:**

Chỉnh sửa `src/main/resources/application.properties`:

```properties
spring.datasource.admin.url=jdbc:oracle:thin:@//localhost:1521/orclpdb.lan
spring.datasource.admin.username=SEC_ADMIN
spring.datasource.admin.password=admin123

spring.datasource.app.url=jdbc:oracle:thin:@//localhost:1521/orclpdb.lan
spring.datasource.app.username=APP_OWNER
spring.datasource.app.password=app123
```

**3. Build và chạy:**

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun
```

**4. Truy cập ứng dụng:**

- URL: http://localhost:8080
- Admin: SEC_ADMIN / admin123
- User: U_USER01 / user123

## CHỨC NĂNG

### Quản lý User
- Tạo user mới với tablespace, quota, profile
- Sửa thông tin user
- Lock/Unlock user
- Xóa user
- Xem danh sách users với roles, privileges

### Quản lý Role
- Tạo role (có hoặc không có password)
- Xóa role
- Xem privileges của role
- Xem users được gán role
- Grant/Revoke role

### Quản lý Profile
- Tạo profile với resource limits
- Sửa resource limits (Sessions, Connect time, Idle time)
- Xóa profile
- Gán profile cho user

### Quản lý Privilege
- Grant/Revoke System Privilege
- Grant/Revoke Object Privilege
- Grant/Revoke Column Privilege
- Hỗ trợ WITH ADMIN OPTION và WITH GRANT OPTION
- Xem danh sách tất cả privileges

### Hiển thị Oracle System Tables
- DBA_USERS
- DBA_ROLES
- DBA_PROFILES
- DBA_SYS_PRIVS
- DBA_TAB_PRIVS
- DBA_COL_PRIVS
- DBA_ROLE_PRIVS
- DBA_TS_QUOTAS

## DEMO VPD (VIRTUAL PRIVATE DATABASE)

VPD tự động filter dữ liệu theo user:

```sql
-- SEC_ADMIN sees all rows
CONNECT SEC_ADMIN/admin123@localhost:1521/orclpdb.lan
SELECT * FROM APP_OWNER.APP_USER_PROFILE;
-- Result: ALL rows

-- U_USER01 sees only own row
CONNECT U_USER01/user123@localhost:1521/orclpdb.lan
SELECT * FROM APP_OWNER.APP_USER_PROFILE;
-- Result: 1 row (U_USER01)
```

**Không có code Java nào filter!** Oracle VPD tự động enforce.

## DEMO ORACLE ENFORCE

User không có quyền bị Oracle từ chối:

```sql
CONNECT U_USER01/user123@localhost:1521/orclpdb.lan
CREATE USER TEST IDENTIFIED BY test123;
-- Result: ORA-01031: insufficient privileges
```

**Code không kiểm tra!** Oracle từ chối ngay.

## KIẾN TRÚC

### 3-Layer Architecture (Passive MVP)

```
[Presentation Layer]
      ↓
[Business Layer]
      ↓
[Data Layer]
      ↓
[Security Layer - ORACLE]
```

### Multiple DataSources

- **SEC_ADMIN Connection:** Cho DDL operations (CREATE USER, GRANT, etc.)
- **APP_OWNER Connection:** Cho business data queries

### Nguyên tắc cốt lõi

1. **Oracle quyết định bảo mật** - Không hardcode trong code
2. **Separation of Concerns** - Mỗi layer có trách nhiệm riêng
3. **Multiple DataSources** - Phân tách quyền rõ ràng

## CẤU TRÚC PROJECT

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── config/          (DataSource configuration)
│   │   │   ├── controller/      (MVC Controllers)
│   │   │   ├── service/         (Business Logic)
│   │   │   ├── repository/      (Data Access)
│   │   │   └── model/           (Domain Models)
│   │   └── resources/
│   │       ├── templates/       (Thymeleaf Views)
│   │       ├── static/          (CSS, JS)
│   │       └── application.properties
├── build.gradle
├── BAO_CAO_DO_AN.md           (Báo cáo chi tiết)
├── SETUP_COMPLETE.sql         (Script setup Oracle)
└── README.md                  (File này)
```

## TÀI LIỆU

- **BAO_CAO_DO_AN.md:** Báo cáo đồ án đầy đủ với code, SQL, giải thích chi tiết
- **SETUP_COMPLETE.sql:** Script setup Oracle hoàn chỉnh
- **HUONG_DAN_DEMO_CHO_CO.md:** Hướng dẫn demo cho giảng viên
- **LUU_Y_QUAN_TRONG.md:** Lưu ý quan trọng khi demo
- **BẢO MẬT CSDL.txt:** Yêu cầu đề bài chi tiết

## KỸ THUẬT ĐÃ ÁP DỤNG

| Kỹ thuật | Trạng thái | Ghi chú |
|----------|------------|---------|
| User Management | 100% | Oracle enforce |
| RBAC (Roles) | 100% | R_EMPLOYEE, R_MANAGER, R_ADMIN |
| VPD | 100% | Row-level security |
| Oracle Profiles | 100% | Resource limits |
| System Privileges | 100% | DDL enforcement |
| Object/Column Privileges | 100% | Table access control |
| Multiple DataSources | 100% | SEC_ADMIN + APP_OWNER |
| BCrypt Hashing | 100% | Password security |
| Standard Audit | Setup | Chưa có UI |
| FGA | Setup | Chưa có UI |
| PL/SQL Packages | Setup | Chưa dùng |

**Tổng: 87% hoàn thành (96/110 điểm)**

## TROUBLESHOOTING

### Application không kết nối được Oracle

Kiểm tra:
1. Oracle listener đang chạy: `lsnrctl status`
2. PDB đã open: `ALTER PLUGGABLE DATABASE orclpdb OPEN;`
3. Connection string đúng trong application.properties
4. Firewall không block port 1521

### SEC_ADMIN không có quyền

Xem hướng dẫn trong file `SETUP_COMPLETE.sql` để khôi phục quyền.

### VPD không hoạt động

Kiểm tra policy:

```sql
SELECT OBJECT_OWNER, OBJECT_NAME, POLICY_NAME, ENABLE
FROM DBA_POLICIES
WHERE OBJECT_OWNER = 'APP_OWNER';
```

## TÁC GIẢ

- Họ tên: [Họ và tên sinh viên]
- MSSV: [Mã số sinh viên]
- Lớp: [Lớp]
- Email: [Email]

## GIẤY PHÉP

Đồ án này được tạo ra cho mục đích học tập.

---

**Xem thêm:** [BAO_CAO_DO_AN.md](BAO_CAO_DO_AN.md) để có báo cáo chi tiết đầy đủ.

