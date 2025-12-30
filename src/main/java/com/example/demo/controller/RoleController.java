package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.PrivilegeService;
import com.example.demo.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/roles")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @GetMapping
    public String listRoles(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        List<Role> roles = roleService.getAllRoles();
        
        model.addAttribute("roles", roles);
        model.addAttribute("username", currentUser);
        model.addAttribute("isAdmin", authenticationService.hasAdminCapabilities(currentUser, privilegeService));
        
        return "roles/list";
    }
    
    @GetMapping("/create")
    public String createRoleForm(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE ROLE")) {
            return "redirect:/roles?error=no_privilege";
        }
        
        model.addAttribute("role", new Role());
        return "roles/create";
    }
    
    @PostMapping("/create")
    public String createRole(@ModelAttribute Role role, HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE ROLE")) {
            return "redirect:/roles?error=no_privilege";
        }
        
        try {
            roleService.createRole(role.getRoleName(), role.getPassword());
            redirectAttributes.addFlashAttribute("success", "Role created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create role: " + e.getMessage());
        }
        
        return "redirect:/roles";
    }
    
    @GetMapping("/{roleName}/edit")
    public String editRoleForm(@PathVariable String roleName, HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "ALTER ANY ROLE")) {
            return "redirect:/roles?error=no_privilege";
        }
        
        Role role = roleService.getRole(roleName);
        model.addAttribute("role", role);
        return "roles/edit";
    }
    
    @PostMapping("/{roleName}/edit")
    public String updateRolePassword(@PathVariable String roleName, @ModelAttribute Role role,
                                    HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "ALTER ANY ROLE")) {
            return "redirect:/roles?error=no_privilege";
        }
        
        try {
            if (role.getPassword() != null && !role.getPassword().isEmpty()) {
                roleService.updateRolePassword(roleName, role.getPassword());
                redirectAttributes.addFlashAttribute("success", "Role password updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update role: " + e.getMessage());
        }
        
        return "redirect:/roles";
    }
    
    @PostMapping("/{roleName}/delete")
    public String deleteRole(@PathVariable String roleName, HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "DROP ANY ROLE")) {
            return "redirect:/roles?error=no_privilege";
        }
        
        try {
            roleService.deleteRole(roleName);
            redirectAttributes.addFlashAttribute("success", "Role deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete role: " + e.getMessage());
        }
        
        return "redirect:/roles";
    }
}

