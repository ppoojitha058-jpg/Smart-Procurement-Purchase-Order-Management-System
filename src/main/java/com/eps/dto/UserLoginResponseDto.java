package com.eps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Login Response DTO
 * Data Transfer Object for login response containing user details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {
    
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String token;
    private String message;
    private Boolean success;
    
}
