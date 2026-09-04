package com.shouldabought.backend.dividend;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dividends")
public class DividendController {

	private final DividendService dividendService;

	public DividendController(DividendService dividendService) {

		this.dividendService = dividendService;
	}

	@PostMapping("/sync/{symbol}")
	public List<Dividend> syncDividends(@PathVariable String symbol) {

		return dividendService.syncDividends(symbol);
	}
}