package com.shouldabought.backend.transaction;

import java.math.BigDecimal;

public record DividendRequest(
        String symbol,
        BigDecimal amount
) {
}