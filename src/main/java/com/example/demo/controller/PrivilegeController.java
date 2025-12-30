package com.example.demo.controller;

import com.example.demo.model.PrivilegeInfo;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.PrivilegeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/privileges")
public class PrivilegeController {
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @GetMapping
    public String listPrivileges(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        List<PrivilegeInfo> privileges = privilegeService.getAllPrivileges();
        
        model.addAttribute("privileges", privileges);
        model.addAttribute("username", currentUser);
        model.addAttribute("isAdmin", authenticationService.hasAdminCapabilities(currentUser, privilegeService));
        
        return "privileges/list";
    }
    
    @GetMapping("/grant")
    public String grantPrivilegeForm(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - Oracle enforces GRANT ANY PRIVILEGE or GRANT ANY ROLE
        // For UI, we check if user can grant privileges
        if (!privilegeService.hasPrivilege(currentUser, "GRANT ANY PRIVILEGE") && 
            !privilegeService.hasPrivilege(currentUser, "GRANT ANY ROLE")) {
            return "redirect:/privileges?error=no_privilege";
        }
        
        model.addAttribute("privilegeTypes", List.of("SYSTEM", "ROLE", "OBJECT", "COLUMN"));
        model.addAttribute("systemPrivileges", List.of(
            "CREATE PROFILE", "ALTER PROFILE", "DROP PROFILE",
            "CREATE ROLE", "ALTER ANY ROLE", "DROP ANY ROLE", "GRANT ANY ROLE",
            "CREATE SESSION",
            "CREATE ANY TABLE", "ALTER ANY TABLE", "DROP ANY TABLE", "SELECT ANY TABLE",
            "DELETE ANY TABLE", "INSERT ANY TABLE", "UPDATE ANY TABLE",
            "CREATE TABLE",
            "CREATE USER", "ALTER USER", "DROP USER"
        ));
        
        return "privileges/grant";
    }
    
    @PostMapping("/grant")
    public String grantPrivilege(@RequestParam String privilegeType,
                                 @RequestParam String privilege,
                                 @RequestParam String grantee,
                                 @RequestParam(required = false) String table,
                                 @RequestParam(required = false) String column,
                                 @RequestParam(defaultValue = "false") boolean withOption,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - Oracle enforces GRANT ANY PRIVILEGE or GRANT ANY ROLE
        // For UI, we check if user can grant privileges
        if (!privilegeService.hasPrivilege(currentUser, "GRANT ANY PRIVILEGE") && 
            !privilegeService.hasPrivilege(currentUser, "GRANT ANY ROLE")) {
            return "redirect:/privileges?error=no_privilege";
        }
        
        try {
            switch (privilegeType) {
                case "SYSTEM" -> {
                    privilegeService.grantSystemPrivilege(privilege, grantee, withOption);
                    redirectAttributes.addFlashAttribute("success", "System privilege granted successfully");
                }
                case "ROLE" -> {
                    privilegeService.grantRole(privilege, grantee, withOption);
                    redirectAttributes.addFlashAttribute("success", "Role granted successfully");
                }
                case "OBJECT" -> {
                    if (table == null || table.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Table name is required");
                        return "redirect:/privileges/grant";
                    }
                    privilegeService.grantObjectPrivilege(privilege, table, grantee, withOption);
                    redirectAttributes.addFlashAttribute("success", "Object privilege granted successfully");
                }
                case "COLUMN" -> {
                    if (table == null || table.isEmpty() || column == null || column.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Table and column names are required");
                        return "redirect:/privileges/grant";
                    }
                    privilegeService.grantColumnPrivilege(privilege, table, column, grantee);
                    redirectAttributes.addFlashAttribute("success", "Column privilege granted successfully");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to grant privilege: " + e.getMessage());
        }
        
        return "redirect:/privileges";
    }
    
    @PostMapping("/revoke")
    public String revokePrivilege(@RequestParam String privilegeType,
                                 @RequestParam String privilege,
                                 @RequestParam String grantee,
                                 @RequestParam(required = false) String table,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - Oracle enforces GRANT ANY PRIVILEGE or GRANT ANY ROLE
        // For UI, we check if user can grant privileges
        if (!privilegeService.hasPrivilege(currentUser, "GRANT ANY PRIVILEGE") && 
            !privilegeService.hasPrivilege(currentUser, "GRANT ANY ROLE")) {
            return "redirect:/privileges?error=no_privilege";
        }
        
        try {
            switch (privilegeType) {
                case "SYSTEM" -> {
                    privilegeService.revokeSystemPrivilege(privilege, grantee);
                    redirectAttributes.addFlashAttribute("success", "System privilege revoked successfully");
                }
                case "ROLE" -> {
                    privilegeService.revokeRole(privilege, grantee);
                    redirectAttributes.addFlashAttribute("success", "Role revoked successfully");
                }
                case "OBJECT" -> {
                    if (table == null || table.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Table name is required");
                        return "redirect:/privileges";
                    }
                    privilegeService.revokeObjectPrivilege(privilege, table, grantee);
                    redirectAttributes.addFlashAttribute("success", "Object privilege revoked successfully");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to revoke privilege: " + e.getMessage());
        }
        
        return "redirect:/privileges";
    }
}

