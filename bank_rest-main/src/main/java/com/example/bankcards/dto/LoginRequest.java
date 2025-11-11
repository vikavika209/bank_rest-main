package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequest {

    @Schema(description = "Имя пользователя", example = "admin")
    private String username;

    @Schema(description = "Пароль", example = "123456")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}

