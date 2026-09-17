package com.eps.service.impl;

import com.eps.dto.UserLoginDto;
import com.eps.dto.UserLoginResponseDto;
import com.eps.dto.UserRegistrationDto;
import com.eps.entity.Department;
import com.eps.entity.Role;
import com.eps.entity.User;
import com.eps.exception.EmailAlreadyExistsException;
import com.eps.exception.InvalidPasswordException;
import com.eps.exception.UserNotFoundException;
import com.eps.repository.DepartmentRepository;
import com.eps.service.AuthenticationService;
import com.eps.service.EmailService;
import com.eps.service.RoleService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import com.eps.security.JwtUtil;

/**
 * Authentication Service Implementation
 * Contains business logic for user registration and login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final DepartmentRepository departmentRepository;
    private final com.eps.repository.SupplierRepository supplierRepository;
    
    @Override
    @Transactional
    public String registerUser(UserRegistrationDto registrationDto) {
        log.info("Registering new USER with email: {}", registrationDto.getEmail());
        
        // Check if email already exists
        if (userService.emailExists(registrationDto.getEmail())) {
            log.warn("Email already exists: {}", registrationDto.getEmail());
            throw new EmailAlreadyExistsException("Email already registered: " + registrationDto.getEmail());
        }
        
        // Public registration is limited to user and supplier accounts. Manager and
        // administrator accounts are provisioned through their protected flows.
        String requestedRole = registrationDto.getRole() != null && !registrationDto.getRole().isBlank()
                ? registrationDto.getRole().trim().toUpperCase()
                : "USER";
        String cleanRole = requestedRole.replace("ROLE_", "");
        if (!"USER".equals(cleanRole) && !"SUPPLIER".equals(cleanRole)) {
            throw new IllegalArgumentException("Public registration supports only USER or SUPPLIER roles");
        }
        Role userRole = roleService.getRoleByName(cleanRole)
                .or(() -> roleService.getRoleByName("ROLE_" + cleanRole))
                .orElseGet(() -> roleService.saveRole(new Role(null, cleanRole)));
        
        // Create new user
        User user = new User();
        user.setFullName(registrationDto.getFullName());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setPhone(registrationDto.getPhone());
        user.setAddress(registrationDto.getAddress());
        user.setRole(userRole);
        user.setEnabled(true);
        
        // Assign department from registration
        String deptName = registrationDto.getDepartmentName();
        if (deptName != null && !deptName.isBlank()) {
            departmentRepository.findByDepartmentName(deptName).ifPresent(user::setDepartment);
        }

        userService.saveUser(user);
        log.info("USER registered successfully with email: {}", registrationDto.getEmail());

        // If registering as SUPPLIER, ensure a Supplier entity exists
        if ("SUPPLIER".equalsIgnoreCase(cleanRole)) {
            if (supplierRepository.findByEmailIgnoreCase(user.getEmail()).isEmpty()) {
                com.eps.entity.Supplier s = new com.eps.entity.Supplier();
                s.setName(user.getFullName());
                s.setEmail(user.getEmail());
                s.setPhone(user.getPhone());
                s.setAddress(user.getAddress() != null ? user.getAddress() : "Industrial Logistics Hub");
                s.setStatus(com.eps.enums.SupplierStatus.ACTIVE);
                s.setRating(java.math.BigDecimal.valueOf(5.0));
                s.setGstNumber("29AAACH" + (int)(1000 + Math.random()*9000) + "R1Z5");
                supplierRepository.save(s);
                log.info("Auto-created Supplier record for vendor: {}", user.getEmail());
            }
        }

        final String userEmail = user.getEmail();
        final String roleName = user.getRole().getRoleName();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.sendHtmlEmailFromTemplate(userEmail, "Welcome to Enterprise Procurement System", "welcome-email", Map.of(
                        "email", userEmail,
                        "role", roleName
                ));
            } catch (Exception ex) {
                log.warn("Asynchronous welcome email to {} could not be delivered: {}", userEmail, ex.getMessage());
            }
        });
        
        return "User registered successfully";
    }
    
    @Override
    @Transactional
    public String registerAdmin(UserRegistrationDto registrationDto) {
        log.info("Registering new ADMIN with email: {}", registrationDto.getEmail());
        
        // Check if email already exists
        if (userService.emailExists(registrationDto.getEmail())) {
            log.warn("Email already exists: {}", registrationDto.getEmail());
            throw new EmailAlreadyExistsException("Email already registered: " + registrationDto.getEmail());
        }
        
        // Get ADMIN role
        Role adminRole = roleService.getRoleByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        
        // Create new admin
        User admin = new User();
        admin.setFullName(registrationDto.getFullName());
        admin.setEmail(registrationDto.getEmail());
        admin.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        admin.setPhone(registrationDto.getPhone());
        admin.setAddress(registrationDto.getAddress());
        admin.setRole(adminRole);
        admin.setEnabled(true);
        
        userService.saveUser(admin);
        log.info("ADMIN registered successfully with email: {}", registrationDto.getEmail());

        final String adminEmail = admin.getEmail();
        final String adminRoleName = admin.getRole().getRoleName();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.sendHtmlEmailFromTemplate(adminEmail, "Welcome to Enterprise Procurement System", "welcome-email", Map.of(
                        "email", adminEmail,
                        "role", adminRoleName
                ));
            } catch (Exception ex) {
                log.warn("Asynchronous welcome email to admin {} could not be delivered: {}", adminEmail, ex.getMessage());
            }
        });
        
        return "Admin registered successfully";
    }

    @Override
    @Transactional
    public String registerManager(UserRegistrationDto registrationDto) {
        log.info("Registering new MANAGER with email: {}", registrationDto.getEmail());

        // Check if email already exists
        if (userService.emailExists(registrationDto.getEmail())) {
            log.warn("Email already exists: {}", registrationDto.getEmail());
            throw new EmailAlreadyExistsException("Email already registered: " + registrationDto.getEmail());
        }

        // Get MANAGER role
        Role managerRole = roleService.getRoleByName("MANAGER")
                .orElseThrow(() -> new RuntimeException("MANAGER role not found"));

        // Create new manager
        User manager = new User();
        manager.setFullName(registrationDto.getFullName());
        manager.setEmail(registrationDto.getEmail());
        manager.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        manager.setPhone(registrationDto.getPhone());
        manager.setAddress(registrationDto.getAddress());
        manager.setRole(managerRole);
        manager.setEnabled(true);
        if (registrationDto.getDepartmentId() != null) {
            departmentRepository.findById(registrationDto.getDepartmentId()).ifPresent(manager::setDepartment);
        } else if (registrationDto.getDepartmentName() != null && !registrationDto.getDepartmentName().isBlank()) {
            departmentRepository.findByDepartmentName(registrationDto.getDepartmentName()).ifPresent(manager::setDepartment);
        }

        userService.saveUser(manager);
        log.info("MANAGER registered successfully with email: {}", registrationDto.getEmail());

        final String mgrEmail = manager.getEmail();
        final String mgrRoleName = manager.getRole().getRoleName();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.sendHtmlEmailFromTemplate(mgrEmail, "Welcome to Enterprise Procurement System", "welcome-email", Map.of(
                        "email", mgrEmail,
                        "role", mgrRoleName
                ));
            } catch (Exception ex) {
                log.warn("Asynchronous welcome email to manager {} could not be delivered: {}", mgrEmail, ex.getMessage());
            }
        });

        return "Manager registered successfully";
    }
    
    @Override
    public UserLoginResponseDto login(UserLoginDto loginDto) {
        log.info("Attempting login for email: {}", loginDto.getEmail());
        
        // Find user by email
        Optional<User> userOptional = userService.getUserByEmail(loginDto.getEmail());
        
        if (userOptional.isEmpty()) {
            log.warn("User not found with email: {}", loginDto.getEmail());
            throw new UserNotFoundException("User not found with email: " + loginDto.getEmail());
        }
        
        User user = userOptional.get();
        
        // Check if user is enabled
        if (!user.getEnabled()) {
            log.warn("User account is disabled: {}", loginDto.getEmail());
            throw new UserNotFoundException("User account is disabled");
        }
        
        // Validate password
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginDto.getEmail());
            throw new InvalidPasswordException("Invalid password");
        }
        
        log.info("Login successful for user: {}", loginDto.getEmail());
        
        // Build response
        UserLoginResponseDto response = new UserLoginResponseDto();
        response.setUserId(user.getUserId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().getRoleName());
        response.setMessage("Login successful");
        response.setSuccess(true);
        if (user.getDepartment() != null) {
            response.setDepartmentId(user.getDepartment().getDepartmentId());
            response.setDepartmentName(user.getDepartment().getDepartmentName());
        } else {
            departmentRepository.findByDepartmentName("Information Technology").ifPresent(d -> {
                user.setDepartment(d);
                userService.saveUser(user);
                response.setDepartmentId(d.getDepartmentId());
                response.setDepartmentName(d.getDepartmentName());
            });
        }

        // Generate JWT token
        try {
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());
            response.setToken(token);
        } catch (Exception ex) {
            log.error("Failed to generate JWT for user {}: {}", user.getEmail(), ex.getMessage());
        }
        
        return response;
    }
    
}
