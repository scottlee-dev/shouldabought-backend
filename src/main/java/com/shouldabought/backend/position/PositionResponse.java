package com.shouldabought.backend.position;

import java.math.BigDecimal;

public record PositionResponse(
        String symbol,
        BigDecimal quantity
) {
}