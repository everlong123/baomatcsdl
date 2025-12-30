package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    public List<Role> getAllRoles() {
        return roleRepository.getAllRoles();
    }
    
    public Role getRole(String roleName) {
        return roleRepository.getRole(roleName);
    }
    
    public void createRole(String roleName, String password) {
        roleRepository.createRole(roleName, password);
    }
    
    public void updateRolePassword(String roleName, String password) {
        roleRepository.alterRolePassword(roleName, password);
    }
    
    public void deleteRole(String roleName) {
        roleRepository.dropRole(roleName);
    }
}

