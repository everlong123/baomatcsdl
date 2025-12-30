package com.example.demo.controller;

import com.example.demo.service.AuthenticationService;
import com.example.demo.service.PrivilegeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/features")
public class FeaturesController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @GetMapping
    public String features(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String username = authenticationService.getCurrentUser(session);
        // Check Oracle privileges - no hardcoding
        boolean isAdmin = authenticationService.hasAdminCapabilities(username, privilegeService);
        
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", isAdmin);
        
        return "features";
    }
}

