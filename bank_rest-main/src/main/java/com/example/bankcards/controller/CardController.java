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

    @PatchMapping("/{id}/block")
    public ResponseEntity<CardResponseDto> blockCardById(
            @PathVariable Long id
    ){
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.block(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCardById(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteCardById: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
