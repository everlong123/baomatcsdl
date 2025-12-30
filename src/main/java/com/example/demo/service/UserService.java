package com.example.demo.service;

import com.example.demo.model.AppLoginUser;
import com.example.demo.model.PrivilegeInfo;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.AppLoginUserRepository;
import com.example.demo.repository.PrivilegeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PrivilegeRepository privilegeRepository;
    
    @Autowired
    private AppLoginUserRepository appLoginUserRepository;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    /**
     * Get all users visible to current user.
     * Oracle decides what users are visible based on privileges:
     * - Users with SELECT ANY DICTIONARY can see all users (DBA_USERS)
     * - Regular users can only see their own info (USER_USERS)
     * Oracle enforces security - no hardcoded checks.
     */
    public List<User> getAllUsers(String currentUser) {
        List<User> users = new ArrayList<>();
        
        try {
            // Try to get all users - Oracle will return only what user has permission to see
            users = userRepository.getAllUsers();
            System.out.println("User " + currentUser + " - Retrieved " + users.size() + " users (Oracle enforced)");
        } catch (Exception e) {
            // If user doesn't have permission to see all users, try to get own info
            System.err.println("Cannot get all users (Oracle permission denied), trying own info: " + e.getMessage());
            try {
                User user = userRepository.getUserInfo(currentUser);
                if (user != null) {
                    users.add(user);
                    System.out.println("User " + currentUser + " - Retrieved own info only");
                }
            } catch (Exception e2) {
                System.err.println("Error getting own user info: " + e2.getMessage());
            }
        }
        
        for (User user : users) {
            if (user != null) {
                try {
                    populateUserDetails(user);
                } catch (Exception e) {
                    System.err.println("Error populating details for user " + user.getUsername() + ": " + e.getMessage());
                }
            }
        }
        
        return users;
    }
    
    /**
     * Get user info - Oracle enforces visibility.
     * If user doesn't have permission, Oracle will throw exception or return null.
     */
    public User getUser(String username, String currentUser) {
        User user = userRepository.getUserInfo(username);
        
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        
        // Oracle has already enforced visibility - if we got here, user has permission
        populateUserDetails(user);
        return user;
    }
    
    private void populateUserDetails(User user) {
        try {
            // Lấy quota
            if (user.getDefaultTablespace() != null) {
                try {
                    String quota = userRepository.getUserQuota(user.getUsername(), user.getDefaultTablespace());
                    user.setQuota(quota);
                } catch (Exception e) {
                    user.setQuota("N/A");
                }
            }
            
            // Lấy roles
            try {
                List<String> roles = userRepository.getUserRoles(user.getUsername());
                user.setRoles(roles);
            } catch (Exception e) {
                user.setRoles(new ArrayList<>());
            }
            
            // Lấy privileges
            try {
                List<PrivilegeInfo> privileges = userRepository.getUserPrivileges(user.getUsername());
                user.setPrivileges(privileges);
            } catch (Exception e) {
                user.setPrivileges(new ArrayList<>());
            }
            
            // Lấy thông tin profile
            try {
                UserProfile profile = userRepository.getUserProfile(user.getUsername());
                if (profile != null) {
                    user.setFullName(profile.getFullName());
                    user.setEmail(profile.getEmail());
                    user.setPhone(profile.getPhone());
                    user.setAddress(profile.getAddress());
                }
            } catch (Exception e) {
                // Ignore - profile có thể không tồn tại
            }
        } catch (Exception e) {
            // Log error nhưng không throw để không làm crash ứng dụng
            System.err.println("Error populating user details for " + user.getUsername() + ": " + e.getMessage());
        }
    }
    
    /**
     * Create user with BCrypt password hashing.
     * Flow:
     * 1. Check if Oracle user already exists
     * 2. Hash password with BCrypt
     * 3. Insert/Update APP_LOGIN_USER (for application login)
     * 4. Call PKG_USER_ADMIN.CREATE_USER (creates Oracle user with original password)
     * Oracle enforces CREATE USER privilege.
     */
    public void createUser(String username, String password, String defaultTablespace,
                          String temporaryTablespace, String quota) {
        String usernameUpper = username.toUpperCase();
        
        // Check if Oracle user already exists
        User existingOracleUser = null;
        try {
            existingOracleUser = userRepository.getUserInfo(usernameUpper);
        } catch (Exception e) {
            // User not found - OK to create
        }
        
        if (existingOracleUser != null) {
            throw new RuntimeException("ORA-01920: user '" + usernameUpper + "' already exists in Oracle");
        }
        
        // Hash password with BCrypt for application login
        String passwordHash = authenticationService.encodePassword(password);
        
        // Check if user already exists in APP_LOGIN_USER
        AppLoginUser existingAppUser = appLoginUserRepository.findByUsername(usernameUpper);
        
        if (existingAppUser == null) {
            // User doesn't exist - insert new
            AppLoginUser appLoginUser = new AppLoginUser(usernameUpper, passwordHash);
            appLoginUserRepository.save(appLoginUser);
            System.out.println("Added " + usernameUpper + " to APP_LOGIN_USER");
        } else {
            // User already exists - update password
            appLoginUserRepository.updatePassword(usernameUpper, passwordHash);
            System.out.println("Updated password for " + usernameUpper + " in APP_LOGIN_USER (already existed)");
        }
        
        // Create Oracle user using PL/SQL package (uses original password)
        // Oracle enforces CREATE USER privilege - no Spring code checks
        userRepository.createUser(username, password, defaultTablespace, temporaryTablespace, quota);
    }
    
    public void updateUser(String username, String password, String defaultTablespace,
                          String temporaryTablespace, String quota, String profile) {
        userRepository.alterUser(username, password, defaultTablespace, temporaryTablespace, quota, profile);
    }
    
    public void lockUser(String username) {
        userRepository.lockUser(username);
    }
    
    public void unlockUser(String username) {
        userRepository.unlockUser(username);
    }
    
    /**
     * Delete user - also remove from APP_LOGIN_USER.
     * Oracle enforces DROP USER privilege.
     */
    public void deleteUser(String username) {
        // Delete from APP_LOGIN_USER
        try {
            appLoginUserRepository.delete(username);
        } catch (Exception e) {
            System.err.println("Error deleting from APP_LOGIN_USER: " + e.getMessage());
        }
        
        // Drop Oracle user - Oracle enforces privilege
        userRepository.dropUser(username);
    }
    
    public List<String> getUserRoles(String username) {
        try {
            return userRepository.getUserRoles(username);
        } catch (Exception e) {
            System.err.println("Error getting roles for user " + username + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Removed isAdmin() - Oracle decides permissions, not Spring code
}

