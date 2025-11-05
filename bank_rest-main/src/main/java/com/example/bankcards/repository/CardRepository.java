package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findAllBy (Pageable pageable);

    @Query("""
        SELECT c FROM Card c
        WHERE c.cardNumberEncrypted = :cardNumberEncrypted
""")
    Optional<Card> existsByCardNumberEncrypted(
            @Param("cardNumberEncrypted") String cardNumberEncrypted
    );
}
