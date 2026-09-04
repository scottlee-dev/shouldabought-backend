package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
public class StockPriceController {

	private final StockPriceRepository stockPriceRepository;
	private final AlphaVantageService alphaVantageService;

	public StockPriceController(StockPriceRepository stockPriceRepository, AlphaVantageService alphaVantageService) {

		this.stockPriceRepository = stockPriceRepository;
		this.alphaVantageService = alphaVantageService;
	}

	/*
	 * Manual price update.
	 *
	 * Used mainly for development/testing.
	 */
	@PostMapping("/prices")
	public StockPrice updatePrice(@RequestBody StockPriceRequest request) {

		String symbol = request.symbol().toUpperCase();

		StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
				.orElseGet(() -> new StockPrice(symbol, request.price(), LocalDateTime.now()));

		stockPrice.updatePrice(request.price(), LocalDateTime.now());

		return stockPriceRepository.save(stockPrice);
	}

	/*
	 * Refresh a stock price from Alpha Vantage.
	 *
	 * Example:
	 *
	 * POST /api/market/prices/refresh/AAPL
	 */
	@PostMapping("/prices/refresh/{symbol}")
	public StockPrice refreshPrice(@PathVariable String symbol) {

		symbol = symbol.toUpperCase();

		BigDecimal price = alphaVantageService.getCurrentPrice(symbol);

		String finalSymbol = symbol;
		StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
				.orElseGet(() -> new StockPrice(finalSymbol, price, LocalDateTime.now()));

		stockPrice.updatePrice(price, LocalDateTime.now());

		return stockPriceRepository.save(stockPrice);
	}

	public record StockPriceRequest(String symbol, BigDecimal price) {
	}
}