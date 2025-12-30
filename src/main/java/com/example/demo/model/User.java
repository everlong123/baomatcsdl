package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String username;
    private String password;
    private String defaultTablespace;
    private String temporaryTablespace;
    private String quota;
    private String accountStatus;
    private LocalDateTime lockDate;
    private LocalDateTime createdDate;
    private String profile;
    private List<String> roles;
    private List<PrivilegeInfo> privileges;
    
    // Thông tin bổ sung từ APP_USER_PROFILE
    private String fullName;
    private String email;
    private String phone;
    private String address;
}

