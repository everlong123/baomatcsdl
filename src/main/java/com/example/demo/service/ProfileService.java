package com.example.demo.service;

import com.example.demo.model.Profile;
import com.example.demo.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    
    @Autowired
    private ProfileRepository profileRepository;
    
    public List<Profile> getAllProfiles() {
        return profileRepository.getAllProfiles();
    }
    
    public Profile getProfile(String profileName) {
        return profileRepository.getProfile(profileName);
    }
    
    public void createProfile(String profileName, String sessionsPerUser,
                             String connectTime, String idleTime) {
        profileRepository.createProfile(profileName, sessionsPerUser, connectTime, idleTime);
    }
    
    public void updateProfile(String profileName, String sessionsPerUser,
                             String connectTime, String idleTime) {
        profileRepository.alterProfile(profileName, sessionsPerUser, connectTime, idleTime);
    }
    
    public void deleteProfile(String profileName) {
        profileRepository.dropProfile(profileName);
    }
}

