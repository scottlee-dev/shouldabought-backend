package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.dividend.DividendService;

@Service
public class MarketJobService {

	private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

	private final AlphaVantageService alphaVantageService;
	private final StockPriceRepository stockPriceRepository;
	private final DividendService dividendService;

	public MarketJobService(AlphaVantageService alphaVantageService, StockPriceRepository stockPriceRepository,
			DividendService dividendService) {

		this.alphaVantageService = alphaVantageService;
		this.stockPriceRepository = stockPriceRepository;
		this.dividendService = dividendService;
	}


	public MarketJobResult runMorningJob() {

		LocalDate today = LocalDate.now(MARKET_ZONE);

		/*
		 * If the US equity market is closed, do nothing.
		 *
		 * This naturally handles weekends, market holidays, and unexpected closures
		 * without maintaining a hard-coded calendar.
		 */
		if (!alphaVantageService.isUsEquityMarketOpen()) {

			return new MarketJobResult("MORNING", today, false, 0);
		}

		List<String> heldSymbols = dividendService.getCurrentlyHeldSymbols();

		refreshPrices(heldSymbols);

		dividendService.syncDividendsForHeldSymbols();

		dividendService.createMissingEntitlementsThrough(today);

		dividendService.processDividendPayments(today);

		return new MarketJobResult("MORNING", today, true, heldSymbols.size());
	}


	public MarketJobResult runClosingJob() {

		LocalDate today = LocalDate.now(MARKET_ZONE);

		List<String> heldSymbols = dividendService.getCurrentlyHeldSymbols();

		/*
		 * Do not block this job using MARKET_STATUS.
		 *
		 * On an early-close day the market may already be closed at 3:55 PM, but
		 * refreshing the quote still lets us store the latest available price.
		 */
		refreshPrices(heldSymbols);

		dividendService.syncDividendsForHeldSymbols();

		return new MarketJobResult("CLOSING", today, true, heldSymbols.size());
	}

	private void refreshPrices(List<String> symbols) {

		List<String> failedSymbols = new ArrayList<>();

		for (String symbol : symbols) {

			try {

				BigDecimal price = alphaVantageService.getCurrentPrice(symbol);

				LocalDateTime now = LocalDateTime.now(MARKET_ZONE);

				StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
						.orElseGet(() -> new StockPrice(symbol, price, now));

				stockPrice.updatePrice(price, now);

				stockPriceRepository.save(stockPrice);

			} catch (RuntimeException exception) {

				failedSymbols.add(symbol);

				System.out.println("Price refresh failed for " + symbol + ": " + exception.getMessage());
			}
		}

		if (!failedSymbols.isEmpty()) {

			throw new RuntimeException("Price refresh failed for: " + String.join(", ", failedSymbols));
		}
	}

	public record MarketJobResult(String job, LocalDate date, boolean executed, int symbolCount) {
	}
}