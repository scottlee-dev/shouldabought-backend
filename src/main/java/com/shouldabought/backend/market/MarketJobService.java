package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shouldabought.backend.account.AccountRepository;
import com.shouldabought.backend.dividend.DividendService;
import com.shouldabought.backend.portfolio.PortfolioSnapshotService;

@Service
public class MarketJobService {

	private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

	private final AlphaVantageService alphaVantageService;
	private final StockPriceRepository stockPriceRepository;
	private final DividendService dividendService;
	private final PortfolioSnapshotService portfolioSnapshotService;
	private final AccountRepository accountRepository;

	public MarketJobService(AlphaVantageService alphaVantageService, StockPriceRepository stockPriceRepository,
			DividendService dividendService, PortfolioSnapshotService portfolioSnapshotService,
			AccountRepository accountRepository) {

		this.alphaVantageService = alphaVantageService;
		this.stockPriceRepository = stockPriceRepository;
		this.dividendService = dividendService;
		this.portfolioSnapshotService = portfolioSnapshotService;
		this.accountRepository = accountRepository;
	}

	public MarketJobResult runMorningJob() {
		try {
			LocalDate today = LocalDate.now(MARKET_ZONE);

			if (!alphaVantageService.isUsEquityMarketOpen()) {
				return new MarketJobResult("MORNING", today, false, 0);
			}

			List<String> heldSymbols = dividendService.getCurrentlyHeldSymbols();

			dividendService.createMissingEntitlementsThrough(today);

			dividendService.processDividendPayments(today);

			return new MarketJobResult("MORNING", today, true, heldSymbols.size());

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public MarketJobResult runClosingJob() {

		LocalDate today = LocalDate.now(MARKET_ZONE);

		List<String> heldSymbols = dividendService.getCurrentlyHeldSymbols();


		refreshPrices(heldSymbols);
		sleepForAlphaVantage();
		dividendService.syncDividendsForHeldSymbols();

		accountRepository.findAll().forEach(account -> portfolioSnapshotService.createSnapshot(account.getId()));

		return new MarketJobResult("CLOSING", today, true, heldSymbols.size());
	}
	private void sleepForAlphaVantage() {
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(
					"Alpha Vantage request delay interrupted",
					e
			);
		}
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

			} catch (RuntimeException e) {
				failedSymbols.add(symbol);

				System.out.println("Price refresh failed for " + symbol + ": " + e.getMessage());
			}

			try {
				Thread.sleep(1200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();

				throw new RuntimeException("Price refresh interrupted", e);
			}
		}

		if (!failedSymbols.isEmpty()) {
			throw new RuntimeException("Price refresh failed for: " + String.join(", ", failedSymbols));
		}
	}

	public record MarketJobResult(String job, LocalDate date, boolean executed, int symbolCount) {
	}
}