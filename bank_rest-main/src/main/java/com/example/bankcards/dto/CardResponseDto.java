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
public class CardResponseDto {
    private Long id;
    private String maskedNumber;
    private Long userId;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
}
