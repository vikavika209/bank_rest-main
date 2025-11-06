package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService service;

    @PostMapping
    public ResponseEntity<CardResponseDto> createCard(
            @Valid @RequestBody CardCrateDto dto
    ) {
        log.info("Вызван createCard");
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDto> getCardById(
            @PathVariable("id") Long id
    ) {
        log.info("Вызван getCardById: {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CardResponseDto>> getAllCards(Pageable pageable) {
        log.info("Вызван getAllCards");
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponseDto> updateCardById(
            @PathVariable("id") Long id,
            @Valid @RequestBody CardUpdateDto dto) {
        log.info("Вызван updateCardById: {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PatchMapping("/admin/block/{id}")
    public ResponseEntity<CardResponseDto> blockCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.block(id));
    }

    @PatchMapping("/block/{id}")
    public ResponseEntity<CardResponseDto> blockCardByIdForUser(
            @PathVariable Long id,
            @RequestParam Long userId
    ){
        log.info("Вызван blockCardByIdForUser: {}", id);
        return ResponseEntity.ok(service.blockByUser(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCardById(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteCardById: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transfer")
    public ResponseEntity<String> transferBetweenUserCards(
            @RequestParam("cardNumberFrom") String cardNumberFrom,
            @RequestParam("cardNumberTo") String cardNumberTo,
            @RequestParam("userId") Long userId,
            @RequestParam("amount") BigDecimal amount
    ){
        log.info("Вызван transferBetweenUserCards");
        service.transferBetweenUserCards(userId, cardNumberFrom, cardNumberTo, amount);
        return ResponseEntity.ok("Перевод выполнен");
    }
}
