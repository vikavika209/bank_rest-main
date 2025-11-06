package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.*;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService service;

    private CardCrateDto validReq() {
        CardCrateDto dto = new CardCrateDto();
        dto.setUserId(10L);
        dto.setCardNumber("4111111111111111");
        return dto;
    }

    private String asJson(String s) { return s; }

    @Test
    @DisplayName("POST /api/cards — 200 OK при успешном создании")
    void create_ok() throws Exception {
        CardCrateDto req = validReq();

        CardResponseDto resp = CardResponseDto.builder()
                .id(100L)
                .userId(10L)
                .maskedNumber("**** **** **** 1111")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        when(service.create(any(CardCrateDto.class))).thenReturn(resp);

        String body = """
            {
              "userId": 10,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.userId", is(10)))
                .andExpect(jsonPath("$.maskedNumber", is("**** **** **** 1111")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(service).create(any(CardCrateDto.class));
    }

    @Test
    @DisplayName("POST /api/cards — 500 INTERNAL SERVER ERROR при невалидном теле (Bean Validation)")
    void create_validationError() throws Exception {

        String invalidBody = """
            {
              "cardNumber": "not-a-number"
            }
            """;

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/cards — 409 CONFLICT если номер карты уже занят")
    void create_conflict_cardNumberBusy() throws Exception {
        when(service.create(any(CardCrateDto.class))).thenThrow(new CardNumberIsNotFree("Карта уже существует"));

        String body = """
            {
              "userId": 10,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Номер карты не свободен")))
                .andExpect(jsonPath("$.detailedMessage", containsString("Карта уже существует")));
    }

    @Test
    @DisplayName("POST /api/cards — 404 NOT_FOUND если пользователь не найден (проксируется GlobalExceptionHandler)")
    void create_userNotFound() throws Exception {
        when(service.create(any(CardCrateDto.class)))
                .thenThrow(new UserNotFoundCustomException("Пользователь не найден: 999"));

        String body = """
            {
              "userId": 999,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Пользователь не найден")))
                .andExpect(jsonPath("$.detailedMessage", containsString("999")));
    }

    @Test
    @DisplayName("GET /api/cards/{id} — 200 OK при наличии карты")
    void getById_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(123L)
                .userId(10L)
                .maskedNumber("**** **** **** 1111")
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .expiryDate(LocalDate.now().plusYears(3))
                .build();

        when(service.getById(123L)).thenReturn(resp);

        mockMvc.perform(get("/api/cards/{id}", 123))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(123)))
                .andExpect(jsonPath("$.userId", is(10)));
    }

    @Test
    @DisplayName("GET /api/cards/{id} — 404 если карта не найдена")
    void getById_notFound() throws Exception {
        when(service.getById(777L)).thenThrow(new CardNotFoundException("Карта не найдена: 777"));

        mockMvc.perform(get("/api/cards/{id}", 777))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Карта не найдена")))
                .andExpect(jsonPath("$.detailedMessage", containsString("777")));
    }

    @Test
    @DisplayName("GET /api/cards — 200 OK с пагинацией")
    void getAll_ok() throws Exception {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());
        List<CardResponseDto> content = List.of(
                CardResponseDto.builder().id(1L).userId(10L).build(),
                CardResponseDto.builder().id(2L).userId(11L).build()
        );
        Page<CardResponseDto> page = new PageImpl<>(content, pageable, 5);

        when(service.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("PUT /api/cards/{id} — 200 OK при успешном обновлении")
    void update_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(5L)
                .userId(20L)
                .status(CardStatus.BLOCKED)
                .build();

        when(service.update(eq(5L), any(CardUpdateDto.class))).thenReturn(resp);

        String body = """
            {
              "userId": 20,
              "status": "BLOCKED",
              "cardNumber": "1111111111111111"
            }
            """;

        mockMvc.perform(put("/api/cards/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.userId", is(20)))
                .andExpect(jsonPath("$.status", is("BLOCKED")));
    }

    @Test
    @DisplayName("PUT /api/cards/{id} — 404 если карта не найдена")
    void update_notFound() throws Exception {
        when(service.update(eq(5L), any(CardUpdateDto.class)))
                .thenThrow(new CardNotFoundException("Карта не найдена: 5"));

        String body = """
            {
              "userId": 20
            }
            """;

        mockMvc.perform(put("/api/cards/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Карта не найдена")))
                .andExpect(jsonPath("$.detailedMessage", containsString("5")));
    }

    @Test
    @DisplayName("PATCH /api/cards/admin/block/{id} — 200 OK при успешной блокировке")
    void block_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(9L)
                .status(CardStatus.BLOCKED)
                .build();

        when(service.block(9L)).thenReturn(resp);

        mockMvc.perform(patch("/api/cards/admin/block/{id}", 9))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(9)))
                .andExpect(jsonPath("$.status", is("BLOCKED")));
    }

    @Test
    @DisplayName("PATCH /api/cards/admin/block/{id} — 404 если карта не найдена")
    void block_notFound() throws Exception {
        when(service.block(9L)).thenThrow(new CardNotFoundException("Карта не найдена: 9"));

        mockMvc.perform(patch("/api/cards/admin/block/{id}", 9))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Карта не найдена")))
                .andExpect(jsonPath("$.detailedMessage", containsString("9")));
    }

    @Test
    @DisplayName("DELETE /api/cards/{id} — 204 No Content при успешном удалении")
    void delete_ok() throws Exception {
        doNothing().when(service).delete(11L);

        mockMvc.perform(delete("/api/cards/{id}", 11))
                .andExpect(status().isNoContent());

        verify(service).delete(11L);
    }

    @Test
    @DisplayName("DELETE /api/cards/{id} — 404 если карта не найдена")
    void delete_notFound() throws Exception {
        doThrow(new CardNotFoundException("Карта не найдена: 11"))
                .when(service).delete(11L);

        mockMvc.perform(delete("/api/cards/{id}", 11))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Карта не найдена")))
                .andExpect(jsonPath("$.detailedMessage", containsString("11")));
    }

    @Test
    @DisplayName("Успешный перевод — статус 200")
    void transfer_success() throws Exception {

        Long userId = 10L;
        String from = "4111111111111111";
        String to   = "4222222222222222";
        BigDecimal amount = new BigDecimal("100.00");

        mockMvc.perform(put("/api/cards/transfer")
                        .param("userId", String.valueOf(userId))
                        .param("cardNumberFrom", from)
                        .param("cardNumberTo", to)
                        .param("amount", amount.toPlainString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Перевод выполнен"));

        Mockito.verify(service).transferBetweenUserCards(userId, from, to, amount);
    }

    @Test
    @DisplayName("Ошибка перевода — сервис выбрасывает TransferException, возвращается 400")
    void transfer_error() throws Exception {
        Long userId = 10L;
        String from = "4111111111111111";
        String to   = "4222222222222222";
        BigDecimal amount = new BigDecimal("100.00");

        Mockito.doThrow(new TransferException("Недостаточно средств"))
                .when(service)
                .transferBetweenUserCards(anyLong(), anyString(), anyString(), any(BigDecimal.class));

        mockMvc.perform(put("/api/cards/transfer")
                        .param("userId", String.valueOf(userId))
                        .param("cardNumberFrom", from)
                        .param("cardNumberTo", to)
                        .param("amount", amount.toPlainString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ошибка во время перевода денежных средств"));
    }

    @Test
    @DisplayName("PATCH /block/{id} — успех 200 и корректный JSON")
    void block_success() throws Exception {
        long cardId = 7L;
        long userId = 10L;

        CardResponseDto resp = new CardResponseDto();
        resp.setId(cardId);
        resp.setUserId(userId);
        resp.setStatus(CardStatus.BLOCKED);
        resp.setMaskedNumber("**** **** **** 1111");
        resp.setBalance( new BigDecimal("123.45"));

        Mockito.when(service.blockByUser(cardId, userId)).thenReturn(resp);

        mockMvc.perform(patch("/api/cards/block/{id}", cardId)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) cardId))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.balance").value(123.45))
                .andExpect(jsonPath("$.userId").value((int) userId));

        verify(service).blockByUser(cardId, userId);
    }

    @Test
    @DisplayName("PATCH /block/{id} — 404 если карта не найдена")
    void blockByUser_notFound() throws Exception {
        long cardId = 999L;
        long userId = 10L;

        Mockito.when(service.blockByUser(anyLong(), anyLong()))
                .thenThrow(new CardNotFoundException("Карта не найдена: " + cardId));

        mockMvc.perform(patch("/api/cards/block/{id}", cardId)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Карта не найдена"));
    }
}