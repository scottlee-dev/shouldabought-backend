package com.shouldabought.backend.dividend;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class DividendController {

	private final DividendService dividendService;

	public DividendController(DividendService dividendService) {
		this.dividendService = dividendService;
	}

	@PostMapping("/{id}/dividends/auto")
	public Dividend processAutomaticDividend(@PathVariable Long id, @RequestBody AutoDividendRequest request) {

		return dividendService.processAutomaticDividend(id, request.symbol(), request.reinvest());
	}

	public record AutoDividendRequest(String symbol, boolean reinvest) {
	}
}