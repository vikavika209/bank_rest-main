package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.dto.PageCardResponseSchema;
import com.example.bankcards.util.AuthUtils;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cards", description = "Операции с картами. Требуется JWT. Админ-эндпоинты помечены /admin.")
public class CardController {

    private final CardService service;


    @Operation(
            summary = "Создать карту (ADMIN)",
            description = "Создаёт новую карту и возвращает её данные.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карта создана",
                            content = @Content(schema = @Schema(implementation = CardResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Валидационная ошибка",
                            content = @Content(schema = @Schema(example = "{\"message\":\"Bad Request\",\"detailedMessage\":\"...\"}"))),
                    @ApiResponse(responseCode = "401", description = "Неавторизован"),
                    @ApiResponse(responseCode = "403", description = "Нет прав")
            }
    )
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> createCard(
            @Valid @RequestBody CardCreateDto dto
    ) {
        log.info("Вызван createCard");
        return ResponseEntity.ok(service.create(dto));
    }


    @Operation(summary = "Получить карту по ID (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Ок",
            content = @Content(schema = @Schema(implementation = CardResponseDto.class)))
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> getCardById(
            @PathVariable("id") Long id
    ) {
        log.info("Вызван getCardById: {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(
            summary = "Список карт текущего пользователя (USER)",
            description = "Возвращает страницы с картами авторизованного пользователя."
    )
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (0..N)", example = "0"),
            @Parameter(name = "size", description = "Размер страницы", example = "20"),
            @Parameter(name = "sort", description = "Сортировка, например: id,desc", example = "id,desc")
    })
    @ApiResponse(responseCode = "200", description = "Ок",
            content = @Content(schema = @Schema(implementation = PageCardResponseSchema.class)))
    @GetMapping("/all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponseDto>> getAllByUserId(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван getAllByUserId userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(service.getAllByUserId(userId, pageable));
    }


    @Operation(
            summary = "Все карты (ADMIN)",
            description = "Постраничный список всех карт."
    )
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (0..N)", example = "0"),
            @Parameter(name = "size", description = "Размер страницы", example = "20"),
            @Parameter(name = "sort", description = "Сортировка, например: id,desc", example = "id,desc")
    })
    @ApiResponse(responseCode = "200", description = "Ок",
            content = @Content(schema = @Schema(implementation = PageCardResponseSchema.class)))
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponseDto>> getAllCards(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Вызван getAllCards");
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @Operation(summary = "Обновить карту по ID (ADMIN)")
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> updateCardById(
            @PathVariable("id") Long id,
            @Valid @RequestBody CardUpdateDto dto) {
        log.info("Вызван updateCardById: {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Блокировать карту (ADMIN)")
    @PatchMapping("/admin/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> blockCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.block(id));
    }

    @Operation(summary = "Разблокировать карту (ADMIN)")
    @PatchMapping("/admin/unblock/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> unblockCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван unblockCardById: {}", id);
        return ResponseEntity.ok(service.unblock(id));
    }

    @Operation(summary = "Активировать карту (ADMIN)")
    @PatchMapping("/admin/activate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> activateCardByIdForAdmin(
            @PathVariable Long id
    ){
        log.info("Вызван activateCardByIdForAdmin: {}", id);
        return ResponseEntity.ok(service.activate(id));
    }

    @Operation(summary = "Заблокировать свою карту (USER)")
    @PatchMapping("/block/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponseDto> blockCardByIdForUser(
            @PathVariable Long id
    ){
        Long userId = AuthUtils.currentUserId();
        log.info("Вызван blockCardById: {}", id);
        return ResponseEntity.ok(service.blockByUser(id, userId));
    }

    @Operation(summary = "Удалить карту по ID (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCardById(
            @PathVariable("id") Long id
    ){
        log.info("Вызван deleteCardById: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Перевод между своими картами (USER)",
            description = "Перевод средств между картами текущего пользователя."
    )
    @Parameters({
            @Parameter(name = "cardNumberFrom", description = "Откуда (16 цифр)", required = true, example = "5555444433332222"),
            @Parameter(name = "cardNumberTo", description = "Куда (16 цифр)", required = true, example = "4111111111111111"),
            @Parameter(name = "amount", description = "Сумма перевода", required = true, example = "250.00")
    })
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

    @Operation(summary = "Получить баланс по номеру карты (USER)")
    @Parameters({
            @Parameter(name = "cardNumber", description = "Номер карты (16 цифр)", required = true, example = "5555444433332222")
    })
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
