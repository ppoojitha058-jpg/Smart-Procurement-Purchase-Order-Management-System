package com.eps.config;

import com.eps.entity.Role;
import com.eps.entity.User;
import com.eps.service.RoleService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Application data initializer
 * Seeds required roles and creates a default admin account on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Initializing default roles and admin account");

        Role adminRole = roleService.getRoleByName("ADMIN")
                .orElseGet(() -> roleService.saveRole(new Role(null, "ADMIN")));

        Role userRole = roleService.getRoleByName("USER")
            .orElseGet(() -> roleService.saveRole(new Role(null, "USER")));

        Role managerRole = roleService.getRoleByName("MANAGER")
            .orElseGet(() -> roleService.saveRole(new Role(null, "MANAGER")));

        userService.getUserByEmail("admin@eps.com").ifPresentOrElse(
                existingAdmin -> log.info("Default admin account already exists: {}", existingAdmin.getEmail()),
                () -> {
                    User admin = new User();
                    admin.setFullName("System Administrator");
                    admin.setEmail("admin@eps.com");
                    admin.setPassword(passwordEncoder.encode("Admin@123"));
                    admin.setPhone("0000000000");
                    admin.setAddress("Default administrator account");
                    admin.setRole(adminRole);
                    admin.setEnabled(true);
                    userService.saveUser(admin);
                    log.info("Created default admin account: admin@eps.com / Admin@123");
                }
        );

        if (userRole == null || adminRole == null) {
            log.warn("One or more required roles are still missing after initialization");
        }
    }
}
