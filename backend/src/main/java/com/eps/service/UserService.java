package com.eps.service;

import com.eps.entity.User;

import java.util.Optional;

/**
 * User Service Interface
 * Defines user-related operations
 */
public interface UserService {
    
    /**
     * Get user by email
     */
    Optional<User> getUserByEmail(String email);
    
    /**
     * Check if email exists
     */
    Boolean emailExists(String email);
    
    /**
     * Save user
     */
    User saveUser(User user);
    
    /**
     * Get user by ID
     */
    Optional<User> getUserById(Long userId);
    
    /**
     * Get all users
     */
    Iterable<User> getAllUsers();
    
}
