package com.example.bankcards.service;


import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardNumberIsNotFree;
import com.example.bankcards.exception.UserNotFoundCustomException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper mapper;

    @Mock
    private CryptoService cryptoService;

    private CardService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new CardService(cardRepository, userRepository, mapper, cryptoService, "36");
    }

    private Card stubCard(Long id, Long userId) {
        User u = new User();
        u.setId(userId);

        Card c = new Card();
        c.setId(id);
        c.setUser(u);
        c.setBalance(BigDecimal.ZERO);
        c.setStatus(CardStatus.ACTIVE);
        c.setExpiryDate(LocalDate.now().plusMonths(36));
        c.setCardNumberEncrypted("enc#123");
        return c;
    }


    @Test
    @DisplayName("getById — возвращает DTO, если карта найдена")
    void getById_ok() {
        Card card = stubCard(1L, 10L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(mapper.toDto(card)).thenReturn(new CardResponseDto());

        CardResponseDto dto = service.getById(1L);

        assertThat(dto).isNotNull();
        verify(cardRepository).findById(1L);
        verify(mapper).toDto(card);
    }

    @Test
    @DisplayName("getById — бросает CardNotFoundException, если не найдена")
    void getById_notFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getAll — мапит страницы корректно")
    void getAll_ok() {
        Pageable pageable = PageRequest.of(1, 2, Sort.by("id").ascending());
        Card c1 = stubCard(1L, 10L);
        Card c2 = stubCard(2L, 10L);
        Page<Card> page = new PageImpl<>(List.of(c1, c2), pageable, 5);

        when(cardRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toDto(c1)).thenReturn(new CardResponseDto());
        when(mapper.toDto(c2)).thenReturn(new CardResponseDto());

        Page<CardResponseDto> result = service.getAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);

        verify(cardRepository).findAll(pageable);
        verify(mapper).toDto(c1);
        verify(mapper).toDto(c2);
    }

    @Test
    @DisplayName("create — успешно создает карту: шифрует номер, устанавливает expiry по validity, баланс=0, статус по умолчанию ACTIVE")
    void create_ok() {
        CardCrateDto req = CardCrateDto.builder()
                .cardNumber("4111111111111111")
                .userId(10L)
                .build();

        User u = new User(); u.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(u));
        when(cryptoService.encrypt("4111111111111111")).thenReturn("enc#4111");
        when(cardRepository.existsByCardNumberEncrypted("enc#4111")).thenReturn(Optional.empty());

        ArgumentCaptor<Card> toSave = ArgumentCaptor.forClass(Card.class);
        Card saved = stubCard(100L, 10L);
        when(cardRepository.save(toSave.capture())).thenReturn(saved);

        when(mapper.toDto(saved)).thenReturn(new CardResponseDto());

        CardResponseDto dto = service.create(req);

        assertThat(dto).isNotNull();

        Card entity = toSave.getValue();
        assertThat(entity.getUser().getId()).isEqualTo(10L);
        assertThat(entity.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(entity.getCardNumberEncrypted()).isEqualTo("enc#4111");
        assertThat(entity.getExpiryDate()).isAfter(LocalDate.now().plusMonths(35));

        verify(userRepository).findById(10L);
        verify(cryptoService).encrypt("4111111111111111");
        verify(cardRepository).existsByCardNumberEncrypted("enc#4111");
        verify(cardRepository).save(any(Card.class));
        verify(mapper).toDto(saved);
    }

    @Test
    @DisplayName("create — бросает CardNumberIsNotFree, если номер уже занят")
    void create_duplicateNumber() {
        CardCrateDto req = CardCrateDto.builder()
                .cardNumber("4111111111111111")
                .userId(10L)
                .build();

        User u = new User(); u.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(u));
        when(cryptoService.encrypt("4111111111111111")).thenReturn("enc#4111");
        when(cardRepository.existsByCardNumberEncrypted("enc#4111"))
                .thenReturn(Optional.of(new Card()));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(CardNumberIsNotFree.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — бросает UserNotFoundCustomException, если userId неизвестен")
    void create_userNotFound() {
        CardCrateDto req = CardCrateDto.builder()
                .userId(999L)
                .cardNumber("4111111111111111")
                .build();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(UserNotFoundCustomException.class);
    }

    @Test
    @DisplayName("update — обновляет user, поля и номер карты (шифрование при изменении)")
    void update_ok_changesUserAndNumber() {
        Card existing = stubCard(5L, 10L);
        when(cardRepository.findById(5L)).thenReturn(Optional.of(existing));

        User newUser = new User(); newUser.setId(20L);

        CardUpdateDto dto = new CardUpdateDto();
        dto.setUserId(20L);
        dto.setStatus(CardStatus.BLOCKED);
        dto.setBalance(new BigDecimal("123.45"));
        dto.setExpiryDate(LocalDate.now().plusYears(1));
        dto.setCardNumber("5555444433332222");

        when(userRepository.findById(20L)).thenReturn(Optional.of(newUser));

        when(cryptoService.decrypt("enc#123")).thenReturn("4111111111111111");
        when(cryptoService.encrypt("5555444433332222")).thenReturn("enc#5555");

        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(Card.class))).thenReturn(new CardResponseDto());

        CardResponseDto out = service.update(5L, dto);

        assertThat(out).isNotNull();
        assertThat(existing.getUser().getId()).isEqualTo(20L);
        assertThat(existing.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(existing.getBalance()).isEqualByComparingTo("123.45");
        assertThat(existing.getExpiryDate()).isEqualTo(dto.getExpiryDate());
        assertThat(existing.getCardNumberEncrypted()).isEqualTo("enc#5555");

        verify(cryptoService).decrypt("enc#123");
        verify(cryptoService).encrypt("5555444433332222");
        verify(cardRepository).save(existing);
        verify(mapper).toDto(existing);
    }

    @Test
    @DisplayName("update — бросает CardNotFoundException, если карта не найдена")
    void update_notFound() {
        when(cardRepository.findById(777L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(777L, new CardUpdateDto()))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    @DisplayName("delete — бросает CardNotFoundException, если id не существует")
    void delete_notFound() {
        when(cardRepository.existsById(111L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(111L))
                .isInstanceOf(CardNotFoundException.class);

        verify(cardRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete — удаляет, если существует")
    void delete_ok() {
        when(cardRepository.existsById(11L)).thenReturn(true);

        service.delete(11L);

        verify(cardRepository).deleteById(11L);
    }

    @Test
    @DisplayName("block — переводит в BLOCKED и сохраняет")
    void block_ok() {
        Card existing = stubCard(9L, 10L);
        when(cardRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(new CardResponseDto());

        CardResponseDto dto = service.block(9L);

        assertThat(dto).isNotNull();
        assertThat(existing.getStatus()).isEqualTo(CardStatus.BLOCKED);

        verify(cardRepository).save(existing);
        verify(mapper).toDto(existing);
    }

    @Test
    @DisplayName("block — бросает CardNotFoundException, если нет карты")
    void block_notFound() {
        when(cardRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.block(9L))
                .isInstanceOf(CardNotFoundException.class);
    }
}