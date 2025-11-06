package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByUser_Id(Long id, Pageable pageable);
    Optional<Card> findByCardNumberEncryptedAndUser_Id(String enc, Long userId);

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
}
