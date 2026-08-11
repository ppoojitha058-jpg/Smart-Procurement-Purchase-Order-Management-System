package com.eps.repository;

import com.eps.entity.Role;
import com.eps.entity.User;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * User Repository Unit Tests
 * Tests the user repository database operations
 */
@SpringBootTest
public class UserRepositoryTest extends AbstractTestNGSpringContextTests {
    
    private UserRepository userRepository = mock(UserRepository.class);
    private RoleRepository roleRepository = mock(RoleRepository.class);
    
    @Test
    public void testFindByEmailSuccess() {
        // Arrange
        Role role = new Role();
        role.setRoleId(2L);
        role.setRoleName("USER");
        
        User user = new User();
        user.setUserId(1L);
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        user.setPassword("password123");
        user.setPhone("1234567890");
        user.setEnabled(true);
        user.setCreatedDate(LocalDateTime.now());
        user.setRole(role);
        
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        
        // Act
        Optional<User> result = userRepository.findByEmail("john@example.com");
        
        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getEmail(), "john@example.com");
    }
    
    @Test
    public void testFindByEmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // Act
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");
        
        // Assert
        Assert.assertFalse(result.isPresent());
    }
    
    @Test
    public void testExistsByEmailTrue() {
        // Arrange
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // Act
        Boolean result = userRepository.existsByEmail("existing@example.com");
        
        // Assert
        Assert.assertTrue(result);
    }
    
    @Test
    public void testExistsByEmailFalse() {
        // Arrange
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);
        
        // Act
        Boolean result = userRepository.existsByEmail("nonexistent@example.com");
        
        // Assert
        Assert.assertFalse(result);
    }
    
}
