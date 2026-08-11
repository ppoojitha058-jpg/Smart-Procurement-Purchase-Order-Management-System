package com.eps.service.impl;

import com.eps.entity.User;
import com.eps.repository.UserRepository;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User Service Implementation
 * Contains business logic for user operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }
    
    @Override
    public Boolean emailExists(String email) {
        log.debug("Checking if email exists: {}", email);
        return userRepository.existsByEmail(email);
    }
    
    @Override
    public User saveUser(User user) {
        log.debug("Saving user: {}", user.getEmail());
        return userRepository.save(user);
    }
    
    @Override
    public Optional<User> getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId);
    }
    
    @Override
    public Iterable<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }
    
}
