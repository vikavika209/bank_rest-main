package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Новый номер карты (16 цифр)", example = "5444444444444444")
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    @Schema(description = "Новый владелец (userId)", example = "2")
    @Positive(message = "userId должен быть положительным")
    private Long userId;

    @Schema(description = "Новый срок действия (YYYY-MM-DD)", example = "2029-05-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Future(message = "Срок действия карты должен быть в будущем")
    private LocalDate expiryDate;

    @Schema(description = "Новый статус", example = "BLOCKED")
    private CardStatus status;

    @Schema(description = "Новый баланс", example = "0.00", minimum = "0.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "Баланс не может быть отрицательным")
    private BigDecimal balance;
}
