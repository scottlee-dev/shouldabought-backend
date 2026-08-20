package com.shouldabought.backend.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String symbol,
        BigDecimal quantity,
        BigDecimal price,
        LocalDateTime createdAt
) {
}