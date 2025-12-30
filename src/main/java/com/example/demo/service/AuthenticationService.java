package com.example.demo.service;

import com.example.demo.model.AppLoginUser;
import com.example.demo.model.User;
import com.example.demo.repository.AppLoginUserRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthenticationService {
    
    @Autowired
    private AppLoginUserRepository appLoginUserRepository;
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Login using application-level authentication with BCrypt password hashing.
     * Oracle username is stored in APP_LOGIN_USER table.
     * 
     * Auto-sync: If user exists in Oracle but not in APP_LOGIN_USER,
     * automatically add to APP_LOGIN_USER with the provided password.
     * Oracle enforces security (VPD, privileges) when user queries data.
     */
    public boolean login(String username, String password, HttpSession session) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }
        
        // Trim username to remove any trailing spaces
        username = username.trim();
        
        // Look up user in APP_LOGIN_USER table
        AppLoginUser appUser = appLoginUserRepository.findByUsername(username);
        
        // Auto-sync: If user not in APP_LOGIN_USER but exists in Oracle, add it
        if (appUser == null) {
            System.out.println("User not found in APP_LOGIN_USER: " + username + ". Checking Oracle...");
            
            // Check if user exists in Oracle
            try {
                User oracleUser = userRepository.getUserInfo(username);
                if (oracleUser != null) {
                    System.out.println("User " + username + " exists in Oracle. Auto-adding to APP_LOGIN_USER...");
                    
                    // Hash password and add to APP_LOGIN_USER
                    String passwordHash = passwordEncoder.encode(password);
                    AppLoginUser newAppUser = new AppLoginUser(username.toUpperCase(), passwordHash);
                    appLoginUserRepository.save(newAppUser);
                    
                    System.out.println("Successfully auto-added " + username + " to APP_LOGIN_USER");
                    appUser = newAppUser;
                } else {
                    System.err.println("User " + username + " not found in Oracle either");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error checking Oracle for user " + username + ": " + e.getMessage());
                return false;
            }
        }
        
        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, appUser.getPasswordHash())) {
            System.err.println("Password mismatch for user: " + username);
            return false;
        }
        
        // Login successful - store Oracle username in session
        // Oracle will enforce security (VPD, privileges) when user queries data
        // IMPORTANT: Trim to remove any trailing spaces!
        session.setAttribute("username", username.trim().toUpperCase());
        
        return true;
    }
    
    public void logout(HttpSession session) {
        session.invalidate();
    }
    
    public String getCurrentUser(HttpSession session) {
        return (String) session.getAttribute("username");
    }
    
    /**
     * Check if user is logged in.
     * Oracle decides permissions - we don't hardcode admin checks here.
     */
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("username") != null;
    }
    
    /**
     * Hash password using BCrypt.
     * Used when creating new users in admin UI.
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Check if user has admin capabilities by checking Oracle privileges.
     * Oracle decides - no hardcoding. Checks for common admin privileges.
     * This is for UI display purposes only - actual operations are enforced by Oracle.
     */
    public boolean hasAdminCapabilities(String username, PrivilegeService privilegeService) {
        if (username == null || privilegeService == null) {
            return false;
        }
        // Trim username to remove any trailing spaces
        username = username.trim();
        
        // Check for common admin privileges - Oracle decides
        return privilegeService.hasPrivilege(username, "CREATE USER") ||
               privilegeService.hasPrivilege(username, "ALTER USER") ||
               privilegeService.hasPrivilege(username, "CREATE ROLE") ||
               privilegeService.hasPrivilege(username, "CREATE PROFILE");
    }
}

