package com.example.bankcards.util;

import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMapper {
    private final CryptoService cryptoService;

    public CardResponseDto toDto(Card card){
        return CardResponseDto.builder()
                .id(card.getId())
                .userId(card.getUser().getId())
                .balance(card.getBalance())
                .status(card.getStatus())
                .expiryDate(card.getExpiryDate())
                .maskedNumber(cryptoService.getMaskedNumber(card.getCardNumberEncrypted()))
                .build();
    }
}
