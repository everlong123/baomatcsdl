package com.example.demo.controller;

import com.example.demo.service.AuthenticationService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.PrivilegeService;
import com.example.demo.service.RoleService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String username = authenticationService.getCurrentUser(session);
        // Check Oracle privileges - no hardcoding
        boolean isAdmin = authenticationService.hasAdminCapabilities(username, privilegeService);
        
        // Get current user's roles from database
        java.util.List<String> userRoles = new java.util.ArrayList<>();
        try {
            userRoles = userService.getUserRoles(username);
            System.out.println("User " + username + " has " + userRoles.size() + " roles: " + userRoles);
        } catch (Exception e) {
            System.err.println("Error getting user roles: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Nếu là admin và không có roles, hiển thị privileges thay vì roles
        java.util.List<String> allSystemRoles = new java.util.ArrayList<>();
        java.util.List<String> adminPrivileges = new java.util.ArrayList<>();
        if (isAdmin && (userRoles == null || userRoles.isEmpty())) {
            try {
                // Lấy privileges của admin (vì admin thường có quyền trực tiếp, không qua role)
                java.util.List<com.example.demo.model.PrivilegeInfo> privileges = userService.getUser(username, username).getPrivileges();
                if (privileges != null && !privileges.isEmpty()) {
                    adminPrivileges = privileges.stream()
                        .map(priv -> priv.getPrivilege())
                        .limit(10) // Chỉ hiển thị 10 quyền đầu tiên
                        .collect(java.util.stream.Collectors.toList());
                    System.out.println("Admin has " + privileges.size() + " direct privileges (showing first 10): " + adminPrivileges);
                }
                
                // Cũng hiển thị tất cả roles có sẵn trong hệ thống
                java.util.List<com.example.demo.model.Role> roles = roleService.getAllRoles();
                System.out.println("Retrieved " + roles.size() + " roles from database");
                allSystemRoles = roles.stream()
                    .map(role -> role.getRoleName())
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Admin has no assigned roles, showing all system roles: " + allSystemRoles);
            } catch (Exception e) {
                System.err.println("Error getting admin privileges/roles: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Get statistics with error handling
        int totalUsers = 0;
        int totalProfiles = 0;
        int totalRoles = 0;
        int totalPrivileges = 0;
        
        try {
            totalUsers = userService.getAllUsers(username).size();
        } catch (Exception e) {
            System.err.println("Error getting users: " + e.getMessage());
        }
        
        try {
            totalProfiles = profileService.getAllProfiles().size();
        } catch (Exception e) {
            System.err.println("Error getting profiles: " + e.getMessage());
        }
        
        try {
            totalRoles = roleService.getAllRoles().size();
        } catch (Exception e) {
            System.err.println("Error getting roles: " + e.getMessage());
        }
        
        try {
            totalPrivileges = privilegeService.getAllPrivileges().size();
        } catch (Exception e) {
            System.err.println("Error getting privileges: " + e.getMessage());
        }
        
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("userRoles", userRoles);
        model.addAttribute("allSystemRoles", allSystemRoles);
        model.addAttribute("adminPrivileges", adminPrivileges);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProfiles", totalProfiles);
        model.addAttribute("totalRoles", totalRoles);
        model.addAttribute("totalPrivileges", totalPrivileges);
        
        return "dashboard";
    }
    
    @GetMapping("/debug")
    public String debug(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String username = authenticationService.getCurrentUser(session);
        boolean isAdmin = authenticationService.hasAdminCapabilities(username, privilegeService);
        
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("hasCreateUser", privilegeService.hasPrivilege(username, "CREATE USER"));
        model.addAttribute("hasAlterUser", privilegeService.hasPrivilege(username, "ALTER USER"));
        model.addAttribute("hasCreateRole", privilegeService.hasPrivilege(username, "CREATE ROLE"));
        model.addAttribute("hasCreateProfile", privilegeService.hasPrivilege(username, "CREATE PROFILE"));
        
        return "debug";
    }
}

