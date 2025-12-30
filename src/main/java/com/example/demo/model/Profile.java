package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private String profileName;
    private String sessionsPerUser; // UNLIMITED, DEFAULT, hoặc số cụ thể
    private String connectTime;     // UNLIMITED, DEFAULT, hoặc số cụ thể (phút)
    private String idleTime;        // UNLIMITED, DEFAULT, hoặc số cụ thể (phút)
    private List<String> assignedUsers;
}

