package com.example.demo.controller;

import com.example.demo.model.AppLoginUser;
import com.example.demo.model.User;
import com.example.demo.repository.AppLoginUserRepository;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.PrivilegeService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.RoleService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private AppLoginUserRepository appLoginUserRepository;
    
    @GetMapping
    public String listUsers(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privileges - no hardcoding
        boolean hasAdminCapabilities = authenticationService.hasAdminCapabilities(currentUser, privilegeService);
        
        try {
            List<User> users = userService.getAllUsers(currentUser);
            model.addAttribute("users", users);
        } catch (Exception e) {
            System.err.println("Error in listUsers: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("error", "Không thể tải danh sách users: " + e.getMessage());
        }
        
        model.addAttribute("username", currentUser);
        model.addAttribute("isAdmin", hasAdminCapabilities);
        
        return "users/list";
    }
    
    @GetMapping("/{username}")
    public String viewUser(@PathVariable String username, HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        try {
            User user = userService.getUser(username, currentUser);
            model.addAttribute("user", user);
            model.addAttribute("username", currentUser);
            model.addAttribute("isAdmin", authenticationService.hasAdminCapabilities(currentUser, privilegeService));
            return "users/view";
        } catch (SecurityException e) {
            return "redirect:/users?error=access_denied";
        }
    }
    
    @GetMapping("/create")
    public String createUserForm(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        // Kiểm tra quyền CREATE USER
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=no_privilege";
        }
        
        model.addAttribute("user", new User());
        model.addAttribute("tablespaces", privilegeService.getAvailableTablespaces());
        return "users/create";
    }
    
    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            userService.createUser(
                user.getUsername(),
                user.getPassword(),
                user.getDefaultTablespace(),
                user.getTemporaryTablespace(),
                user.getQuota()
            );
            redirectAttributes.addFlashAttribute("success", 
                "User " + user.getUsername() + " created successfully");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            
            // Friendly error messages
            if (errorMsg.contains("ORA-01920")) {
                errorMsg = "User '" + user.getUsername() + "' already exists in Oracle";
            } else if (errorMsg.contains("ORA-00001") || errorMsg.contains("unique constraint")) {
                errorMsg = "User '" + user.getUsername() + "' already exists in application database";
            } else if (errorMsg.contains("ORA-01031")) {
                errorMsg = "Insufficient privileges to create user";
            } else if (errorMsg.contains("ORA-00959")) {
                errorMsg = "Tablespace does not exist";
            }
            
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + errorMsg);
            System.err.println("Error creating user " + user.getUsername() + ": " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @GetMapping("/{username}/edit")
    public String editUserForm(@PathVariable String username, HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            User user = userService.getUser(username, currentUser);
            model.addAttribute("user", user);
            model.addAttribute("tablespaces", privilegeService.getAvailableTablespaces());
            // Add profiles and roles for dropdown
            model.addAttribute("profiles", profileService.getAllProfiles());
            model.addAttribute("roles", roleService.getAllRoles());
            return "users/edit";
        } catch (Exception e) {
            return "redirect:/users?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/{username}/edit")
    public String updateUser(@PathVariable String username, @ModelAttribute User user,
                           HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            userService.updateUser(
                username,
                user.getPassword(),
                user.getDefaultTablespace(),
                user.getTemporaryTablespace(),
                user.getQuota(),
                user.getProfile()
            );
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @PostMapping("/{username}/lock")
    public String lockUser(@PathVariable String username, HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            userService.lockUser(username);
            redirectAttributes.addFlashAttribute("success", "User locked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to lock user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @PostMapping("/{username}/unlock")
    public String unlockUser(@PathVariable String username, HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            userService.unlockUser(username);
            redirectAttributes.addFlashAttribute("success", "User unlocked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unlock user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @PostMapping("/{username}/delete")
    public String deleteUser(@PathVariable String username, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        // Kiểm tra quyền DROP USER
        if (!privilegeService.hasPrivilege(currentUser, "DROP USER")) {
            return "redirect:/users?error=no_privilege";
        }
        
        try {
            userService.deleteUser(username);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    /**
     * Add existing Oracle user to APP_LOGIN_USER for application login.
     * This is useful when user was created directly in Oracle (not through the app).
     */
    @PostMapping("/{username}/add-to-app-login")
    public String addToAppLogin(@PathVariable String username,
                                @RequestParam String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege
        if (!privilegeService.hasPrivilege(currentUser, "CREATE USER")) {
            return "redirect:/users?error=access_denied";
        }
        
        try {
            // Hash password with BCrypt
            String passwordHash = authenticationService.encodePassword(password);
            
            // Add to APP_LOGIN_USER
            AppLoginUser appLoginUser = new AppLoginUser(username.toUpperCase(), passwordHash);
            appLoginUserRepository.save(appLoginUser);
            
            redirectAttributes.addFlashAttribute("success", 
                "User " + username + " đã được thêm vào APP_LOGIN_USER. Bạn có thể login với password: " + password);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to add user to APP_LOGIN_USER: " + e.getMessage());
        }
        
        return "redirect:/users/" + username;
    }
}

