package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
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
    private Long id;

    @NotBlank(message = "Поле 'username' не может быть пустым")
    private String username;

    private boolean enabled;
    private Set<Long> cardIds;
    private Set<Role> roles;
}
