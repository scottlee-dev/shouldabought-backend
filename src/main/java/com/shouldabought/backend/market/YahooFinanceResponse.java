package com.shouldabought.backend.market;

import java.util.Map;

public record YahooFinanceResponse(Chart chart) {

	public record Chart(Result[] result, Object error) {
	}

	public record Result(Meta meta, Indicators indicators, Events events) {
	}

	public record Meta(String symbol, String currency, String exchangeName, String instrumentType) {
	}

	public record Indicators(Quote[] quote) {
	}

	public record Quote(Map<String, Object> data) {
	}

	public record Events(Map<String, DividendEvent> dividends) {
	}

	public record DividendEvent(double amount, long date) {
	}

	/*
	 * Yahoo Finance occasionally returns numeric values as JSON numbers. Using a
	 * wrapper here keeps the DTO flexible.
	 */
	public record BigDecimalWrapper(double value) {
	}
}