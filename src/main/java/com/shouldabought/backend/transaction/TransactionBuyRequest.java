package com.shouldabought.backend.transaction;

import java.math.BigDecimal;

public record TransactionBuyRequest(String symbol, BigDecimal quantity, BigDecimal cashAmount) {
}