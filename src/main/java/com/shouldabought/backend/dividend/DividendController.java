package com.shouldabought.backend.dividend;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class DividendController {

    private final DividendService dividendService;

    public DividendController(DividendService dividendService) {
        this.dividendService = dividendService;
    }

    @PostMapping("/{id}/dividends")
    public Dividend processDividend(
            @PathVariable Long id,
            @RequestBody DividendProcessRequest request) {

        return dividendService.processDividend(id, request);
    }
}