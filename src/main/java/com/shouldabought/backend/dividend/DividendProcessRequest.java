package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividendProcessRequest(
        String externalId,
        String symbol,
        BigDecimal amountPerShare,
        LocalDate declarationDate,
        LocalDate exDividendDate,
        LocalDate recordDate,
        LocalDate payDate,
        boolean reinvest
) {
}