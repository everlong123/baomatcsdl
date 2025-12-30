package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivilegeInfo {
    private String privilege;
    private String grantee; // User hoặc Role nhận quyền
    private String grantor; // User cấp quyền
    private boolean adminOption; // Có quyền grant cho người khác không
    private String type; // "DIRECT", "ROLE", hoặc "OBJECT"
    private String roleName; // Nếu type = "ROLE"
    private String objectName; // Nếu type = "OBJECT" (tên table)
}

