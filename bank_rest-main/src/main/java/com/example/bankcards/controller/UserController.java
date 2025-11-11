package com.example.bankcards.controller;

import com.example.bankcards.dto.PageUserResponseSchema;
import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Управление пользователями")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService service;

    @Tag(name = "Users", description = "Управление пользователями")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> createUser (
            @RequestBody @Validated UserRequestDto userDto
    ){
        log.info("Called createUser: username = {}", userDto.getUsername());
        return ResponseEntity.ok(service.create(userDto));
    }

    @Operation(
            summary = "Получить пользователя по id",
            description = "Возвращает данные пользователя"
    )
    @ApiResponse(responseCode = "200", description = "Ок",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @GetMapping("/get/{id}")
    public ResponseEntity<UserResponseDto> getUser (
            @PathVariable("id") Long id
    ){
        log.info("Called getUser: id = {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(
            summary = "Список пользователей (пагинация)",
            description = "Возвращает страницу с пользователями"
    )
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (0..N)", example = "0"),
            @Parameter(name = "size", description = "Размер страницы", example = "20"),
            @Parameter(name = "sort", description = "Сортировка, например: id,desc", example = "id,desc")
    })
    @ApiResponse(responseCode = "200", description = "Ок",
            content = @Content(schema = @Schema(implementation = PageUserResponseSchema.class)))
    @GetMapping("/all")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable){
        log.info("Called getAllUsers");
        return ResponseEntity.ok(service.getAll(
                pageable.getPageNumber(),
                pageable.getPageSize())
        );
    }

    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет данные пользователя"
    )
    @ApiResponse(responseCode = "200", description = "Обновлено",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @PutMapping("update/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable("id") Long id,
            @RequestBody @Validated UserRequestDto userRequestDto
    ){
        log.info("Вызван updateUser: id = {}", id);
        return ResponseEntity.ok(service.update(id, userRequestDto));
    }

    @Operation(
            summary = "Заблокировать пользователя",
            description = "Переводит пользователя в недоступное состояние"
    )
    @ApiResponse(responseCode = "200", description = "Заблокирован",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @PutMapping("block/{id}")
    public ResponseEntity<UserResponseDto> blockUser(
            @PathVariable("id") Long id
    ){
        log.info("Вызван blockUser: id = {}", id);
        return ResponseEntity.ok(service.makeUnavailable(id));
    }

    @Operation(
            summary = "Назначить ADMIN",
            description = "Выдаёт пользователю роль ADMIN"
    )
    @ApiResponse(responseCode = "200", description = "Роль обновлена",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @PutMapping("admin/{id}")
    public ResponseEntity<UserResponseDto> makeAdmin(
            @PathVariable("id") Long id
    ){
        log.info("Вызван makeAdmin: id = {}", id);
        return ResponseEntity.ok(service.changeRoleAdmin(id, true));
    }

    @Operation(
            summary = "Снять ADMIN",
            description = "Убирает у пользователя роль ADMIN"
    )
    @ApiResponse(responseCode = "200", description = "Роль обновлена",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @PutMapping("not_admin/{id}")
    public ResponseEntity<UserResponseDto> depriveAdmin(
            @PathVariable("id") Long id
    ){
        log.info("Вызван depriveAdmin: id = {}", id);
        return ResponseEntity.ok(service.changeRoleAdmin(id, false));
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя по ID"
    )
    @ApiResponse(responseCode = "200", description = "Удалён", content = @Content)
    @ApiResponse(responseCode = "404", description = "Не найден", content = @Content)
    @DeleteMapping("delete/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteUser: id = {}", id);
        service.delete(id);
        return ResponseEntity.ok("Пользователь удалён: id = " + id);
    }
}
