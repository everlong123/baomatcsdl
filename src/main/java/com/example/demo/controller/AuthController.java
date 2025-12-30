package com.example.demo.controller;

import com.example.demo.model.LoginRequest;
import com.example.demo.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class AuthController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }
    
    @PostMapping("/login")
    public String login(LoginRequest loginRequest, HttpSession session, Model model) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty() ||
            loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            model.addAttribute("error", "Username and password are required");
            return "login";
        }
        
        boolean authenticated = authenticationService.login(
            loginRequest.getUsername(), 
            loginRequest.getPassword(), 
            session
        );
        
        if (authenticated) {
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        authenticationService.logout(session);
        return "redirect:/login?logout=true";
    }
    
    @GetMapping
    public String root() {
        return "redirect:/dashboard";
    }
}

