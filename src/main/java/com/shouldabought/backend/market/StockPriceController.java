package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class StockPriceController {

	private final StockPriceRepository stockPriceRepository;

	public StockPriceController(StockPriceRepository stockPriceRepository) {
		this.stockPriceRepository = stockPriceRepository;
	}

	@PostMapping("/prices")
	public StockPrice updatePrice(@RequestBody StockPriceRequest request) {

		StockPrice stockPrice = stockPriceRepository.findBySymbol(request.symbol())
				.orElseGet(() -> new StockPrice(request.symbol(), request.price(), LocalDateTime.now()));

		stockPrice.updatePrice(request.price(), LocalDateTime.now());

		return stockPriceRepository.save(stockPrice);
	}

	public record StockPriceRequest(String symbol, BigDecimal price) {
	}
}