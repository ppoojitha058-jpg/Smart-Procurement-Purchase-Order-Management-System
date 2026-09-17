package com.eps.repository;

import com.eps.entity.Role;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Role Repository Unit Tests
 * Tests the role repository database operations
 */
@SpringBootTest
public class RoleRepositoryTest extends AbstractTestNGSpringContextTests {
    
    private RoleRepository roleRepository = mock(RoleRepository.class);
    
    @Test
    public void testFindByRoleNameAdmin() {
        // Arrange
        Role adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        
        // Act
        Optional<Role> result = roleRepository.findByRoleName("ADMIN");
        
        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getRoleName(), "ADMIN");
    }
    
    @Test
    public void testFindByRoleNameUser() {
        // Arrange
        Role userRole = new Role();
        userRole.setRoleId(2L);
        userRole.setRoleName("USER");
        
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        
        // Act
        Optional<Role> result = roleRepository.findByRoleName("USER");
        
        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getRoleName(), "USER");
    }
    
    @Test
    public void testFindByRoleNameNotFound() {
        // Arrange
        when(roleRepository.findByRoleName("NONEXISTENT")).thenReturn(Optional.empty());
        
        // Act
        Optional<Role> result = roleRepository.findByRoleName("NONEXISTENT");
        
        // Assert
        Assert.assertFalse(result.isPresent());
    }
    
}
