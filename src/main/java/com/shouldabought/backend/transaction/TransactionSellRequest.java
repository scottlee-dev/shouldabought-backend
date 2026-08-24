package com.shouldabought.backend.transaction;

import java.math.BigDecimal;

public record TransactionSellRequest(String symbol, BigDecimal quantity,  BigDecimal cashAmount) {
}