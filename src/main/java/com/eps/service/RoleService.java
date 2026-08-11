package com.eps.service;

import com.eps.entity.Role;

import java.util.Optional;

/**
 * Role Service Interface
 * Defines role-related operations
 */
public interface RoleService {
    
    /**
     * Get role by name
     */
    Optional<Role> getRoleByName(String roleName);
    
    /**
     * Save role
     */
    Role saveRole(Role role);
    
    /**
     * Get role by ID
     */
    Optional<Role> getRoleById(Long roleId);
    
}
