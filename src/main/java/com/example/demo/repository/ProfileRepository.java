package com.example.demo.repository;

import com.example.demo.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProfileRepository {
    
    @Autowired
    @Qualifier("adminJdbcTemplate")
    private JdbcTemplate adminJdbcTemplate;
    
    public List<Profile> getAllProfiles() {
        String sql = """
            SELECT PROFILE
            FROM DBA_PROFILES
            WHERE RESOURCE_NAME IN ('SESSIONS_PER_USER', 'CONNECT_TIME', 'IDLE_TIME')
            GROUP BY PROFILE
            ORDER BY PROFILE
            """;
        
        List<String> profileNames = adminJdbcTemplate.queryForList(sql, String.class);
        List<Profile> profiles = new ArrayList<>();
        
        for (String profileName : profileNames) {
            profiles.add(getProfile(profileName));
        }
        
        return profiles;
    }
    
    public Profile getProfile(String profileName) {
        String sql = """
            SELECT RESOURCE_NAME, RESOURCE_TYPE, LIMIT
            FROM DBA_PROFILES
            WHERE PROFILE = ?
            AND RESOURCE_NAME IN ('SESSIONS_PER_USER', 'CONNECT_TIME', 'IDLE_TIME')
            """;
        
        Profile profile = new Profile();
        profile.setProfileName(profileName);
        profile.setAssignedUsers(getUsersWithProfile(profileName));
        
        adminJdbcTemplate.query(sql, (rs) -> {
            String resourceName = rs.getString("RESOURCE_NAME");
            String limit = rs.getString("LIMIT");
            
            switch (resourceName) {
                case "SESSIONS_PER_USER" -> profile.setSessionsPerUser(limit);
                case "CONNECT_TIME" -> profile.setConnectTime(limit);
                case "IDLE_TIME" -> profile.setIdleTime(limit);
            }
        }, profileName);
        
        return profile;
    }
    
    public List<String> getUsersWithProfile(String profileName) {
        String sql = """
            SELECT USERNAME
            FROM DBA_USERS
            WHERE PROFILE = ?
            ORDER BY USERNAME
            """;
        
        return adminJdbcTemplate.queryForList(sql, String.class, profileName);
    }
    
    public void createProfile(String profileName, String sessionsPerUser, 
                             String connectTime, String idleTime) {
        StringBuilder sql = new StringBuilder("CREATE PROFILE " + profileName + " LIMIT ");
        
        if (sessionsPerUser != null && !sessionsPerUser.isEmpty()) {
            sql.append("SESSIONS_PER_USER ").append(sessionsPerUser).append(" ");
        }
        if (connectTime != null && !connectTime.isEmpty()) {
            sql.append("CONNECT_TIME ").append(connectTime).append(" ");
        }
        if (idleTime != null && !idleTime.isEmpty()) {
            sql.append("IDLE_TIME ").append(idleTime).append(" ");
        }
        
        adminJdbcTemplate.execute(sql.toString());
    }
    
    public void alterProfile(String profileName, String sessionsPerUser,
                            String connectTime, String idleTime) {
        StringBuilder sql = new StringBuilder("ALTER PROFILE " + profileName + " LIMIT ");
        
        if (sessionsPerUser != null && !sessionsPerUser.isEmpty()) {
            sql.append("SESSIONS_PER_USER ").append(sessionsPerUser).append(" ");
        }
        if (connectTime != null && !connectTime.isEmpty()) {
            sql.append("CONNECT_TIME ").append(connectTime).append(" ");
        }
        if (idleTime != null && !idleTime.isEmpty()) {
            sql.append("IDLE_TIME ").append(idleTime).append(" ");
        }
        
        adminJdbcTemplate.execute(sql.toString());
    }
    
    public void dropProfile(String profileName) {
        adminJdbcTemplate.execute("DROP PROFILE " + profileName + " CASCADE");
    }
}

