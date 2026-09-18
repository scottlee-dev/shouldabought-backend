package com.shouldabought.backend.portfolio;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/portfolio/history")
public class PortfolioSnapshotController {

	private final PortfolioSnapshotService portfolioSnapshotService;

	public PortfolioSnapshotController(PortfolioSnapshotService portfolioSnapshotService) {
		this.portfolioSnapshotService = portfolioSnapshotService;
	}

	@GetMapping
	public List<PortfolioSnapshot> getHistory(@PathVariable Long accountId) {
		return portfolioSnapshotService.getHistory(accountId);
	}

}