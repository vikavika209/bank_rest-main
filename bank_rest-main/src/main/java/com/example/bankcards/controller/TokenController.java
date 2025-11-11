package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/api/token")
@RequiredArgsConstructor
@Slf4j
@RestController
@Tag(name = "Auth", description = "Аутентификация и выдача JWT токена")
public class TokenController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;


    @Operation(
            summary = "Аутентификация и выдача JWT токена",
            description = "Принимает username и password, проверяет их и возвращает JWT-токен",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                            content = @Content(schema = @Schema(
                                    example = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
                            ))),
                    @ApiResponse(responseCode = "401", description = "Неверные имя пользователя или пароль",
                            content = @Content(schema = @Schema(
                                    example = "{\"message\": \"Unauthorized\", \"detailedMessage\": \"Bad credentials\"}"
                            )))
            }
    )
    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        String token = jwtService.generateToken(request.getUsername());
        log.info("Выдан токен пользователю: {}", request.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
