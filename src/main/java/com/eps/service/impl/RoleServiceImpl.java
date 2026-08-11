package com.eps.service.impl;

import com.eps.entity.Role;
import com.eps.repository.RoleRepository;
import com.eps.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Role Service Implementation
 * Contains business logic for role operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;
    
    @Override
    public Optional<Role> getRoleByName(String roleName) {
        log.debug("Fetching role by name: {}", roleName);
        return roleRepository.findByRoleName(roleName);
    }
    
    @Override
    public Role saveRole(Role role) {
        log.debug("Saving role: {}", role.getRoleName());
        return roleRepository.save(role);
    }
    
    @Override
    public Optional<Role> getRoleById(Long roleId) {
        log.debug("Fetching role by ID: {}", roleId);
        return roleRepository.findById(roleId);
    }
    
}
