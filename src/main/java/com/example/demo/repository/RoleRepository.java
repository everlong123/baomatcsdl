package com.example.demo.repository;

import com.example.demo.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RoleRepository {
    
    @Autowired
    @Qualifier("adminJdbcTemplate")
    private JdbcTemplate adminJdbcTemplate;
    
    public List<Role> getAllRoles() {
        String sql = """
            SELECT ROLE
            FROM DBA_ROLES
            WHERE ROLE NOT IN ('CONNECT', 'RESOURCE', 'DBA', 'SELECT_CATALOG_ROLE')
            ORDER BY ROLE
            """;
        
        List<String> roleNames = adminJdbcTemplate.queryForList(sql, String.class);
        List<Role> roles = new ArrayList<>();
        
        for (String roleName : roleNames) {
            roles.add(getRole(roleName));
        }
        
        return roles;
    }
    
    public Role getRole(String roleName) {
        // Kiểm tra role có password không
        String checkPasswordSql = """
            SELECT PASSWORD_REQUIRED
            FROM DBA_ROLES
            WHERE ROLE = ?
            """;
        
        boolean hasPassword = false;
        try {
            List<String> results = adminJdbcTemplate.query(checkPasswordSql, (rs, rowNum) -> rs.getString("PASSWORD_REQUIRED"), roleName);
            if (results != null && !results.isEmpty()) {
                hasPassword = "YES".equals(results.get(0));
            }
        } catch (Exception e) {
            System.err.println("Error checking role password for " + roleName + ": " + e.getMessage());
        }
        
        Role role = new Role();
        role.setRoleName(roleName);
        role.setHasPassword(hasPassword);
        role.setPrivileges(getRolePrivileges(roleName));
        role.setAssignedUsers(getUsersWithRole(roleName));
        
        return role;
    }
    
    public List<String> getRolePrivileges(String roleName) {
        String sql = """
            SELECT PRIVILEGE
            FROM DBA_SYS_PRIVS
            WHERE GRANTEE = ?
            ORDER BY PRIVILEGE
            """;
        
        return adminJdbcTemplate.queryForList(sql, String.class, roleName);
    }
    
    public List<String> getUsersWithRole(String roleName) {
        String sql = """
            SELECT GRANTEE
            FROM DBA_ROLE_PRIVS
            WHERE GRANTED_ROLE = ?
            ORDER BY GRANTEE
            """;
        
        return adminJdbcTemplate.queryForList(sql, String.class, roleName);
    }
    
    public void createRole(String roleName, String password) {
        if (password != null && !password.isEmpty()) {
            adminJdbcTemplate.execute("CREATE ROLE " + roleName + " IDENTIFIED BY " + password);
        } else {
            adminJdbcTemplate.execute("CREATE ROLE " + roleName);
        }
    }
    
    public void alterRolePassword(String roleName, String password) {
        adminJdbcTemplate.execute("ALTER ROLE " + roleName + " IDENTIFIED BY " + password);
    }
    
    public void dropRole(String roleName) {
        adminJdbcTemplate.execute("DROP ROLE " + roleName);
    }
}

