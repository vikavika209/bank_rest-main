package com.example.bankcards.service;

import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (existsByUsername(dto.getUsername())) {
            log.error("Пользователь с таким логином уже существует: {}", dto.getUsername());
            throw new IllegalArgumentException("Пользователь с таким логином уже существует");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(Role.ROLE_USER)));

        User saved = userRepository.save(user);
        log.info("Сохранён новый пользователь с id: {}", saved.getId());

        return userMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: id = {}", id);
                    return new NoSuchElementException("Пользователь не найден: " + id);
                });
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users = userRepository.findAll(pageable);
        Page<UserResponseDto> map = users.map(userMapper::toDto);
        log.info("Кол-во найденных пользователей = {}", map.getSize());

        return map;
    }

    @Transactional
    public UserResponseDto updateUsername(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + id));

        String oldUsername = user.getUsername();

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {

            if (existsByUsername(dto.getUsername())) {
                log.error("\"Логин уже занят: {}", dto.getUsername());
                throw new IllegalArgumentException("Логин уже занят: " + dto.getUsername());
            }
            user.setUsername(dto.getUsername());
        }

        User saved = userRepository.save(user);
        log.info("Логин пользователя с id = {} изменён: {} -> {}", id, oldUsername, saved.getUsername());
        return userMapper.toDto(saved);
    }

    @Transactional
    public UserResponseDto updatePassword(Long id, UserRequestDto dto) {
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            log.error("Пароль должен содержать минимум 6 символов");
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + id));
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        User saved = userRepository.save(user);
        log.info("Пароль пользователя {} изменён", id);

        return userMapper.toDto(saved);
    };

    @Transactional
    public UserResponseDto makeUnavailable (Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", userId);
                    return new NoSuchElementException("Пользователь не найден: " + userId);
                });

        user.setEnabled(false);

        User save = userRepository.save(user);

        log.info("Пользователь заблокирован: id = {}", save.getId());

        return userMapper.toDto(save);
    }

    @Transactional
    public UserResponseDto changeRoleAdmin (Long userId, boolean isAdmin){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", userId);
                    return new NoSuchElementException("Пользователь не найден: " + userId);
                });

        Set<Role> roles = user.getRoles();

        if(isAdmin){
            roles.add(Role.ROLE_ADMIN);
        }

        else {
            if (!roles.contains(Role.ROLE_ADMIN)){
                log.error("Пользователь не является админом: {}", user.getRoles());
                return userMapper.toDto(user);
            }

            roles.remove(Role.ROLE_ADMIN);
        }

        user.setRoles(roles);

        User save = userRepository.save(user);

        log.info("Новые роли пользователя: {}", save.getRoles());

        return userMapper.toDto(save);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            log.error("Пользователь не найден с id = {} ", id);
            throw new NoSuchElementException("Пользователь не найден: " + id);
        }
        userRepository.deleteById(id);
        log.info("Пользователь с id = {} удалён", id);
    }

    private boolean existsByUsername(String username){
        Optional<User> byUsername = userRepository.getByUsername(username);
        if (byUsername.isPresent()){
            return true;
        }
        return false;
    }
}
