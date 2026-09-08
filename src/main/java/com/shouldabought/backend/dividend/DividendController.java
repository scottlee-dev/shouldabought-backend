package com.shouldabought.backend.dividend;

import java.time.LocalDate;
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

	@PostMapping("/entitlements/process/{exDate}")
	public List<DividendEntitlement> processEntitlements(@PathVariable LocalDate exDate) {

		return dividendService.createEntitlementsForExDate(exDate);
	}

	@PostMapping("/payments/process/{processDate}")
	public List<DividendEntitlement> processPayments(@PathVariable LocalDate processDate) {

		return dividendService.processDividendPayments(processDate);
	}
}