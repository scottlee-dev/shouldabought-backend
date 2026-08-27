package com.shouldabought.backend.market;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlphaVantageResponse(
        @JsonProperty("Global Quote")
        GlobalQuote globalQuote
) {

    public record GlobalQuote(
            @JsonProperty("01. symbol")
            String symbol,

            @JsonProperty("05. price")
            String price
    ) {
    }
}