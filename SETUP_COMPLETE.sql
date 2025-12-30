-- =====================================================
-- SCRIPT SETUP HOÀN CHỈNH CHO ĐỒ ÁN
-- BẢO MẬT CƠ SỞ DỮ LIỆU - QUẢN LÝ USER ORACLE
-- =====================================================
-- Chạy script này bằng SYS AS SYSDBA
-- =====================================================

-- =====================================================
-- BƯỚC 1: TẠO PLUGGABLE DATABASE (Nếu chưa có)
-- =====================================================
-- Bỏ qua bước này nếu ORCLPDB đã tồn tại

-- CREATE PLUGGABLE DATABASE orclpdb
-- ADMIN USER pdbadmin IDENTIFIED BY pdbadmin123
-- FILE_NAME_CONVERT = ('pdbseed', 'orclpdb');
-- 
-- ALTER PLUGGABLE DATABASE orclpdb OPEN;
-- ALTER PLUGGABLE DATABASE orclpdb SAVE STATE;

-- =====================================================
-- BƯỚC 2: CONNECT VÀO PDB
-- =====================================================
CONNECT / AS SYSDBA
ALTER SESSION SET CONTAINER = orclpdb;

-- Xác nhận đang ở đúng PDB
SHOW CON_NAME;
-- Phải hiển thị: ORCLPDB

-- =====================================================
-- BƯỚC 3: TẠO TABLESPACES
-- =====================================================
-- Tablespace cho dữ liệu nghiệp vụ
CREATE TABLESPACE TS_APP_DATA
DATAFILE 'TS_APP_DATA01.DBF'
SIZE 100M
AUTOEXTEND ON NEXT 10M
MAXSIZE UNLIMITED;

-- Tablespace cho index
CREATE TABLESPACE TS_APP_INDEX
DATAFILE 'TS_APP_INDEX01.DBF'
SIZE 50M
AUTOEXTEND ON NEXT 10M
MAXSIZE UNLIMITED;

-- =====================================================
-- BƯỚC 4: TẠO USER SEC_ADMIN (ADMIN BẢO MẬT)
-- =====================================================
CREATE USER SEC_ADMIN IDENTIFIED BY admin123
DEFAULT TABLESPACE TS_APP_DATA
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON TS_APP_DATA;

-- Grant quyền cơ bản
GRANT CREATE SESSION TO SEC_ADMIN;

-- Grant quyền quản lý user
GRANT CREATE USER TO SEC_ADMIN;
GRANT ALTER USER TO SEC_ADMIN;
GRANT DROP USER TO SEC_ADMIN;

-- Grant quyền quản lý role
GRANT CREATE ROLE TO SEC_ADMIN;
GRANT ALTER ANY ROLE TO SEC_ADMIN;
GRANT DROP ANY ROLE TO SEC_ADMIN;
GRANT GRANT ANY ROLE TO SEC_ADMIN;

-- Grant quyền quản lý profile
GRANT CREATE PROFILE TO SEC_ADMIN;
GRANT ALTER PROFILE TO SEC_ADMIN;
GRANT DROP PROFILE TO SEC_ADMIN;

-- Grant quyền grant privileges
GRANT GRANT ANY PRIVILEGE TO SEC_ADMIN;

-- Grant quyền xem dictionary
GRANT SELECT ANY DICTIONARY TO SEC_ADMIN;

-- =====================================================
-- BƯỚC 5: TẠO USER APP_OWNER (CHỨA SCHEMA NGHIỆP VỤ)
-- =====================================================
CREATE USER APP_OWNER IDENTIFIED BY app123
DEFAULT TABLESPACE TS_APP_DATA
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON TS_APP_DATA;

-- Grant quyền
GRANT CREATE SESSION TO APP_OWNER;
GRANT CREATE TABLE TO APP_OWNER;
GRANT CREATE VIEW TO APP_OWNER;
GRANT CREATE SEQUENCE TO APP_OWNER;
GRANT CREATE PROCEDURE TO APP_OWNER;
GRANT CREATE TRIGGER TO APP_OWNER;

-- =====================================================
-- BƯỚC 6: TẠO ROLES DEMO
-- =====================================================
CONNECT SEC_ADMIN/admin123@localhost:1521/orclpdb.lan

-- Role cho nhân viên thường
CREATE ROLE R_EMPLOYEE;
GRANT CREATE SESSION TO R_EMPLOYEE;

-- Role cho quản lý
CREATE ROLE R_MANAGER;
GRANT R_EMPLOYEE TO R_MANAGER;
GRANT SELECT ANY TABLE TO R_MANAGER;

