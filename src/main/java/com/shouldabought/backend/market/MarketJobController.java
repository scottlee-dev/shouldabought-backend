package com.shouldabought.backend.market;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs/market")
public class MarketJobController {

	private final MarketJobService marketJobService;

	public MarketJobController(MarketJobService marketJobService) {

		this.marketJobService = marketJobService;
	}

//	@PostMapping("/morning")
//	public MarketJobService.MarketJobResult runMorningJob() {
//
//		return marketJobService.runMorningJob();
//	}
@PostMapping("/morning")
public ResponseEntity<?> runMorningJob() {
	try {
		return ResponseEntity.ok(marketJobService.runMorningJob());
	} catch (RuntimeException e) {
		return ResponseEntity
				.internalServerError()
				.body(Map.of("error", e.getMessage()));
	}
}
	@PostMapping("/closing")
	public MarketJobService.MarketJobResult runClosingJob() {

		return marketJobService.runClosingJob();
	}
}