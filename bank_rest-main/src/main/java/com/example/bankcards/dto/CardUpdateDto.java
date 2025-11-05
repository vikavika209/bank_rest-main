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
public class CardUpdateDto {
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    @Positive(message = "userId должен быть положительным")
    private Long userId;

    @Future(message = "Срок действия карты должен быть в будущем")
    private LocalDate expiryDate;

    private CardStatus status;

    @DecimalMin(value = "0.00", inclusive = true, message = "Баланс не может быть отрицательным")
    private BigDecimal balance;
}
