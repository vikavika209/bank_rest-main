package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardCrateDto {

    @NotBlank(message = "Поле 'cardNumber' обязательно")
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    @NotNull(message = "Поле 'userId' обязательно")
    @Positive(message = "userId должен быть положительным")
    private Long userId;
}
