package com.eps.service;

import com.eps.dto.UserLoginDto;
import com.eps.dto.UserLoginResponseDto;
import com.eps.dto.UserRegistrationDto;

/**
 * Authentication Service Interface
 * Defines authentication-related operations
 */
public interface AuthenticationService {
    
    /**
     * Register a new USER
     */
    String registerUser(UserRegistrationDto registrationDto);
    
    /**
     * Register a new ADMIN (only ADMIN can create another ADMIN)
     */
    String registerAdmin(UserRegistrationDto registrationDto);

    /**
     * Register a new MANAGER (only ADMIN can create a MANAGER)
     */
    String registerManager(UserRegistrationDto registrationDto);
    
    /**
     * Login user with email and password
     */
    UserLoginResponseDto login(UserLoginDto loginDto);
    
}
