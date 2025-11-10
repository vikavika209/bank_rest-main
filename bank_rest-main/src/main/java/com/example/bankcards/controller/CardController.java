package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.util.AuthUtils;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService service;

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> createCard(
            @Valid @RequestBody CardCrateDto dto
    ) {
        log.info("Вызван createCard");
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> getCardById(
            @PathVariable("id") Long id
    ) {
        log.info("Вызван getCardById: {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/all/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponseDto>> getAllByUserId(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван getAllByUserId userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(service.getAllByUserId(userId, pageable));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponseDto>> getAllCards(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Вызван getAllCards");
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> updateCardById(
            @PathVariable("id") Long id,
            @Valid @RequestBody CardUpdateDto dto) {
        log.info("Вызван updateCardById: {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PatchMapping("/admin/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> blockCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.block(id));
    }

    @PatchMapping("/admin/activate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> activateCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван activateCardByIdForAdmin: {}", id);
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/block/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponseDto> blockCardByIdForUser(
            @PathVariable Long id
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.blockByUser(id, userId));
    }

    @DeleteMapping("/admin/{id}  ")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCardById(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteCardById: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> transferBetweenUserCards(
            @RequestParam("cardNumberFrom")
            @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
            String cardNumberFrom,
            @RequestParam("cardNumberTo")
            @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
            String cardNumberTo,
            @RequestParam("amount") BigDecimal amount
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван transferBetweenUserCards");
        service.transferBetweenUserCards(userId, cardNumberFrom, cardNumberTo, amount);
        return ResponseEntity.ok("Перевод выполнен");
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getBalance (
            @RequestParam
            @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
            String cardNumber
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван getBalance");
        return ResponseEntity.ok(service.getBalance(userId, cardNumber));
    }
}
