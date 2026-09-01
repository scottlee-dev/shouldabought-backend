package com.shouldabought.backend.market;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlphaVantageDividendResponse(

        @JsonProperty("data")
        List<DividendData> data

) {

    public record DividendData(

            @JsonProperty("ex_dividend_date")
			String exDividendDate,

            @JsonProperty("declaration_date")
            String declarationDate,

            @JsonProperty("record_date")
            String recordDate,

            @JsonProperty("payment_date")
            String paymentDate,

            @JsonProperty("amount")
            String amount

    ) {
    }
}