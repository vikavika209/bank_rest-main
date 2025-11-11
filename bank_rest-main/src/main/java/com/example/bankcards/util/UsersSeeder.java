package com.example.bankcards.util;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class UsersSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
//            User admin = new User();
//            admin.setUsername("admin");
//            admin.setPassword(encoder.encode("123456"));
//            admin.setRoles(Set.of(Role.ROLE_ADMIN));
//            userRepository.save(admin);
//
//            User user = new User();
//            user.setUsername("user");
//            user.setPassword(encoder.encode("123456"));
//            user.setRoles(Set.of(Role.ROLE_USER));
//            userRepository.save(user);
    }
}
