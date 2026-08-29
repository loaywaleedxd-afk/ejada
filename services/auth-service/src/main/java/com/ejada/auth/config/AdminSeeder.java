package com.ejada.auth.config;

import com.ejada.auth.domain.Role;
import com.ejada.auth.entity.User;
import com.ejada.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (users.existsByUsername(adminUsername)) {
            return;
        }
        users.save(User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("Administrator")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());
    }
}
