package com.shouldabought.backend.market;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs/market")
public class MarketJobController {

	private final MarketJobService marketJobService;

	public MarketJobController(MarketJobService marketJobService) {

		this.marketJobService = marketJobService;
	}

	@PostMapping("/morning")
	public MarketJobService.MarketJobResult runMorningJob() {

		return marketJobService.runMorningJob();
	}

	@PostMapping("/closing")
	public MarketJobService.MarketJobResult runClosingJob() {

		return marketJobService.runClosingJob();
	}
}