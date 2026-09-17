package com.eps.service.impl;

import com.eps.entity.User;
import com.eps.repository.UserRepository;
import com.eps.service.UserService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * User Service Unit Tests
 * Tests the user service business logic
 */
public class UserServiceTest {
    
    private UserService userService;
    private UserRepository userRepository;
    
    @BeforeMethod
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserServiceImpl(userRepository);
    }
    
    @Test
    public void testGetUserByEmailSuccess() {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        
        // Act
        Optional<User> result = userService.getUserByEmail("john@example.com");
        
        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getEmail(), "john@example.com");
    }
    
    @Test
    public void testGetUserByEmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // Act
        Optional<User> result = userService.getUserByEmail("nonexistent@example.com");
        
        // Assert
        Assert.assertFalse(result.isPresent());
    }
    
    @Test
    public void testEmailExistsTrue() {
        // Arrange
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // Act
        Boolean result = userService.emailExists("existing@example.com");
        
        // Assert
        Assert.assertTrue(result);
    }
    
    @Test
    public void testEmailExistsFalse() {
        // Arrange
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);
        
        // Act
        Boolean result = userService.emailExists("nonexistent@example.com");
        
        // Assert
        Assert.assertFalse(result);
    }
    
    @Test
    public void testSaveUserSuccess() {
        // Arrange
        User user = new User();
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        
        when(userRepository.save(user)).thenReturn(user);
        
        // Act
        User result = userService.saveUser(user);
        
        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getEmail(), "john@example.com");
    }
    
    @Test
    public void testGetUserByIdSuccess() {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setEmail("john@example.com");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // Act
        Optional<User> result = userService.getUserById(1L);
        
        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getUserId(), 1L);
    }
    
}
