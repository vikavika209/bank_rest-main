package com.example.bankcards.service;

import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("create() — успешно создаёт пользователя")
    void create_ok() {
        UserRequestDto req = new UserRequestDto();
        req.setUsername("vika");
        req.setPassword("secret123");

        User toSave = new User();
        toSave.setUsername("vika");
        toSave.setPassword("ENC(secret123)");
        toSave.setEnabled(true);
        toSave.setRoles(new HashSet<>(Set.of(Role.ROLE_USER)));

        User saved = new User();
        saved.setId(1L);
        saved.setUsername("vika");
        saved.setPassword("ENC(secret123)");
        saved.setEnabled(true);
        saved.setRoles(new HashSet<>(Set.of(Role.ROLE_USER)));

        UserResponseDto dto = new UserResponseDto();
        dto.setId(1L);
        dto.setUsername("vika");
        dto.setEnabled(true);

        when(userRepository.getByUsername("vika")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("ENC(secret123)");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.create(req);

        Assertions.assertEquals(res.getId(),1L);
        Assertions.assertEquals(res.getUsername(), "vika");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(any(User.class));
    }

    @Test
    @DisplayName("create() — бросает ошибку, если username уже занят")
    void create_duplicateUsername() {
        UserRequestDto req = new UserRequestDto();
        req.setUsername("vika");
        req.setPassword("secret123");

        when(userRepository.getByUsername("vika")).thenReturn(Optional.of(new User()));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.create(req)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById() — возвращает пользователя")
    void getById_ok() {
        User u = new User();
        u.setId(3L);
        u.setUsername("john");
        u.setEnabled(true);

        when(userRepository.findById(3L)).thenReturn(Optional.of(u));

        UserResponseDto dto = new UserResponseDto();
        dto.setId(3L);
        dto.setUsername("john");
        dto.setEnabled(true);

        when(userMapper.toDto(u)).thenReturn(dto);

        UserResponseDto res = userService.getById(3L);

        Assertions.assertEquals(res.getId(), 3L);
        Assertions.assertEquals(res.getUsername(), "john");
        verify(userMapper).toDto(u);
    }

    @Test
    @DisplayName("getById() — если не найден, NoSuchElementException")
    void getById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> userService.getById(99L)
        );
    }

    @Test
    @DisplayName("getAll() — отдаёт страницу пользователей")
    void getAll_ok() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());

        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("a");

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("b");

        Page<User> page = new PageImpl<>(List.of(u2, u1), pageable, 2);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserResponseDto d1 = new UserResponseDto(); d1.setId(2L); d1.setUsername("b");
        UserResponseDto d2 = new UserResponseDto(); d2.setId(1L); d2.setUsername("a");

        when(userMapper.toDto(u2)).thenReturn(d1);
        when(userMapper.toDto(u1)).thenReturn(d2);

        Page<UserResponseDto> result = userService.getAll(0, 2);

        Assertions.assertEquals(result.getTotalElements(), 2);
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("updateUsername() — меняет логин, если свободен")
    void updateUsername_ok() {
        UserRequestDto req = new UserRequestDto();
        req.setUsername("newname");

        User existing = new User();
        existing.setId(5L);
        existing.setUsername("oldname");

        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.getByUsername("newname")).thenReturn(Optional.empty());

        User saved = new User();
        saved.setId(5L);
        saved.setUsername("newname");

        when(userRepository.save(existing)).thenReturn(saved);

        UserResponseDto dto = new UserResponseDto();
        dto.setId(5L); dto.setUsername("newname");
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.updateUsername(5L, req);

        Assertions.assertEquals(res.getUsername(), "newname");
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUsername() — бросает, если новый логин занят")
    void updateUsername_duplicate() {
        UserRequestDto req = new UserRequestDto();
        req.setUsername("newname");

        User existing = new User();
        existing.setId(5L);
        existing.setUsername("oldname");

        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.getByUsername("newname")).thenReturn(Optional.of(new User()));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUsername(5L, req)
        );
    }

    @Test
    @DisplayName("updatePassword() — меняет пароль, валидный кейс")
    void updatePassword_ok() {
        UserRequestDto req = new UserRequestDto();
        req.setPassword("newpass");

        User existing = User.builder().id(7L).username("vika").password("OLD").build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass")).thenReturn("ENC(newpass)");

        User saved = User.builder().id(7L).username("vika").password("ENC(newpass)").build();
        when(userRepository.save(existing)).thenReturn(saved);

        UserResponseDto dto = new UserResponseDto();
        dto.setId(7L); dto.setUsername("vika");
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.updatePassword(7L, req);

        Assertions.assertEquals(existing.getPassword(), "ENC(newpass)");
        Assertions.assertEquals(res.getId(), 7L);
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(existing);
        verify(userMapper).toDto(saved);
    }

    @Test
    @DisplayName("updatePassword() — бросает, если пароль короче 6")
    void updatePassword_short() {
        UserRequestDto req = new UserRequestDto();
        req.setPassword("12345");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePassword(1L, req)
        );

        verifyNoInteractions(userRepository, passwordEncoder, userMapper);
    }

    @Test
    @DisplayName("makeUnavailable() — переводит enabled=false")
    void makeUnavailable_ok() {
        User existing = User.builder().id(9L).username("john").enabled(true).build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(existing));

        User saved = User.builder().id(9L).username("john").enabled(false).build();
        when(userRepository.save(existing)).thenReturn(saved);

        UserResponseDto dto = new UserResponseDto();
        dto.setId(9L); dto.setUsername("john"); dto.setEnabled(false);
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.makeUnavailable(9L);

        Assertions.assertEquals(existing.isEnabled(), false);
        Assertions.assertEquals(res.isEnabled(), false);
    }

    @Test
    @DisplayName("changeRoleAdmin(true) — добавляет ROLE_ADMIN")
    void changeRoleAdmin_add() {
        User existing = User.builder()
                .id(10L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));

        User saved = User.builder()
                .id(10L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN)))
                .build();

        when(userRepository.save(existing)).thenReturn(saved);

        UserResponseDto dto = new UserResponseDto();
        dto.setId(10L); dto.setUsername("vika");
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.changeRoleAdmin(10L, true);


        Assertions.assertEquals(saved.getRoles().size(), 2);
        verify(userRepository).save(existing);
        verify(userMapper).toDto(saved);
    }

    @Test
    @DisplayName("changeRoleAdmin(false) — удаляет ROLE_ADMIN, если была")
    void changeRoleAdmin_remove_ok() {
        User existing = User.builder()
                .id(11L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN)))
                .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(existing));

        User saved = User.builder()
                .id(11L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();

        when(userRepository.save(existing)).thenReturn(saved);

        UserResponseDto dto = new UserResponseDto();
        dto.setId(11L); dto.setUsername("vika");
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserResponseDto res = userService.changeRoleAdmin(11L, false);

        Assertions.assertEquals(saved.getRoles().size(), 1);
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("changeRoleAdmin(false) —  администратора снимать не с кого")
    void changeRoleAdmin_remove_error() {
        User existing = User.builder()
                .id(12L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();

        UserResponseDto existingDto = UserResponseDto.builder()
                .id(12L).username("vika")
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();

        when(userRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(userMapper.toDto(any(User.class))).thenReturn(existingDto);

        UserResponseDto res = userService.changeRoleAdmin(12L, false);

        Assertions.assertEquals("vika", res.getUsername());
        Assertions.assertEquals(1, res.getRoles().size());
        verify(userRepository, never()).save(any());

    }

    @Test
    @DisplayName("delete() — удаляет, если существует")
    void delete_ok() {
        when(userRepository.existsById(15L)).thenReturn(true);

        userService.delete(15L);

        verify(userRepository).deleteById(15L);
    }

    @Test
    @DisplayName("delete() — бросает, если не существует")
    void delete_notFound() {
        when(userRepository.existsById(16L)).thenReturn(false);

        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> userService.delete(16L)
        );

        verify(userRepository, never()).deleteById(anyLong());
    }
}