package com.example.demo.config;

import com.example.demo.mapper.UserMapper;
import com.example.demo.model.UserAccount;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminUserSeeder {

    @Bean
    @ConditionalOnProperty(name = "app.admin.seed.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedAdminUser(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            if (userMapper.selectByUsername(adminUsername) != null) {
                return;
            }

            UserAccount user = new UserAccount();
            LocalDateTime now = LocalDateTime.now();
            user.setUsername(adminUsername);
            user.setPasswordHash(passwordEncoder.encode(adminPassword));
            user.setRole("ADMIN");
            user.setEnabled(true);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
        };
    }
}
