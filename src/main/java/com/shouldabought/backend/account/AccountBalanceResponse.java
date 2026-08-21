package com.shouldabought.backend.account;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        Long accountId,
        BigDecimal cash,
        BigDecimal stockValue,
        BigDecimal totalBalance
) {
}