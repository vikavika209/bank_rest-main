package com.example.bankcards.service;

import com.example.bankcards.dto.CardCrateDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardUpdateDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
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
import java.math.RoundingMode;
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

    public CardResponseDto create(CardCrateDto cardCrateDto) {
        User user = userRepository.findById(cardCrateDto.getUserId())
                .orElseThrow(() -> new UserNotFoundCustomException("Пользователь не найден: " + cardCrateDto.getUserId()));

        String encrypted = cryptoService.encrypt(cardCrateDto.getCardNumber());

        if (cardRepository.findByCardNumberEncrypted(encrypted).isPresent()) {
            throw new CardNumberIsNotFree("Карта уже существует: {}");
        }

        Card card = Card.builder()
                .balance(BigDecimal.ZERO)
                .expiryDate(getExpiryDate())
                .cardNumberEncrypted(encrypted)
                .user(user)
                .status(CardStatus.ACTIVE)
                .build();

        Card saved = cardRepository.save(card);
        log.info("Сохранена карта: id = {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Transactional
    public CardResponseDto update(Long id, CardUpdateDto dto) {
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

    public CardResponseDto blockByUser(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cardId));

        verify(cardId, userId);

        card.setStatus(CardStatus.BLOCKED);

        Card save = cardRepository.save(card);
        log.info("Карта: id = {} заблокирована пользователем: id = {}", save.getId(), userId);

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

    public Page<CardResponseDto> getAllByUserId(Long userId, Pageable pageable){
        Page<Card> cards = cardRepository.findByUser_Id(userId, pageable);

        List<CardResponseDto> content = cards
                .getContent()
                .stream()
                .map(mapper::toDto)
                .toList();

        log.info("Найдено карт: {} для пользователя: id = {}", content.size(), userId);
        return new PageImpl<>(content, pageable, cards.getTotalElements());
    }

    private void verify(Long cardId, Long userId){
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cardId));

        if (card.getUser().getId().equals(userId)){
            log.info("Карта: id = {} принадлежит пользователю: id = {}", cardId, userId);
        }

        else {
            log.error("Карта: id = {} не принадлежит пользователю: id = {}", cardId, userId);
            throw new NotVerifyException("Карта не принадлежит пользователю");
        }
    }

    @Transactional
    public void transferBetweenUserCards(
            Long userId,
            String cardNumberFrom,
            String cardNumberTo,
            BigDecimal amount
    ) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("Сумма перевода должна быть больше 0");
        }
        if (Objects.equals(cardNumberFrom, cardNumberTo)) {
            throw new TransferException("Нельзя переводить на ту же самую карту");
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundCustomException("Пользователь не найден: " + userId));

        String encFrom = cryptoService.encrypt(cardNumberFrom);
        String encTo = cryptoService.encrypt(cardNumberTo);

        Card cardFrom = cardRepository.findByCardNumberEncryptedAndUser_Id(encFrom, userId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cryptoService.getMaskedNumber(cardNumberFrom)));
        Card cardTo = cardRepository.findByCardNumberEncryptedAndUser_Id(encTo, userId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cryptoService.getMaskedNumber(cardNumberTo)));

        Card lowId = cardFrom.getId() < cardTo.getId() ? cardFrom : cardTo;
        Card highId = lowId.equals(cardFrom) ? cardTo : cardFrom;

        Card cardFirstLocked = cardRepository.lockByIdAndUserAndStatus(lowId.getId(), userId, CardStatus.ACTIVE)
                .orElseThrow(() -> new CardNotFoundException("Активная карта не найдена: id = " + lowId.getId()));
        Card cardSecondLocked = cardRepository.lockByIdAndUserAndStatus(highId.getId(), userId, CardStatus.ACTIVE)
                .orElseThrow(() -> new CardNotFoundException("Активная карта не найдена: id = " + highId.getId()));

        cardFrom = cardFirstLocked.getId().equals(cardFrom.getId()) ? cardFirstLocked : cardSecondLocked;
        cardTo = cardFrom.equals(cardFirstLocked) ? cardSecondLocked : cardFirstLocked;

        if (cardFrom.getBalance().compareTo(amount) < 0) {
            String maskedNumber = cryptoService.getMaskedNumber(cardNumberFrom);
            throw new TransferException("Недостаточно средств на карте " + maskedNumber);
        }

        cardFrom.setBalance(cardFrom.getBalance().subtract(amount));
        cardTo.setBalance(cardTo.getBalance().add(amount));

        cardRepository.save(cardFrom);
        cardRepository.save(cardTo);

        log.info("Перевод {} выполнен: from {} -> to {}; новые балансы: from={}, to={}",
                amount,
                cryptoService.getMaskedNumber(cardNumberFrom), cryptoService.getMaskedNumber(cardNumberTo),
                cardFrom.getBalance(), cardTo.getBalance());
    }
}
