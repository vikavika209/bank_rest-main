package com.example.bankcards.util;

import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                user.getCards().stream()
                        .map(card -> card.getId())
                        .collect(Collectors.toSet()),
                user.getRoles()
        );
    }
}
