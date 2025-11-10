package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity
class CardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService service;

    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private String json(String s) { return s; }

    @BeforeEach
    void passJwt() throws Exception {
        doAnswer(inv -> {
            var req = (jakarta.servlet.http.HttpServletRequest) inv.getArgument(0);
            var res = (jakarta.servlet.http.HttpServletResponse) inv.getArgument(1);
            var chain = (jakarta.servlet.FilterChain) inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("ADMIN: POST /api/cards/admin — 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_create_ok() throws Exception {
        String body = """
            {
              "userId": 10,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        CardResponseDto resp = CardResponseDto.builder()
                .id(100L).userId(10L).maskedNumber("**** **** **** 1111")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE).balance(BigDecimal.ZERO).build();

        when(service.create(any(CardCrateDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/cards/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("USER: POST /api/cards/admin — 403 Forbidden")
    @WithMockUser(username = "user", roles = {"USER"})
    void admin_create_forbidden_for_user() throws Exception {
        String body = """
            {
              "userId": 10,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/cards/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("ANON: POST /api/cards/admin — 403 Forbidden")
    void admin_create_forbidden() throws Exception {
        String body = """
            {
              "userId": 10,
              "cardNumber": "4111111111111111",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/cards/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("ADMIN: PATCH /api/cards/admin/block/{id} — 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_block_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(9L).status(CardStatus.BLOCKED).build();
        when(service.block(9L)).thenReturn(resp);

        mockMvc.perform(patch("/api/cards/admin/block/{id}", 9)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("USER: PATCH /api/cards/admin/block/{id} — 403 Forbidden")
    @WithMockUser(roles = "USER")
    void admin_block_forbidden_for_user() throws Exception {
        mockMvc.perform(patch("/api/cards/admin/block/{id}", 9))
                .andExpect(status().isForbidden());
        verify(service, never()).block(anyLong());
    }

    @Test
    @DisplayName("USER: GET /api/cards/balance — 200 OK")
    @WithMockUser(roles = "USER")
    void user_balance_ok() throws Exception {
        when(service.getBalance(10L, "4111111111111111"))
                .thenReturn(new BigDecimal("123.45"));

        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::currentUserId).thenReturn(10L);

            mockMvc.perform(get("/api/cards/balance")
                            .param("cardNumber", "4111111111111111"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("123.45"));
        }
    }

    @Test
    @DisplayName("ANON: GET /api/cards/balance — 401 Unauthorized")
    void user_balance_unauthorized() throws Exception {
        mockMvc.perform(get("/api/cards/balance")
                        .param("userId", "10")
                        .param("cardNumber", "4111111111111111"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).getBalance(anyLong(), anyString());
    }

    @Test
    @DisplayName("USER: PUT /api/cards/transfer — 200 OK")
    @WithMockUser(roles = "USER")
    void user_transfer_ok() throws Exception {

        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::currentUserId).thenReturn(10L);

            mockMvc.perform(put("/api/cards/transfer")
                            .param("userId", "10")
                            .param("cardNumberFrom", "4111111111111111")
                            .param("cardNumberTo", "4222222222222222")
                            .param("amount", "100.00")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Перевод выполнен"));

            verify(service).transferBetweenUserCards(10L, "4111111111111111", "4222222222222222", new BigDecimal("100.00"));
        }
    }

    @Test
    @DisplayName("ANON: PUT /api/cards/transfer — 403 Forbidden")
    void user_transfer_forbidden() throws Exception {
        mockMvc.perform(put("/api/cards/transfer")
                        .param("userId", "10")
                        .param("cardNumberFrom", "4111111111111111")
                        .param("cardNumberTo", "4222222222222222")
                        .param("amount", "100.00"))
                .andExpect(status().isForbidden());
        verify(service, never()).transferBetweenUserCards(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("USER: PATCH /api/cards/block/{id} — 200 OK")
    @WithMockUser(roles = "USER")
    void user_blockByUser_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(7L).userId(10L).status(CardStatus.BLOCKED)
                .maskedNumber("**** **** **** 1111")
                .balance(new BigDecimal("123.45")).build();

        when(service.blockByUser(7L, 10L)).thenReturn(resp);

        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::currentUserId).thenReturn(10L);

            mockMvc.perform(patch("/api/cards/block/{id}", 7)
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }
    }

    @Test
    @DisplayName("ANON: PATCH /api/cards/block/{id} — 403 Forbidden")
    void user_blockByUser_forbidden() throws Exception {
        mockMvc.perform(patch("/api/cards/block/{id}", 7)
                        .param("userId", "10"))
                .andExpect(status().isForbidden());
        verify(service, never()).blockByUser(anyLong(), anyLong());
    }

    @Test
    @DisplayName("USER: GET /api/cards/admin/{id} — 200 OK")
    @WithMockUser(roles = "ADMIN")
    void user_getById_ok() throws Exception {
        CardResponseDto resp = CardResponseDto.builder()
                .id(123L).userId(10L).maskedNumber("**** **** **** 1111")
                .status(CardStatus.ACTIVE).balance(BigDecimal.ZERO)
                .expiryDate(LocalDate.now().plusYears(3)).build();

        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::currentUserId).thenReturn(10L);

            when(service.getById(123L)).thenReturn(resp);

            mockMvc.perform(get("/api/cards/admin/{id}", 123))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(123));
        }
    }

    @Test
    @DisplayName("ANON: GET /api/cards/{id} — 401 Unauthorized")
    void anon_getById_unauthorized() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", 123))
                .andExpect(status().isUnauthorized());
        verify(service, never()).getById(anyLong());
    }

    @Test
    @DisplayName("ADMIN: GET /api/cards/admin — 200 OK")
    @WithMockUser(roles = "ADMIN")
    void admin_getAll_ok() throws Exception {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());
        List<CardResponseDto> content = List.of(
                CardResponseDto.builder().id(1L).userId(10L).build(),
                CardResponseDto.builder().id(2L).userId(11L).build()
        );
        Page<CardResponseDto> page = new PageImpl<>(content, pageable, 5);

        when(service.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards/admin?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("USER: GET /api/cards/admin — 403 Forbidden (админский список)")
    @WithMockUser(roles = "USER")
    void admin_getAll_forbidden_for_user() throws Exception {
        mockMvc.perform(get("/api/cards/admin?page=0&size=2&sort=id,asc"))
                .andExpect(status().isForbidden());
        verify(service, never()).getAll(any());
    }
}