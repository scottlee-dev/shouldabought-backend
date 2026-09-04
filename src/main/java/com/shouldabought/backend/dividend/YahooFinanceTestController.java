package com.shouldabought.backend.dividend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shouldabought.backend.market.YahooFinanceService;

@RestController
@RequestMapping("/api/test/yahoo")
public class YahooFinanceTestController {

	private final YahooFinanceService yahooFinanceService;

	public YahooFinanceTestController(YahooFinanceService yahooFinanceService) {

		this.yahooFinanceService = yahooFinanceService;
	}

	@GetMapping("/dividend/{symbol}")
	public List<YahooFinanceService.DividendData> getDividendHistory(@PathVariable String symbol) {

		return yahooFinanceService.getDividendHistory(symbol);
	}
}