package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private String roleName;
    private boolean hasPassword;
    private String password;
    private List<String> privileges;
    private List<String> assignedUsers;
}

