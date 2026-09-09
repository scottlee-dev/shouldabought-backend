package com.shouldabought.backend.transaction;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findByAccountIdOrderByCreatedAtAsc(Long accountId);

	List<Transaction> findByAccountIdAndSymbolAndCreatedAtBeforeOrderByCreatedAtAsc(Long accountId, String symbol,
			LocalDateTime before);

	List<Transaction> findByTypeIn(List<TransactionType> types);

	boolean existsByAccountIdAndTypeAndSymbolAndDividendExternalId(Long accountId, TransactionType type, String symbol,
			String dividendExternalId);
}