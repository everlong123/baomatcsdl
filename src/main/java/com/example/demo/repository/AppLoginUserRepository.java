package com.example.demo.repository;

import com.example.demo.model.AppLoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class AppLoginUserRepository {
    
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate appJdbcTemplate;
    
    public AppLoginUser findByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        
        // Normalize username to uppercase (Oracle usernames are case-insensitive but stored uppercase)
        String normalizedUsername = username.toUpperCase().trim();
        
        String sql = """
            SELECT USERNAME, PASSWORD_HASH
            FROM APP_OWNER.APP_LOGIN_USER
            WHERE USERNAME = ?
            """;
        
        try {
            List<AppLoginUser> users = appJdbcTemplate.query(sql, new AppLoginUserRowMapper(), normalizedUsername);
            if (users != null && !users.isEmpty()) {
                System.out.println("Found user in APP_LOGIN_USER: " + normalizedUsername);
                return users.get(0);
            } else {
                System.err.println("User not found in APP_LOGIN_USER: " + normalizedUsername);
                // Debug: Check what users exist
                try {
                    List<String> allUsers = appJdbcTemplate.queryForList(
                        "SELECT USERNAME FROM APP_OWNER.APP_LOGIN_USER", String.class);
                    System.err.println("Available users in APP_LOGIN_USER: " + allUsers);
                    System.err.println("Searching for: '" + normalizedUsername + "' (length: " + normalizedUsername.length() + ")");
                    // Check if there's any match with different case
                    for (String u : allUsers) {
                        System.err.println("  Comparing: '" + u + "' (length: " + (u != null ? u.length() : 0) + ")");
                        if (u != null && u.equalsIgnoreCase(normalizedUsername)) {
                            System.err.println("  -> Case-insensitive match found! Using: " + u);
                            // Retry with exact case from database
                            return appJdbcTemplate.query(sql, new AppLoginUserRowMapper(), u).get(0);
                        }
                    }
                } catch (Exception debugE) {
                    System.err.println("Error checking available users: " + debugE.getMessage());
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error querying APP_LOGIN_USER for " + normalizedUsername + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public void save(AppLoginUser appLoginUser) {
        String sql = """
            INSERT INTO APP_OWNER.APP_LOGIN_USER (USERNAME, PASSWORD_HASH)
            VALUES (?, ?)
            """;
        
        try {
            appJdbcTemplate.update(sql, 
                appLoginUser.getUsername().toUpperCase(), 
                appLoginUser.getPasswordHash());
            System.out.println("Successfully inserted user: " + appLoginUser.getUsername());
        } catch (Exception e) {
            System.err.println("Error inserting user " + appLoginUser.getUsername() + ": " + e.getMessage());
            throw e;
        }
    }
    
    public void updatePassword(String username, String passwordHash) {
        String sql = """
            UPDATE APP_OWNER.APP_LOGIN_USER
            SET PASSWORD_HASH = ?
            WHERE USERNAME = ?
            """;
        
        appJdbcTemplate.update(sql, passwordHash, username.toUpperCase());
    }
    
    public void delete(String username) {
        String sql = """
            DELETE FROM APP_OWNER.APP_LOGIN_USER
            WHERE USERNAME = ?
            """;
        
        appJdbcTemplate.update(sql, username.toUpperCase());
    }
    
    private static class AppLoginUserRowMapper implements RowMapper<AppLoginUser> {
        @Override
        public AppLoginUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            AppLoginUser user = new AppLoginUser();
            user.setUsername(rs.getString("USERNAME"));
            user.setPasswordHash(rs.getString("PASSWORD_HASH"));
            return user;
        }
    }
}

