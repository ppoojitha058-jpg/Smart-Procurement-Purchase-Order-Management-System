package com.eps.service.impl;

import com.eps.dto.UserLoginDto;
import com.eps.dto.UserLoginResponseDto;
import com.eps.dto.UserRegistrationDto;
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
import com.eps.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Authentication Service Unit Tests
 * Tests the authentication service business logic
 */
public class AuthenticationServiceTest {
    
    private AuthenticationService authenticationService;
    private UserService userService;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;
    private com.eps.security.JwtUtil jwtUtil;
    private EmailService emailService;
    private DepartmentRepository departmentRepository;
    private com.eps.repository.SupplierRepository supplierRepository;
    
    @BeforeMethod
    public void setUp() {
        userService = mock(UserService.class);
        roleService = mock(RoleService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(com.eps.security.JwtUtil.class);
        emailService = mock(EmailService.class);
        departmentRepository = mock(DepartmentRepository.class);
        supplierRepository = mock(com.eps.repository.SupplierRepository.class);
        
        authenticationService = new AuthenticationServiceImpl(userService, roleService, passwordEncoder, jwtUtil, emailService, departmentRepository, supplierRepository);
    }
    
    @Test
    public void testRegisterUserSuccess() {
        // Arrange
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setFullName("John Doe");
        registrationDto.setEmail("john@example.com");
        registrationDto.setPassword("Password123");
        registrationDto.setPhone("1234567890");
        registrationDto.setAddress("123 Main St");
        
        Role userRole = new Role();
        userRole.setRoleId(2L);
        userRole.setRoleName("USER");
        
        when(userService.emailExists("john@example.com")).thenReturn(false);
        when(roleService.getRoleByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userService.saveUser(any(User.class))).thenReturn(new User());
        
        // Act
        String result = authenticationService.registerUser(registrationDto);
        
        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result, "User registered successfully");
        verify(userService, times(1)).saveUser(any(User.class));
    }
    
    @Test(expectedExceptions = EmailAlreadyExistsException.class)
    public void testRegisterUserEmailAlreadyExists() {
        // Arrange
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("existing@example.com");
        
        when(userService.emailExists("existing@example.com")).thenReturn(true);
        
        // Act
        authenticationService.registerUser(registrationDto);
    }
    
    @Test
    public void testLoginSuccess() {
        // Arrange
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("john@example.com");
        loginDto.setPassword("Password123");
        
        Role userRole = new Role();
        userRole.setRoleId(2L);
        userRole.setRoleName("USER");
        
        User user = new User();
        user.setUserId(1L);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setRole(userRole);
        
        when(userService.getUserByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);
        
        // Act
        UserLoginResponseDto response = authenticationService.login(loginDto);
        
        // Assert
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getSuccess());
        Assert.assertEquals(response.getEmail(), "john@example.com");
        Assert.assertEquals(response.getRole(), "USER");
    }
    
    @Test(expectedExceptions = UserNotFoundException.class)
    public void testLoginUserNotFound() {
        // Arrange
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("nonexistent@example.com");
        loginDto.setPassword("Password123");
        
        when(userService.getUserByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // Act
        authenticationService.login(loginDto);
    }
    
    @Test(expectedExceptions = InvalidPasswordException.class)
    public void testLoginInvalidPassword() {
        // Arrange
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("john@example.com");
        loginDto.setPassword("WrongPassword");
        
        Role userRole = new Role();
        userRole.setRoleName("USER");
        
        User user = new User();
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setRole(userRole);
        
        when(userService.getUserByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);
        
        // Act
        authenticationService.login(loginDto);
    }
    
}
