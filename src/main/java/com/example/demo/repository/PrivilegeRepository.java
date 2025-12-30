package com.example.demo.repository;

import com.example.demo.model.PrivilegeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PrivilegeRepository {
    
    @Autowired
    @Qualifier("adminJdbcTemplate")
    private JdbcTemplate adminJdbcTemplate;
    
    public List<PrivilegeInfo> getAllPrivileges() {
        List<PrivilegeInfo> privileges = new ArrayList<>();
        
        try {
            // Lấy system privileges trực tiếp
            // Lưu ý: DBA_SYS_PRIVS không có cột GRANTOR
            String sysPrivSql = """
                SELECT 
                    PRIVILEGE,
                    GRANTEE,
                    ADMIN_OPTION
                FROM DBA_SYS_PRIVS
                WHERE GRANTEE NOT IN ('SYS', 'SYSTEM')
                ORDER BY GRANTEE, PRIVILEGE
                """;
            
            List<PrivilegeInfo> sysPrivs = adminJdbcTemplate.query(sysPrivSql, (rs, rowNum) -> {
                PrivilegeInfo priv = new PrivilegeInfo();
                priv.setPrivilege(rs.getString("PRIVILEGE"));
                priv.setGrantee(rs.getString("GRANTEE"));
                priv.setGrantor(null); // DBA_SYS_PRIVS không có GRANTOR
                priv.setAdminOption("YES".equals(rs.getString("ADMIN_OPTION")));
                priv.setType("DIRECT");
                return priv;
            });
            
            if (sysPrivs != null) {
                privileges.addAll(sysPrivs);
            }
            
            // Lấy object privileges
            String objPrivSql = """
                SELECT 
                    PRIVILEGE,
                    GRANTEE,
                    GRANTOR,
                    TABLE_NAME as OBJECT_NAME,
                    GRANTABLE
                FROM DBA_TAB_PRIVS
                WHERE GRANTEE NOT IN ('SYS', 'SYSTEM')
                ORDER BY GRANTEE, PRIVILEGE
                """;
            
            List<PrivilegeInfo> objPrivs = adminJdbcTemplate.query(objPrivSql, (rs, rowNum) -> {
                PrivilegeInfo priv = new PrivilegeInfo();
                priv.setPrivilege(rs.getString("PRIVILEGE"));
                priv.setGrantee(rs.getString("GRANTEE"));
                priv.setGrantor(rs.getString("GRANTOR"));
                priv.setObjectName(rs.getString("OBJECT_NAME"));
                priv.setAdminOption("YES".equals(rs.getString("GRANTABLE")));
                priv.setType("OBJECT");
                return priv;
            });
            
            if (objPrivs != null) {
                privileges.addAll(objPrivs);
            }
            
            System.out.println("Total privileges retrieved: " + privileges.size());
        } catch (Exception e) {
            // Nếu không có quyền truy cập DBA views, trả về danh sách rỗng
            System.err.println("Error getting all privileges: " + e.getMessage());
            e.printStackTrace();
        }
        
        return privileges;
    }
    
    public boolean hasPrivilege(String username, String privilege) {
        try {
            System.out.println("DEBUG: Checking privilege " + privilege + " for user " + username);
            
            // Kiểm tra quyền trực tiếp - sử dụng DBA_SYS_PRIVS với GRANTEE
            String directSql = """
                SELECT COUNT(*) as CNT
                FROM DBA_SYS_PRIVS
                WHERE GRANTEE = ?
                AND PRIVILEGE = ?
                """;
            
            System.out.println("DEBUG: Running direct query for user: " + username);
            List<Integer> results = adminJdbcTemplate.query(directSql, (rs, rowNum) -> rs.getInt("CNT"), 
                username.toUpperCase(), privilege);
            
            System.out.println("DEBUG: Direct query result: " + (results != null && !results.isEmpty() ? results.get(0) : "null"));
            
            if (results != null && !results.isEmpty() && results.get(0) > 0) {
                System.out.println("DEBUG: User " + username + " HAS privilege " + privilege + " (direct)");
                return true;
            }
            
            // Kiểm tra quyền thông qua role - dùng DBA views
            String roleSql = """
                SELECT COUNT(*) as CNT
                FROM DBA_SYS_PRIVS
                WHERE GRANTEE IN (
                    SELECT GRANTED_ROLE
                    FROM DBA_ROLE_PRIVS
                    WHERE GRANTEE = ?
                )
                AND PRIVILEGE = ?
                """;
            
            System.out.println("DEBUG: Running role query for user: " + username);
            results = adminJdbcTemplate.query(roleSql, (rs, rowNum) -> rs.getInt("CNT"), 
                username.toUpperCase(), privilege);
            
            System.out.println("DEBUG: Role query result: " + (results != null && !results.isEmpty() ? results.get(0) : "null"));
            
            boolean hasPriv = results != null && !results.isEmpty() && results.get(0) > 0;
            System.out.println("DEBUG: User " + username + " " + (hasPriv ? "HAS" : "DOES NOT HAVE") + " privilege " + privilege);
            
            return hasPriv;
        } catch (Exception e) {
            System.err.println("ERROR checking privilege for " + username + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void grantSystemPrivilege(String privilege, String grantee, boolean withAdminOption) {
        String sql = "GRANT " + privilege + " TO " + grantee.toUpperCase();
        if (withAdminOption) {
            sql += " WITH ADMIN OPTION";
        }
        adminJdbcTemplate.execute(sql);
    }
    
    public void revokeSystemPrivilege(String privilege, String grantee) {
        adminJdbcTemplate.execute("REVOKE " + privilege + " FROM " + grantee.toUpperCase());
    }
    
    public void grantRole(String role, String grantee, boolean withAdminOption) {
        String sql = "GRANT " + role.toUpperCase() + " TO " + grantee.toUpperCase();
        if (withAdminOption) {
            sql += " WITH ADMIN OPTION";
        }
        adminJdbcTemplate.execute(sql);
    }
    
    public void revokeRole(String role, String grantee) {
        adminJdbcTemplate.execute("REVOKE " + role.toUpperCase() + " FROM " + grantee.toUpperCase());
    }
    
    public void grantObjectPrivilege(String privilege, String table, String grantee, boolean withGrantOption) {
        String sql = "GRANT " + privilege + " ON " + table + " TO " + grantee.toUpperCase();
        if (withGrantOption) {
            sql += " WITH GRANT OPTION";
        }
        adminJdbcTemplate.execute(sql);
    }
    
    public void revokeObjectPrivilege(String privilege, String table, String grantee) {
        adminJdbcTemplate.execute("REVOKE " + privilege + " ON " + table + " FROM " + grantee.toUpperCase());
    }
    
    public void grantColumnPrivilege(String privilege, String table, String column, String grantee) {
        adminJdbcTemplate.execute("GRANT " + privilege + " (" + column + ") ON " + table + " TO " + grantee.toUpperCase());
    }
    
    public List<String> getAvailableTablespaces() {
        try {
            String sql = """
                SELECT TABLESPACE_NAME
                FROM DBA_TABLESPACES
                WHERE TABLESPACE_NAME NOT IN ('SYSTEM', 'SYSAUX', 'TEMP', 'UNDOTBS1')
                ORDER BY TABLESPACE_NAME
                """;
            
            return adminJdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            System.err.println("Error getting tablespaces: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

