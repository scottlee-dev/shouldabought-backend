package com.shouldabought.backend.market;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlphaVantageMarketStatusResponse(

		@JsonProperty("markets") List<MarketStatus> markets

) {

	public record MarketStatus(

			@JsonProperty("market_type") String marketType,

			@JsonProperty("region") String region,

			@JsonProperty("primary_exchanges") String primaryExchanges,

			@JsonProperty("local_open") String localOpen,

			@JsonProperty("local_close") String localClose,

			@JsonProperty("current_status") String currentStatus

	) {
	}
}