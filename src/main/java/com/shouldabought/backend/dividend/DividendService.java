package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.market.AlphaVantageDividendResponse;
import com.shouldabought.backend.market.AlphaVantageService;

@Service
public class DividendService {

	private final AlphaVantageService alphaVantageService;
	private final DividendRepository dividendRepository;

	public DividendService(AlphaVantageService alphaVantageService, DividendRepository dividendRepository) {

		this.alphaVantageService = alphaVantageService;
		this.dividendRepository = dividendRepository;
	}

	@Transactional
	public List<Dividend> syncDividends(String symbol) {

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		symbol = symbol.trim().toUpperCase();

		List<AlphaVantageDividendResponse.DividendData> dividendData = alphaVantageService.getDividendHistory(symbol);

		List<Dividend> syncedDividends = new ArrayList<>();

		for (AlphaVantageDividendResponse.DividendData data : dividendData) {

			LocalDate exDividendDate = parseDate(data.exDividendDate());

			BigDecimal amountPerShare = parseAmount(data.amount());

			if (exDividendDate == null || amountPerShare == null || amountPerShare.compareTo(BigDecimal.ZERO) <= 0) {

				continue;
			}

			LocalDate declarationDate = parseDate(data.declarationDate());

			LocalDate recordDate = parseDate(data.recordDate());

			LocalDate payDate = parseDate(data.paymentDate());

			String externalId = symbol + "-" + exDividendDate;

			String finalSymbol = symbol;

			Dividend dividend = dividendRepository.findByExternalId(externalId).map(existingDividend -> {

				existingDividend.updateDetails(amountPerShare, declarationDate, exDividendDate, recordDate, payDate);

				return existingDividend;
			}).orElseGet(() -> new Dividend(externalId, finalSymbol, amountPerShare, declarationDate, exDividendDate,
					recordDate, payDate));

			syncedDividends.add(dividendRepository.save(dividend));
		}

		syncedDividends.sort(Comparator.comparing(Dividend::getExDividendDate).reversed());

		return syncedDividends;
	}

	private LocalDate parseDate(String value) {

		if (value == null || value.isBlank() || "None".equalsIgnoreCase(value)) {

			return null;
		}

		try {
			return LocalDate.parse(value);

		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private BigDecimal parseAmount(String value) {

		if (value == null || value.isBlank() || "None".equalsIgnoreCase(value)) {

			return null;
		}

		try {
			return new BigDecimal(value);

		} catch (NumberFormatException exception) {
			return null;
		}
	}
}