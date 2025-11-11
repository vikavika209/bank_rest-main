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
import org.springframework.scheduling.annotation.Scheduled;
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
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper mapper;
    private final CryptoService cryptoService;
    private final String validityMonths;

    public CardService(CardRepository cardRepository,
                       UserRepository userRepository,
                       CardMapper mapper,
                       CryptoService cryptoService,
                       @Value("${card.crypto.validity}") String validityMonths) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.cryptoService = cryptoService;
        this.validityMonths = validityMonths;
    }

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
        Card card = cardRepository.findByIdAndStatus(id,  CardStatus.ACTIVE)
                .orElseThrow(() -> new CardNotFoundException("Активная карта не найдена: " + id));

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

    public CardResponseDto unblock(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + id));
        card.setStatus(CardStatus.ACTIVE);

        Card save = cardRepository.save(card);
        log.info("Карта разблокирована: id = {}", save.getId());

        return mapper.toDto(save);
    }

    public CardResponseDto activate(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + id));
        card.setStatus(CardStatus.ACTIVE);

        Card save = cardRepository.save(card);
        log.info("Карта активирована: id = {}", save.getId());

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
        Page<CardResponseDto> page = cards.map(mapper::toDto);

        log.info("Найдено карт (всего): {} для userId={}; на странице: {}",
                cards.getTotalElements(), userId, page.getNumberOfElements());
        return page;
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
        log.info("Запрошен перевод с карты: " +
                "номер = {} " +
                "на карту: номер = {}; " +
                "сумма = {}; " +
                "пользователь: id = {}",
                cardNumberFrom, cardNumberTo, amount, userId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("Сумма перевода должна быть больше 0");
        }
        if (Objects.equals(cardNumberFrom, cardNumberTo)) {
            throw new TransferException("Нельзя переводить на ту же самую карту");
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundCustomException("Пользователь не найден: " + userId));
        log.info("Пользователь найден: id = {}", user.getId());

        String encFrom = cryptoService.encrypt(cardNumberFrom);
        log.info("Зашифровали номер from: {}", encFrom);
        String encTo = cryptoService.encrypt(cardNumberTo);
        log.info("Зашифровали номер to: {}", encTo);

        Card cardFrom = cardRepository.findByCardNumberEncryptedAndUser_Id(encFrom, userId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cryptoService.getMaskedNumber(encFrom)));
        log.info("Найдена cardFrom: id = {}", cardFrom.getId());

        Card cardTo = cardRepository.findByCardNumberEncryptedAndUser_Id(encTo, userId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена: " + cryptoService.getMaskedNumber(encTo)));
        log.info("Найдена cardTo: id = {}", cardTo.getId());


        Card lowId = cardFrom.getId() < cardTo.getId() ? cardFrom : cardTo;
        log.info("Присвоено lowId: id = {}", lowId.getId());

        Card highId = lowId.equals(cardFrom) ? cardTo : cardFrom;
        log.info("Присвоено highId: id = {}", highId.getId());

        Card cardFirstLocked = cardRepository.lockByIdAndUserAndStatus(lowId.getId(), userId, CardStatus.ACTIVE)
                .orElseThrow(() -> new CardNotFoundException("Активная карта не найдена: id = " + lowId.getId()));
        log.info("Присвоено cardFirstLocked: id = {}", cardFirstLocked.getId());

        Card cardSecondLocked = cardRepository.lockByIdAndUserAndStatus(highId.getId(), userId, CardStatus.ACTIVE)
                .orElseThrow(() -> new CardNotFoundException("Активная карта не найдена: id = " + highId.getId()));
        log.info("Присвоено cardSecondLocked: id = {}", cardSecondLocked.getId());

        cardFrom = cardFirstLocked.getId().equals(cardFrom.getId()) ? cardFirstLocked : cardSecondLocked;
        log.info("Присвоено cardFrom: id = {}", cardFrom.getId());

        cardTo = cardFrom.equals(cardFirstLocked) ? cardSecondLocked : cardFirstLocked;
        log.info("Присвоено cardTo: id = {}", cardTo.getId());

        if (cardFrom.getBalance().compareTo(amount) < 0) {
            String maskedNumber = cryptoService.getMaskedNumber(encFrom);
            log.info("После проверки баланса maskedNumber = {}", maskedNumber);
            throw new TransferException("Недостаточно средств на карте " + maskedNumber);
        }

        cardFrom.setBalance(cardFrom.getBalance().subtract(amount));
        log.info("Новый баланс карты: id = {} равен {}", cardFrom.getId(), cardFrom.getBalance());

        cardTo.setBalance(cardTo.getBalance().add(amount));
        log.info("Новый баланс карты: id = {} равен {}", cardTo.getId(), cardTo.getBalance());

        Card savedFrom = cardRepository.save(cardFrom);
        log.info("Успешное сохранение карты: id = {}", savedFrom.getId());

        Card savedTo = cardRepository.save(cardTo);
        log.info("Успешное сохранение карты: id = {}", savedTo.getId());

        log.info("Перевод {} выполнен: from {} -> to {}; новые балансы: from={}, to={}",
                amount,
                cryptoService.getMaskedNumber(encFrom), cryptoService.getMaskedNumber(encTo),
                cardFrom.getBalance(), cardTo.getBalance());
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void setExpired(){
        List<CardStatus> statuses = List.of(CardStatus.ACTIVE, CardStatus.BLOCKED);
        int updated = cardRepository.markExpiredForStatuses(CardStatus.EXPIRED, statuses);
        log.info("Помечено как EXPIRED: {} карт", updated);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId, String cardNumber){
        log.info("Получен номер карты: {} для пользователя: id = {}", cardNumber, userId);
        String cardNumberEncrypted = cryptoService.encrypt(cardNumber);
        log.info("Зашифровали номер: {}", cardNumberEncrypted);
        String cardNumberMasked = cryptoService.getMaskedNumber(cardNumberEncrypted);
        Card card = cardRepository
                .findByCardNumberEncryptedAndUser_IdAndStatus(cardNumberEncrypted, userId, CardStatus.ACTIVE)
                .orElseThrow(
                        () -> new CardNotFoundException("Карта не найдена: " + cardNumberMasked)
                );
        return card.getBalance();
    }
}
