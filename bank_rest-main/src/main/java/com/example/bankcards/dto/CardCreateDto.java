package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardCreateDto {

    @Schema(description = "Номер карты (16 цифр)", example = "5333333333333333")
    @NotBlank(message = "Поле 'cardNumber' обязательно")
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    @Schema(description = "ID владельца карты", example = "1")
    @NotNull(message = "Поле 'userId' обязательно")
    @Positive(message = "userId должен быть положительным")
    private Long userId;
}
