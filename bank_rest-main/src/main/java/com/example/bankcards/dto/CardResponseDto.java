package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "ID карты", example = "42")
    private Long id;

    @Schema(description = "Маскированный номер карты", example = "5333 **** **** 3333")
    private String maskedNumber;

    @Schema(description = "ID владельца", example = "1")
    private Long userId;

    @Schema(description = "Срок действия карты (YYYY-MM-DD)", example = "2028-11-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    @Schema(description = "Статус карты", example = "ACTIVE")
    private CardStatus status;

    @Schema(description = "Текущий баланс", example = "1999.99")
    private BigDecimal balance;
}
