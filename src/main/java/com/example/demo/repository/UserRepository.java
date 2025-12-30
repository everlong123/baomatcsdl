package com.example.demo.repository;

import com.example.demo.model.PrivilegeInfo;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {
    
    @Autowired
    @Qualifier("adminJdbcTemplate")
    private JdbcTemplate adminJdbcTemplate;
    
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate appJdbcTemplate;
    
    
    public User getUserInfo(String username) {
        // Dùng DBA_USERS (yêu cầu quyền SELECT ANY DICTIONARY)
        String dbaSql = """
            SELECT 
                USERNAME,
                ACCOUNT_STATUS,
                LOCK_DATE,
                CREATED,
                DEFAULT_TABLESPACE,
                TEMPORARY_TABLESPACE,
                PROFILE
            FROM DBA_USERS
            WHERE USERNAME = ?
            """;
        
        try {
            List<User> users = adminJdbcTemplate.query(dbaSql, new UserRowMapper(), username.toUpperCase());
            if (users != null && !users.isEmpty()) {
                System.out.println("Found user " + username + " in DBA_USERS");
                return users.get(0);
            } else {
                System.err.println("User " + username + " not found in DBA_USERS (empty result)");
                // Test query để xem có users nào trong DBA_USERS không
                String testSql = "SELECT COUNT(*) as CNT FROM DBA_USERS WHERE USERNAME = ?";
                List<Integer> counts = adminJdbcTemplate.query(testSql, (rs, rowNum) -> rs.getInt("CNT"), username.toUpperCase());
                if (counts != null && !counts.isEmpty()) {
                    System.err.println("Test query returned count: " + counts.get(0) + " for user " + username);
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying DBA_USERS for " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback: Tạo User object cơ bản nếu không tìm thấy trong DBA_USERS
        // (Có thể do user không tồn tại hoặc không có quyền)
        try {
            String currentUserSql = "SELECT USER FROM DUAL";
            List<String> currentUsers = adminJdbcTemplate.query(currentUserSql, (rs, rowNum) -> rs.getString("USER"));
            if (currentUsers != null && !currentUsers.isEmpty()) {
                String currentUser = currentUsers.get(0);
                System.out.println("Current user from DUAL: " + currentUser);
                if (currentUser.equalsIgnoreCase(username)) {
                    // Tạo User object với thông tin cơ bản
                    User user = new User();
                    user.setUsername(currentUser);
                    user.setAccountStatus("OPEN"); // Giả định
                    System.out.println("Created basic user object for " + currentUser);
                    return user;
                } else {
                    System.err.println("Current user (" + currentUser + ") does not match requested user (" + username + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public List<User> getAllUsers() {
        // Chỉ loại bỏ các system users cơ bản, giữ lại tất cả application users
        String sql = """
            SELECT 
                USERNAME,
                ACCOUNT_STATUS,
                LOCK_DATE,
                CREATED,
                DEFAULT_TABLESPACE,
                TEMPORARY_TABLESPACE,
                PROFILE
            FROM DBA_USERS
            WHERE USERNAME NOT IN ('SYS', 'SYSTEM', 'SYSAUX', 'XS$NULL')
               AND USERNAME NOT LIKE 'C##%'
            ORDER BY USERNAME
            """;
        
        try {
            // Test query trước để xem có bao nhiêu users
            String countSql = """
                SELECT COUNT(*) as CNT
                FROM DBA_USERS
                WHERE USERNAME NOT IN ('SYS', 'SYSTEM', 'SYSAUX', 'XS$NULL')
                   AND USERNAME NOT LIKE 'C##%'
                """;
            System.out.println("Executing count query for getAllUsers...");
            List<Integer> counts = adminJdbcTemplate.query(countSql, (rs, rowNum) -> rs.getInt("CNT"));
            int totalCount = counts != null && !counts.isEmpty() ? counts.get(0) : 0;
            System.out.println("Total users in DBA_USERS (after filter): " + totalCount);
            
            // Test query không filter để xem tổng số users
            String totalCountSql = "SELECT COUNT(*) as CNT FROM DBA_USERS";
            List<Integer> totalCounts = adminJdbcTemplate.query(totalCountSql, (rs, rowNum) -> rs.getInt("CNT"));
            int totalAllUsers = totalCounts != null && !totalCounts.isEmpty() ? totalCounts.get(0) : 0;
            System.out.println("Total users in DBA_USERS (no filter): " + totalAllUsers);
            
            // List một số usernames để debug
            String sampleSql = "SELECT USERNAME FROM DBA_USERS WHERE ROWNUM <= 10 ORDER BY USERNAME";
            List<String> sampleUsers = adminJdbcTemplate.queryForList(sampleSql, String.class);
            System.out.println("Sample usernames in DBA_USERS: " + sampleUsers);
            
            System.out.println("Executing main query for getAllUsers...");
            List<User> users = adminJdbcTemplate.query(sql, new UserRowMapper());
            System.out.println("Successfully mapped " + users.size() + " users from DBA_USERS");
            
            if (users.isEmpty() && totalCount > 0) {
                System.err.println("WARNING: Query returned " + totalCount + " rows but mapping resulted in 0 users. Check RowMapper!");
            } else if (users.isEmpty()) {
                System.err.println("WARNING: No users found! Total in DBA_USERS: " + totalAllUsers + ", After filter: " + totalCount);
            }
            
            return users;
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public String getUserQuota(String username, String tablespace) {
        String sql = """
            SELECT 
                CASE 
                    WHEN MAX_BYTES = -1 THEN 'UNLIMITED'
                    ELSE TO_CHAR(MAX_BYTES / 1024 / 1024) || 'M'
                END AS QUOTA
            FROM DBA_TS_QUOTAS
            WHERE USERNAME = ? AND TABLESPACE_NAME = ?
            """;
        
        try {
            List<String> results = adminJdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("QUOTA"), 
                username.toUpperCase(), tablespace);
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            // Ignore - quota có thể không được set
        }
        
        return "0M";
    }
    
    public List<String> getUserRoles(String username) {
        String sql = """
            SELECT GRANTED_ROLE
            FROM DBA_ROLE_PRIVS
            WHERE GRANTEE = ?
            ORDER BY GRANTED_ROLE
            """;
        
        return adminJdbcTemplate.queryForList(sql, String.class, username.toUpperCase());
    }
    
    public List<PrivilegeInfo> getUserPrivileges(String username) {
        List<PrivilegeInfo> privileges = new ArrayList<>();
        
        try {
            // System privileges (direct)
            // Lưu ý: DBA_SYS_PRIVS không có cột GRANTOR
            String sysPrivSql = """
                SELECT 
                    PRIVILEGE,
                    GRANTEE,
                    ADMIN_OPTION
                FROM DBA_SYS_PRIVS
                WHERE GRANTEE = ?
                """;
            
            adminJdbcTemplate.query(sysPrivSql, (rs, rowNum) -> {
                PrivilegeInfo priv = new PrivilegeInfo();
                priv.setPrivilege(rs.getString("PRIVILEGE"));
                priv.setGrantee(rs.getString("GRANTEE"));
                priv.setGrantor(null); // DBA_SYS_PRIVS không có GRANTOR
                priv.setAdminOption("YES".equals(rs.getString("ADMIN_OPTION")));
                priv.setType("DIRECT");
                privileges.add(priv);
                return null;
            }, username.toUpperCase());
            
            // Privileges through roles
            // Lưu ý: DBA_SYS_PRIVS không có cột GRANTOR
            String rolePrivSql = """
                SELECT DISTINCT
                    sp.PRIVILEGE,
                    sp.ADMIN_OPTION,
                    rp.GRANTED_ROLE
                FROM DBA_SYS_PRIVS sp
                INNER JOIN DBA_ROLE_PRIVS rp ON sp.GRANTEE = rp.GRANTED_ROLE
                WHERE rp.GRANTEE = ?
                """;
            
            adminJdbcTemplate.query(rolePrivSql, (rs, rowNum) -> {
                PrivilegeInfo priv = new PrivilegeInfo();
                priv.setPrivilege(rs.getString("PRIVILEGE"));
                priv.setGrantee(username.toUpperCase());
                priv.setGrantor(null); // DBA_SYS_PRIVS không có GRANTOR
                priv.setAdminOption("YES".equals(rs.getString("ADMIN_OPTION")));
                priv.setType("ROLE");
                priv.setRoleName(rs.getString("GRANTED_ROLE"));
                privileges.add(priv);
                return null;
            }, username.toUpperCase());
        } catch (Exception e) {
            // Nếu không có quyền truy cập DBA views, trả về danh sách rỗng
            System.err.println("Error getting privileges for user " + username + ": " + e.getMessage());
        }
        
        return privileges;
    }
    
    public UserProfile getUserProfile(String username) {
        String sql = """
            SELECT USERNAME, FULL_NAME, EMAIL, PHONE, ADDRESS
            FROM APP_OWNER.APP_USER_PROFILE
            WHERE USERNAME = ?
            """;
        
        try {
            // Use appJdbcTemplate - VPD will automatically filter rows based on SESSION_USER
            // Oracle enforces row-level security - no Spring code needed
            List<UserProfile> profiles = appJdbcTemplate.query(sql, (rs, rowNum) -> {
                UserProfile profile = new UserProfile();
                profile.setUsername(rs.getString("USERNAME"));
                profile.setFullName(rs.getString("FULL_NAME"));
                profile.setEmail(rs.getString("EMAIL"));
                profile.setPhone(rs.getString("PHONE"));
                profile.setAddress(rs.getString("ADDRESS"));
                return profile;
            }, username.toUpperCase());
            
            if (profiles != null && !profiles.isEmpty()) {
                return profiles.get(0);
            }
        } catch (Exception e) {
            // Profile có thể không tồn tại - không phải lỗi
        }
        
        return null;
    }
    
    /**
     * Create user using DDL.
     * Oracle enforces CREATE USER privilege - no Spring code checks.
     */
    public void createUser(String username, String password, String defaultTablespace, 
                          String temporaryTablespace, String quota) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE USER ").append(username.toUpperCase());
        sql.append(" IDENTIFIED BY ").append(password);
        
        if (defaultTablespace != null && !defaultTablespace.isEmpty()) {
            sql.append(" DEFAULT TABLESPACE ").append(defaultTablespace);
        }
        
        if (temporaryTablespace != null && !temporaryTablespace.isEmpty()) {
            sql.append(" TEMPORARY TABLESPACE ").append(temporaryTablespace);
        } else {
            sql.append(" TEMPORARY TABLESPACE TEMP");
        }
        
        if (quota != null && !quota.isEmpty()) {
            sql.append(" QUOTA ").append(quota).append(" ON ").append(defaultTablespace);
        }
        
        adminJdbcTemplate.execute(sql.toString());
        
        // Grant CREATE SESSION by default
        adminJdbcTemplate.execute("GRANT CREATE SESSION TO " + username.toUpperCase());
    }
    
    public void alterUser(String username, String password, String defaultTablespace,
                         String temporaryTablespace, String quota, String profile) {
        StringBuilder sql = new StringBuilder("ALTER USER " + username.toUpperCase());
        
        if (password != null && !password.isEmpty()) {
            sql.append(" IDENTIFIED BY ").append(password);
        }
        if (defaultTablespace != null && !defaultTablespace.isEmpty()) {
            sql.append(" DEFAULT TABLESPACE ").append(defaultTablespace);
        }
        if (temporaryTablespace != null && !temporaryTablespace.isEmpty()) {
            sql.append(" TEMPORARY TABLESPACE ").append(temporaryTablespace);
        }
        if (quota != null && !quota.isEmpty()) {
            sql.append(" QUOTA ").append(quota).append(" ON ").append(defaultTablespace);
        }
        if (profile != null && !profile.isEmpty()) {
            sql.append(" PROFILE ").append(profile);
        }
        
        adminJdbcTemplate.execute(sql.toString());
    }
    
    /**
     * Lock user using DDL.
     * Oracle enforces ALTER USER privilege.
     */
    public void lockUser(String username) {
        String sql = "ALTER USER " + username.toUpperCase() + " ACCOUNT LOCK";
        adminJdbcTemplate.execute(sql);
    }
    
    /**
     * Unlock user using DDL.
     * Oracle enforces ALTER USER privilege.
     */
    public void unlockUser(String username) {
        String sql = "ALTER USER " + username.toUpperCase() + " ACCOUNT UNLOCK";
        adminJdbcTemplate.execute(sql);
    }
    
    /**
     * Drop user - Oracle enforces DROP USER privilege.
     */
    public void dropUser(String username) {
        adminJdbcTemplate.execute("DROP USER " + username.toUpperCase() + " CASCADE");
    }
    
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                User user = new User();
                user.setUsername(rs.getString("USERNAME"));
                user.setAccountStatus(rs.getString("ACCOUNT_STATUS"));
                
                java.sql.Timestamp lockDate = rs.getTimestamp("LOCK_DATE");
                if (lockDate != null) {
                    user.setLockDate(lockDate.toLocalDateTime());
                }
                
                java.sql.Timestamp created = rs.getTimestamp("CREATED");
                if (created != null) {
                    user.setCreatedDate(created.toLocalDateTime());
                }
                
                user.setDefaultTablespace(rs.getString("DEFAULT_TABLESPACE"));
                user.setTemporaryTablespace(rs.getString("TEMPORARY_TABLESPACE"));
                user.setProfile(rs.getString("PROFILE"));
                
                return user;
            } catch (SQLException e) {
                System.err.println("Error mapping row " + rowNum + ": " + e.getMessage());
                throw e;
            }
        }
    }
}

