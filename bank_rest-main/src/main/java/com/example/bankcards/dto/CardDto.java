package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import jakarta.persistence.*;
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
public class CardDto {
    private Long id;

    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    private String maskedNumber;

    @NotBlank(message = "Поле 'userId' не может быть пустым")
    private Long userId;

    @NotNull(message = "Поле 'expiryDate' обязательно")
    @Future(message = "Срок действия карты должен быть в будущем")
    private LocalDate expiryDate;

    @NotNull(message = "Поле 'status' обязательно")
    private CardStatus status;

    @NotNull(message = "Поле 'balance' обязательно")
    @DecimalMin(value = "0.00", inclusive = true, message = "Баланс не может быть отрицательным")
    private BigDecimal balance;
}
