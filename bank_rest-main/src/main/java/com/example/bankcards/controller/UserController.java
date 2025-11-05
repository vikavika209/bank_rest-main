package com.example.bankcards.controller;

import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> createUser (
            @RequestBody @Validated UserRequestDto userDto
    ){
        log.info("Called createUser: username = {}", userDto.getUsername());
        return ResponseEntity.ok(service.create(userDto));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<UserResponseDto> getUser (
            @PathVariable("id") Long id
    ){
        log.info("Called getUser: id = {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable){
        log.info("Called getAllUsers");
        return ResponseEntity.ok(service.getAll(
                pageable.getPageNumber(),
                pageable.getPageSize())
        );
    }

    @PutMapping("update/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable("id") Long id,
            @RequestBody @Validated UserRequestDto userRequestDto
    ){
        log.info("Вызван updateUser: id = {}", id);
        return ResponseEntity.ok(service.update(id, userRequestDto));
    }

    @PutMapping("block/{id}")
    public ResponseEntity<UserResponseDto> blockUser(
            @PathVariable("id") Long id
    ){
        log.info("Вызван blockUser: id = {}", id);
        return ResponseEntity.ok(service.makeUnavailable(id));
    }

    @PutMapping("admin/{id}")
    public ResponseEntity<UserResponseDto> makeAdmin(
            @PathVariable("id") Long id
    ){
        log.info("Вызван makeAdmin: id = {}", id);
        return ResponseEntity.ok(service.changeRoleAdmin(id, true));
    }

    @PutMapping("not_admin/{id}")
    public ResponseEntity<UserResponseDto> depriveAdmin(
            @PathVariable("id") Long id
    ){
        log.info("Вызван depriveAdmin: id = {}", id);
        return ResponseEntity.ok(service.changeRoleAdmin(id, false));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteUser: id = {}", id);
        service.delete(id);
        return ResponseEntity.ok("Пользователь удалён: id = " + id);
    }
}
