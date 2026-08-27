package com.shouldabought.backend.market;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AlphaVantageService {

    private final RestClient restClient;
    private final String apiKey;

    public AlphaVantageService(
            @Value("${alphavantage.base-url}") String baseUrl,
            @Value("${alphavantage.api-key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    public BigDecimal getCurrentPrice(String symbol) {

        AlphaVantageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(AlphaVantageResponse.class);

        if (response == null || response.globalQuote() == null) {
            throw new RuntimeException(
                    "No market data returned for " + symbol
            );
        }

        String price = response.globalQuote().price();

        if (price == null || price.isBlank()) {
            throw new RuntimeException(
                    "No price returned for " + symbol
            );
        }

        return new BigDecimal(price);
    }
}