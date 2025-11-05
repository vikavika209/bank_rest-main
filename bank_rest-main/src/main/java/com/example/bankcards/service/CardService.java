package com.example.bankcards.service;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardNumberIsNotFree;
import com.example.bankcards.exception.UserNotFoundCustomException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper mapper;
    private final CryptoService cryptoService;

    @Value("${card.crypto.validity}")
    private final String validityMonths;

    @Transactional(readOnly = true)
    public CardResponseDto getById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + id));

        log.info("Получена карта с id = {}", id);
        return mapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public Page<CardResponseDto> getAll(Pageable pageable) {
        Page<Card> page = cardRepository.findAll(pageable);
        List<CardResponseDto> content = page
                .getContent()
                .stream()
                .map(mapper::toDto)
                .toList();

        log.info("Найдено карт: {}", content.size());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public CardResponseDto create(CardRequestDto cardRequestDto) {
        User user = userRepository.findById(cardRequestDto.getUserId())
                .orElseThrow(() -> new UserNotFoundCustomException("Пользователь не найден: " + cardRequestDto.getUserId()));

        String encrypted = cryptoService.encrypt(cardRequestDto.getCardNumber());

        if (cardRepository.existsByCardNumberEncrypted(encrypted).isPresent()) {
            throw new CardNumberIsNotFree("Карта уже существует: {}");
        }

        Card card = Card.builder()
                .balance(BigDecimal.ZERO)
                .expiryDate(cardRequestDto.getExpiryDate() == null
                        ? getExpiryDate()
                        : cardRequestDto.getExpiryDate()
                )
                .cardNumberEncrypted(encrypted)
                .user(user)
                .status(cardRequestDto.getStatus() == null
                        ? CardStatus.ACTIVE
                        : cardRequestDto.getStatus()
                )
                .build();

        Card saved = cardRepository.save(card);
        log.info("Сохранена карта: id = {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Transactional
    public CardResponseDto update(Long id, CardRequestDto dto) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + id));

        if (dto.getUserId() != null && !Objects.equals(dto.getUserId(), card.getUser().getId())) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new UserNotFoundCustomException("Пользователь не найден: " + dto.getUserId()));
            card.setUser(user);
            log.info("Пользователь обновлён: id = {}", user.getId());
        }

        applyIfChanged(dto.getExpiryDate(), card::getExpiryDate, card::setExpiryDate, "Срок действия обновлён: {}");
        applyIfChanged(dto.getBalance(),     card::getBalance,    card::setBalance,    "Баланс обновлён: {}");
        applyIfChanged(dto.getStatus(),      card::getStatus,     card::setStatus,     "Статус обновлён: {}");

        if (dto.getCardNumber() != null) {
            String currentPlain = cryptoService.decrypt(card.getCardNumberEncrypted());
            if (!Objects.equals(currentPlain, dto.getCardNumber())) {
                card.setCardNumberEncrypted(cryptoService.encrypt(dto.getCardNumber()));
                log.info("Номер карты обновлён");
            }
        }

        Card saved = cardRepository.save(card);
        log.info("Карта обновлена: id = {}", saved.getId());
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new CardNotFoundException("Карта не найдена: " + id);
        }
        cardRepository.deleteById(id);
        log.info("Карта удалена: id = {}", id);
    }

    public CardResponseDto block(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + id));
        card.setStatus(CardStatus.BLOCKED);

        Card save = cardRepository.save(card);
        log.info("Карта заблокирована: id = {}", save.getId());

        return mapper.toDto(save);
    }

    private LocalDate getExpiryDate() {
        int months = 36;
        try {
            months = Integer.parseInt(validityMonths);
        } catch (NumberFormatException e) {
            log.warn("Некорректное значение card.crypto.validity: '{}', используется по умолчанию {}", validityMonths, months);
        }
        return LocalDate.now().plusMonths(months);
    }

    private static <T> void applyIfChanged(T newVal, Supplier<T> getter, Consumer<T> setter, String logMsg) {
        if (newVal == null) return;
        T cur = getter.get();
        if (!Objects.equals(cur, newVal)) {
            setter.accept(newVal);
            log.info(logMsg, newVal);
        }
    }

}
