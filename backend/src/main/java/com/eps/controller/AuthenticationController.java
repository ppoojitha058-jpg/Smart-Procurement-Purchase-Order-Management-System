package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.UserLoginDto;
import com.eps.dto.UserRegistrationDto;
import com.eps.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles registration and login APIs
 * Base URL: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    /**
     * Register new USER
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        log.info("Register USER request received for email: {}", registrationDto.getEmail());
        
        try {
            String message = authenticationService.registerUser(registrationDto);
            
            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message(message)
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Register new ADMIN (Only ADMIN can create another ADMIN)
     * POST /api/auth/admin/register
     */
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto> registerAdmin(@Valid @RequestBody UserRegistrationDto registrationDto) {
        log.info("Register ADMIN request received for email: {}", registrationDto.getEmail());
        
        try {
            String message = authenticationService.registerAdmin(registrationDto);
            
            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message(message)
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error registering admin: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Login User
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto> login(@Valid @RequestBody UserLoginDto loginDto) {
        log.info("Login request received for email: {}", loginDto.getEmail());
        
        try {
            var loginResponse = authenticationService.login(loginDto);
            
            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message(loginResponse.getMessage())
                    .data(loginResponse)
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during login: {}", e.getMessage());
            throw e;
        }
    }
    
}