-- Role cho admin
CREATE ROLE R_ADMIN;
GRANT R_MANAGER TO R_ADMIN;
GRANT CREATE USER TO R_ADMIN;

-- =====================================================
-- BƯỚC 7: TẠO PROFILES DEMO
-- =====================================================
CREATE PROFILE P_STANDARD LIMIT
    SESSIONS_PER_USER 2
    CONNECT_TIME 60
    IDLE_TIME 30;

CREATE PROFILE P_UNLIMITED LIMIT
    SESSIONS_PER_USER UNLIMITED
    CONNECT_TIME UNLIMITED
    IDLE_TIME UNLIMITED;

-- =====================================================
-- BƯỚC 8: TẠO USER DEMO
-- =====================================================
CREATE USER U_USER01 IDENTIFIED BY user123
DEFAULT TABLESPACE TS_APP_DATA
TEMPORARY TABLESPACE TEMP
QUOTA 20M ON TS_APP_DATA
PROFILE P_STANDARD;

GRANT CREATE SESSION TO U_USER01;
GRANT R_EMPLOYEE TO U_USER01;

-- =====================================================
-- BƯỚC 9: TẠO TABLES TRONG APP_OWNER
-- =====================================================
CONNECT APP_OWNER/app123@localhost:1521/orclpdb.lan

-- Table quản lý login ứng dụng (BCrypt password)
CREATE TABLE APP_LOGIN_USER (
    USERNAME VARCHAR2(30) PRIMARY KEY,
    PASSWORD_HASH VARCHAR2(100) NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
);

-- Table thông tin user (demo Object/Column Privilege)
CREATE TABLE APP_USER_PROFILE (
    USERNAME VARCHAR2(30) PRIMARY KEY,
    FULL_NAME VARCHAR2(100),
    EMAIL VARCHAR2(100),
    PHONE VARCHAR2(20),
    ADDRESS VARCHAR2(200),
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
);

-- Insert dữ liệu mẫu
INSERT INTO APP_USER_PROFILE VALUES 
('U_USER01', 'Nguyen Van A', 'nguyenvana@example.com', '0901234567', '123 Nguyen Hue, Ho Chi Minh', SYSTIMESTAMP);

INSERT INTO APP_USER_PROFILE VALUES 
('SEC_ADMIN', 'Security Administrator', 'admin@company.com', '0900000000', 'Head Office', SYSTIMESTAMP);

COMMIT;

-- Table demo khác (cho Object Privilege)
CREATE TABLE EMPLOYEES (
    EMP_ID NUMBER PRIMARY KEY,
    EMP_NAME VARCHAR2(100) NOT NULL,
    EMAIL VARCHAR2(100),
    PHONE VARCHAR2(20),
    DEPARTMENT VARCHAR2(50),
    SALARY NUMBER(10,2)
);

INSERT INTO EMPLOYEES VALUES 
(1, 'Tran Thi B', 'tranthib@company.com', '0912345678', 'IT', 15000000);

INSERT INTO EMPLOYEES VALUES 
(2, 'Le Van C', 'levanc@company.com', '0923456789', 'HR', 12000000);

INSERT INTO EMPLOYEES VALUES 
(3, 'Pham Thi D', 'phamthid@company.com', '0934567890', 'Finance', 18000000);

COMMIT;

-- =====================================================
-- BƯỚC 10: TẠO VPD FUNCTION VÀ POLICY
-- =====================================================
-- Function filter theo SESSION_USER
CREATE OR REPLACE FUNCTION FN_VPD_USER_PROFILE (
    p_schema VARCHAR2,
    p_object VARCHAR2
) RETURN VARCHAR2 IS
    v_user VARCHAR2(30);
