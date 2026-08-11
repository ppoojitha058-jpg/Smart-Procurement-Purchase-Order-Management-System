package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.entity.User;
import com.eps.dto.UserRegistrationDto;
import com.eps.service.AuthenticationService;
import com.eps.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller
 * Handles admin-specific APIs
 * Base URL: /api/admin
 * Access: Only ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    
    /**
     * Admin Dashboard
     * GET /admin/dashboard
     * Returns admin dashboard information
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto> adminDashboard() {
        log.info("Admin Dashboard accessed");
        
        try {
            int totalUsers = 0;
            int totalAdmins = 0;
            for (User user : userService.getAllUsers()) {
                totalUsers++;
                if ("ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
                    totalAdmins++;
                }
            }

            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("dashboardName", "Admin Dashboard");
            dashboardData.put("totalUsers", totalUsers);
            dashboardData.put("totalAdmins", totalAdmins);
            dashboardData.put("systemStatus", "Active");
            dashboardData.put("lastUpdated", System.currentTimeMillis());
            
            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message("Admin Dashboard loaded successfully")
                    .data(dashboardData)
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error loading admin dashboard: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto> listUsers() {
        log.info("Admin requested all users");

        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : userService.getAllUsers()) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("enabled", user.getEnabled());
            userData.put("role", user.getRole().getRoleName());
            users.add(userData);
        }

        ApiResponseDto response = ApiResponseDto.builder()
                .success(true)
                .message("Users loaded successfully")
                .data(users)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PostMapping("/manager/register")
    public ResponseEntity<ApiResponseDto> registerManager(@RequestBody UserRegistrationDto registrationDto) {
        log.info("Admin requested manager registration for email: {}", registrationDto.getEmail());

        try {
            String message = authenticationService.registerManager(registrationDto);

            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message(message)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error registering manager: {}", e.getMessage());
            throw e;
        }
    }
    
}
