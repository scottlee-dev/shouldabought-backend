package com.shouldabought.backend.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtAsc(Long accountId);

    boolean existsByAccountIdAndTypeAndSymbolAndDividendExternalId(
            Long accountId,
            TransactionType type,
            String symbol,
            String dividendExternalId
    );
}