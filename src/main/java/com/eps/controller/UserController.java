package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * User Controller
 * Handles user-specific APIs
 * Base URL: /api/users
 * Access: Only USER role
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final UserService userService;
    
    /**
     * User Dashboard
     * GET /user/dashboard
     * Returns user dashboard information
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto> userDashboard() {
        log.info("User Dashboard accessed");
        
        try {
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("dashboardName", "User Dashboard");
            dashboardData.put("recentActivity", "No recent activity");
            dashboardData.put("accountStatus", "Active");
            dashboardData.put("lastLogin", System.currentTimeMillis());
            
            ApiResponseDto response = ApiResponseDto.builder()
                    .success(true)
                    .message("User Dashboard loaded successfully")
                    .data(dashboardData)
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error loading user dashboard: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDto> getProfile(Principal principal) {
        log.info("Profile request received for user: {}", principal.getName());

        return userService.getUserByEmail(principal.getName())
                .map(user -> {
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("userId", user.getUserId());
                    profileData.put("fullName", user.getFullName());
                    profileData.put("email", user.getEmail());
                    profileData.put("phone", user.getPhone());
                    profileData.put("address", user.getAddress());
                    profileData.put("enabled", user.getEnabled());
                    profileData.put("role", user.getRole().getRoleName());

                    ApiResponseDto response = ApiResponseDto.builder()
                            .success(true)
                            .message("User profile loaded successfully")
                            .data(profileData)
                            .build();

                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    ApiResponseDto response = ApiResponseDto.builder()
                            .success(false)
                            .message("User profile not found")
                            .error("USER_NOT_FOUND")
                            .build();
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                });
    }
    
}
