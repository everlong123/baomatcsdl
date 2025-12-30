package com.example.demo.service;

import com.example.demo.model.PrivilegeInfo;
import com.example.demo.repository.PrivilegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivilegeService {
    
    @Autowired
    private PrivilegeRepository privilegeRepository;
    
    public List<PrivilegeInfo> getAllPrivileges() {
        return privilegeRepository.getAllPrivileges();
    }
    
    public boolean hasPrivilege(String username, String privilege) {
        return privilegeRepository.hasPrivilege(username, privilege);
    }
    
    public void grantSystemPrivilege(String privilege, String grantee, boolean withAdminOption) {
        privilegeRepository.grantSystemPrivilege(privilege, grantee, withAdminOption);
    }
    
    public void revokeSystemPrivilege(String privilege, String grantee) {
        privilegeRepository.revokeSystemPrivilege(privilege, grantee);
    }
    
    public void grantRole(String role, String grantee, boolean withAdminOption) {
        privilegeRepository.grantRole(role, grantee, withAdminOption);
    }
    
    public void revokeRole(String role, String grantee) {
        privilegeRepository.revokeRole(role, grantee);
    }
    
    public void grantObjectPrivilege(String privilege, String table, String grantee, boolean withGrantOption) {
        privilegeRepository.grantObjectPrivilege(privilege, table, grantee, withGrantOption);
    }
    
    public void revokeObjectPrivilege(String privilege, String table, String grantee) {
        privilegeRepository.revokeObjectPrivilege(privilege, table, grantee);
    }
    
    public void grantColumnPrivilege(String privilege, String table, String column, String grantee) {
        privilegeRepository.grantColumnPrivilege(privilege, table, column, grantee);
    }
    
    public List<String> getAvailableTablespaces() {
        return privilegeRepository.getAvailableTablespaces();
    }
}

