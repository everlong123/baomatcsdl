package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.PrivilegeService;
import com.example.demo.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/profiles")
public class ProfileController {
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    @GetMapping
    public String listProfiles(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        List<Profile> profiles = profileService.getAllProfiles();
        
        model.addAttribute("profiles", profiles);
        model.addAttribute("username", currentUser);
        model.addAttribute("isAdmin", authenticationService.hasAdminCapabilities(currentUser, privilegeService));
        
        return "profiles/list";
    }
    
    @GetMapping("/create")
    public String createProfileForm(HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE PROFILE")) {
            return "redirect:/profiles?error=no_privilege";
        }
        
        model.addAttribute("profile", new Profile());
        return "profiles/create";
    }
    
    @PostMapping("/create")
    public String createProfile(@ModelAttribute Profile profile, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "CREATE PROFILE")) {
            return "redirect:/profiles?error=no_privilege";
        }
        
        try {
            profileService.createProfile(
                profile.getProfileName(),
                profile.getSessionsPerUser(),
                profile.getConnectTime(),
                profile.getIdleTime()
            );
            redirectAttributes.addFlashAttribute("success", "Profile created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create profile: " + e.getMessage());
        }
        
        return "redirect:/profiles";
    }
    
    @GetMapping("/{profileName}/edit")
    public String editProfileForm(@PathVariable String profileName, HttpSession session, Model model) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "ALTER PROFILE")) {
            return "redirect:/profiles?error=no_privilege";
        }
        
        Profile profile = profileService.getProfile(profileName);
        model.addAttribute("profile", profile);
        return "profiles/edit";
    }
    
    @PostMapping("/{profileName}/edit")
    public String updateProfile(@PathVariable String profileName, @ModelAttribute Profile profile,
                               HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "ALTER PROFILE")) {
            return "redirect:/profiles?error=no_privilege";
        }
        
        try {
            profileService.updateProfile(
                profileName,
                profile.getSessionsPerUser(),
                profile.getConnectTime(),
                profile.getIdleTime()
            );
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        
        return "redirect:/profiles";
    }
    
    @PostMapping("/{profileName}/delete")
    public String deleteProfile(@PathVariable String profileName, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!authenticationService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        String currentUser = authenticationService.getCurrentUser(session);
        // Check Oracle privilege - no hardcoding
        if (!privilegeService.hasPrivilege(currentUser, "DROP PROFILE")) {
            return "redirect:/profiles?error=no_privilege";
        }
        
        try {
            profileService.deleteProfile(profileName);
            redirectAttributes.addFlashAttribute("success", "Profile deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete profile: " + e.getMessage());
        }
        
        return "redirect:/profiles";
    }
}