BEGIN
    -- Lấy username của user đang login
    v_user := SYS_CONTEXT('USERENV', 'SESSION_USER');
    
    -- SEC_ADMIN xem toàn bộ
    IF v_user = 'SEC_ADMIN' THEN
        RETURN '1=1';
    END IF;
    
    -- APP_OWNER xem toàn bộ
    IF v_user = 'APP_OWNER' THEN
        RETURN '1=1';
    END IF;
    
    -- User khác chỉ xem data của mình
    RETURN 'USERNAME = ''' || v_user || '''';
END;
/

-- Gắn policy (phải chạy bằng SYS)
CONNECT / AS SYSDBA
ALTER SESSION SET CONTAINER = orclpdb;

BEGIN
    -- Xóa policy cũ nếu có
    BEGIN
        DBMS_RLS.DROP_POLICY(
            object_schema   => 'APP_OWNER',
            object_name     => 'APP_USER_PROFILE',
            policy_name     => 'PL_VPD_USER_PROFILE'
        );
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;
    
    -- Thêm policy mới
    DBMS_RLS.ADD_POLICY(
        object_schema   => 'APP_OWNER',
        object_name     => 'APP_USER_PROFILE',
        policy_name     => 'PL_VPD_USER_PROFILE',
        function_schema => 'APP_OWNER',
        policy_function => 'FN_VPD_USER_PROFILE',
        statement_types => 'SELECT, UPDATE, DELETE',
        update_check    => TRUE
    );
END;
/

-- =====================================================
-- BƯỚC 11: GRANT QUYỀN ĐỂ USER THƯỜNG XEM TABLE
-- =====================================================
-- Để U_USER01 có thể query APP_USER_PROFILE
GRANT SELECT ON APP_OWNER.APP_USER_PROFILE TO U_USER01;
GRANT UPDATE ON APP_OWNER.APP_USER_PROFILE TO U_USER01;

-- Demo Column Privilege
GRANT SELECT (EMP_NAME, EMAIL, DEPARTMENT) ON APP_OWNER.EMPLOYEES TO U_USER01;

-- =====================================================
-- BƯỚC 12: SETUP AUDIT (STANDARD AUDIT)
-- =====================================================
-- Audit DDL operations
AUDIT CREATE ROLE;
AUDIT DROP ROLE;
AUDIT GRANT ANY ROLE;
AUDIT CREATE USER;
AUDIT DROP USER;
AUDIT ALTER USER;
AUDIT CREATE PROFILE;
AUDIT ALTER PROFILE;
AUDIT DROP PROFILE;

-- =====================================================
-- BƯỚC 13: SETUP FGA (FINE-GRAINED AUDITING)
-- =====================================================
-- Audit khi truy cập các cột nhạy cảm
BEGIN
    -- Xóa policy cũ nếu có
    BEGIN
        DBMS_FGA.DROP_POLICY(
            object_schema => 'APP_OWNER',
            object_name   => 'APP_USER_PROFILE',
            policy_name   => 'FGA_AUDIT_CONTACT'
        );
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;
    
    -- Thêm policy mới
    DBMS_FGA.ADD_POLICY(
        object_schema   => 'APP_OWNER',
        object_name     => 'APP_USER_PROFILE',
        policy_name     => 'FGA_AUDIT_CONTACT',
        audit_column    => 'EMAIL, PHONE',
        statement_types => 'SELECT'
    );
END;
/

-- Audit truy cập ngoài giờ
BEGIN
    -- Xóa policy cũ nếu có
    BEGIN
        DBMS_FGA.DROP_POLICY(
            object_schema => 'APP_OWNER',
            object_name   => 'APP_USER_PROFILE',
            policy_name   => 'FGA_AUDIT_AFTER_HOURS'
        );
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;
    
    -- Thêm policy mới
    DBMS_FGA.ADD_POLICY(
        object_schema   => 'APP_OWNER',
        object_name     => 'APP_USER_PROFILE',
        policy_name     => 'FGA_AUDIT_AFTER_HOURS',
        audit_condition => 'TO_NUMBER(TO_CHAR(SYSDATE,''HH24'')) NOT BETWEEN 8 AND 18',
        statement_types => 'SELECT'
    );
END;
/

-- =====================================================
-- BƯỚC 14: THÊM SEC_ADMIN VÀO APP_LOGIN_USER
-- =====================================================
-- Lưu ý: Password hash này chỉ là ví dụ
-- Spring Boot sẽ tự động tạo hash khi SEC_ADMIN login lần đầu
CONNECT APP_OWNER/app123@localhost:1521/orclpdb.lan

-- Check xem đã có chưa
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM APP_LOGIN_USER WHERE USERNAME = 'SEC_ADMIN';
    
    IF v_count = 0 THEN
        -- Chưa có, insert
        -- Spring Boot sẽ tự động sync khi login lần đầu
        -- Hoặc có thể insert thủ công với BCrypt hash
        NULL;
    ELSE
        DBMS_OUTPUT.PUT_LINE('SEC_ADMIN already exists in APP_LOGIN_USER');
    END IF;
END;
/

-- =====================================================
-- BƯỚC 15: VERIFICATION - KIỂM TRA SETUP
-- =====================================================
-- Kiểm tra users
SELECT 'USERS' as CHECK_TYPE, USERNAME, ACCOUNT_STATUS, DEFAULT_TABLESPACE, PROFILE
FROM DBA_USERS
WHERE USERNAME IN ('SEC_ADMIN', 'APP_OWNER', 'U_USER01')
ORDER BY USERNAME;

-- Kiểm tra roles
SELECT 'ROLES' as CHECK_TYPE, ROLE
FROM DBA_ROLES
WHERE ROLE IN ('R_EMPLOYEE', 'R_MANAGER', 'R_ADMIN')
ORDER BY ROLE;

-- Kiểm tra profiles
SELECT 'PROFILES' as CHECK_TYPE, PROFILE
FROM DBA_PROFILES
WHERE PROFILE IN ('P_STANDARD', 'P_UNLIMITED')
GROUP BY PROFILE
ORDER BY PROFILE;

-- Kiểm tra privileges của SEC_ADMIN
SELECT 'SEC_ADMIN PRIVILEGES' as CHECK_TYPE, PRIVILEGE
FROM DBA_SYS_PRIVS
WHERE GRANTEE = 'SEC_ADMIN'
ORDER BY PRIVILEGE;

-- Kiểm tra roles của U_USER01
SELECT 'U_USER01 ROLES' as CHECK_TYPE, GRANTED_ROLE
FROM DBA_ROLE_PRIVS
WHERE GRANTEE = 'U_USER01'
ORDER BY GRANTED_ROLE;

-- Kiểm tra tables trong APP_OWNER
SELECT 'APP_OWNER TABLES' as CHECK_TYPE, TABLE_NAME
FROM DBA_TABLES
WHERE OWNER = 'APP_OWNER'
ORDER BY TABLE_NAME;

-- Kiểm tra VPD policy
SELECT 'VPD POLICIES' as CHECK_TYPE, OBJECT_OWNER, OBJECT_NAME, POLICY_NAME, FUNCTION
FROM DBA_POLICIES
WHERE OBJECT_OWNER = 'APP_OWNER'
ORDER BY OBJECT_NAME;

-- Kiểm tra FGA policies
SELECT 'FGA POLICIES' as CHECK_TYPE, OBJECT_SCHEMA, OBJECT_NAME, POLICY_NAME
FROM DBA_AUDIT_POLICIES
WHERE OBJECT_SCHEMA = 'APP_OWNER'
ORDER BY OBJECT_NAME;

-- =====================================================
-- BƯỚC 16: TEST VPD
-- =====================================================
PROMPT ========================================
PROMPT TESTING VPD - Row Level Security
PROMPT ========================================

-- Test 1: SEC_ADMIN sees all rows
CONNECT SEC_ADMIN/admin123@localhost:1521/orclpdb.lan
PROMPT Test 1: SEC_ADMIN query (should see ALL rows)
SELECT USERNAME, FULL_NAME, EMAIL FROM APP_OWNER.APP_USER_PROFILE;

-- Test 2: U_USER01 sees only own row
CONNECT U_USER01/user123@localhost:1521/orclpdb.lan
PROMPT Test 2: U_USER01 query (should see ONLY U_USER01 row)
SELECT USERNAME, FULL_NAME, EMAIL FROM APP_OWNER.APP_USER_PROFILE;

-- Test 3: Column Privilege
PROMPT Test 3: U_USER01 query EMPLOYEES (should see only allowed columns)
SELECT EMP_NAME, EMAIL, DEPARTMENT FROM APP_OWNER.EMPLOYEES;

PROMPT Test 4: U_USER01 query SALARY column (should FAIL)
-- SELECT SALARY FROM APP_OWNER.EMPLOYEES;
-- Expected: ORA-00904: "SALARY": invalid identifier

-- =====================================================
-- SETUP HOÀN TẤT
-- =====================================================
CONNECT / AS SYSDBA
ALTER SESSION SET CONTAINER = orclpdb;

PROMPT ========================================
PROMPT SETUP COMPLETE!
PROMPT ========================================
PROMPT 
PROMPT Next steps:
PROMPT 1. Start Spring Boot application: ./gradlew bootRun
PROMPT 2. Open browser: http://localhost:8080
PROMPT 3. Login with:
PROMPT    - SEC_ADMIN / admin123 (Admin)
PROMPT    - U_USER01 / user123 (User)
PROMPT 
PROMPT Database setup summary:
PROMPT - Tablespaces: TS_APP_DATA, TS_APP_INDEX
PROMPT - Users: SEC_ADMIN, APP_OWNER, U_USER01
PROMPT - Roles: R_EMPLOYEE, R_MANAGER, R_ADMIN
PROMPT - Profiles: P_STANDARD, P_UNLIMITED
PROMPT - Tables: APP_LOGIN_USER, APP_USER_PROFILE, EMPLOYEES
PROMPT - VPD: Active on APP_USER_PROFILE
PROMPT - FGA: Active on APP_USER_PROFILE
PROMPT - Audit: Enabled for DDL operations
PROMPT ========================================

