package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class UserResponseDto {

    @Schema(description = "ID пользователя", example = "1")
    private Long id;

    @Schema(description = "Имя пользователя", example = "user")
    @NotBlank(message = "Поле 'username' не может быть пустым")
    private String username;

    @Schema(description = "Аккаунт активен", example = "true")
    private boolean enabled;

    @Schema(description = "Идентификаторы карт пользователя")
    private Set<Long> cardIds;

    @Schema(description = "Набор ролей", example = "[\"ROLE_USER\",\"ROLE_ADMIN\"]")
    private Set<Role> roles;
}
