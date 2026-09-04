package com.shouldabought.backend.account;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(BigDecimal cash, BigDecimal totalValue, BigDecimal totalGainLoss,
		BigDecimal realizedGainLoss, List<Holding> holdings) {

	public record Holding(String symbol, BigDecimal quantity, BigDecimal currentPrice, BigDecimal marketValue,
			BigDecimal averageCost, BigDecimal gainLoss, BigDecimal gainLossPercentage, BigDecimal accountPercentage) {
	}
}