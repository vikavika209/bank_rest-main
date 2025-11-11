package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class UserRequestDto {

    @Schema(description = "Имя пользователя", example = "user")
    @NotBlank(message = "Поле 'username' не может быть пустым")
    private String username;

    @Schema(description = "Пароль (минимум 6 символов)", example = "secret123", minLength = 6)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Поле 'password' не может быть пустым")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
}
