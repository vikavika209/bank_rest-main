package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {


    @Query("""
    SELECT c FROM Card c
    WHERE c.id = :cardId
      AND c.status = :status
""")
    Optional<Card> findByIdAndStatus(
            @Param("cardId") Long cardId,
            @Param("status") CardStatus status);

    Page<Card> findByUser_Id(Long id, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    UPDATE Card c
    SET c.status = :expired
    WHERE c.status IN (:statuses)
      AND c.expiryDate < CURRENT_DATE
""")
    int markExpiredForStatuses(
            @Param("expired") CardStatus expired,
            @Param("statuses") Collection<CardStatus> statuses
    );

    @Query("""
        SELECT c FROM Card c
        WHERE c.cardNumberEncrypted = :cardNumberEncrypted
""")
    Optional<Card> findByCardNumberEncrypted(
            @Param("cardNumberEncrypted") String cardNumberEncrypted
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT c FROM Card c
    WHERE c.id = :cardId
      AND c.user.id = :userId
      AND c.status = :status
""")
    Optional<Card> lockByIdAndUserAndStatus(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId,
            @Param("status") CardStatus status
    );


    @Query("""
    SELECT c FROM Card c
    WHERE c.cardNumberEncrypted = :cardNumberEncrypted
      AND c.user.id = :userId
      AND c.status = :status
""")
    Optional<Card> findByCardNumberEncryptedAndUser_IdAndStatus(
            @Param("cardNumberEncrypted") String cardNumberEncrypted,
            @Param("userId") Long userId,
            @Param("status") CardStatus status);

    @Query("""
    SELECT c FROM Card c
    WHERE c.cardNumberEncrypted = :cardNumberEncrypted
      AND c.user.id = :userId
""")
    Optional<Card> findByCardNumberEncryptedAndUser_Id(
            @Param("cardNumberEncrypted") String enc,
            @Param("userId") Long userId);
}
