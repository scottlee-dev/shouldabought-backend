package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AlphaVantageService {

	private final RestClient restClient;
	private final String apiKey;
	private final ObjectMapper objectMapper;

	public AlphaVantageService(@Value("${alphavantage.base-url}") String baseUrl,
			@Value("${alphavantage.api-key}") String apiKey, ObjectMapper objectMapper) {

		this.restClient = RestClient.builder().baseUrl(baseUrl).build();

		this.apiKey = apiKey;
		this.objectMapper = objectMapper;
	}

	public BigDecimal getCurrentPrice(String symbol) {

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		String normalizedSymbol = symbol.trim().toUpperCase();

		JsonNode root = restClient.get()
				.uri(uriBuilder -> uriBuilder.queryParam("function", "GLOBAL_QUOTE")
						.queryParam("symbol", normalizedSymbol).queryParam("apikey", apiKey).build())
				.retrieve().body(JsonNode.class);

		System.out.println("Alpha Vantage GLOBAL_QUOTE response for " + normalizedSymbol + ": " + root);

		validateResponse(root, "GLOBAL_QUOTE", normalizedSymbol);

		JsonNode quote = root.get("Global Quote");

		if (quote == null || quote.isEmpty()) {
			throw new RuntimeException("No market data returned for " + normalizedSymbol);
		}

		JsonNode priceNode = quote.get("05. price");

		if (priceNode == null || priceNode.asText().isBlank()) {
			throw new RuntimeException("No price returned for " + normalizedSymbol);
		}

		return new BigDecimal(priceNode.asText());
	}

	public List<AlphaVantageDividendResponse.DividendData> getDividendHistory(String symbol) {

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		String normalizedSymbol = symbol.trim().toUpperCase();

		JsonNode root = restClient
				.get().uri(uriBuilder -> uriBuilder.queryParam("function", "DIVIDENDS")
						.queryParam("symbol", normalizedSymbol).queryParam("apikey", apiKey).build())
				.retrieve().body(JsonNode.class);

		System.out.println("Alpha Vantage DIVIDENDS response for " + normalizedSymbol + ": " + root);

		validateResponse(root, "DIVIDENDS", normalizedSymbol);

		JsonNode dataNode = root.get("data");

		if (dataNode == null || !dataNode.isArray() || dataNode.isEmpty()) {
			throw new RuntimeException("No dividend data returned for " + normalizedSymbol);
		}

		List<AlphaVantageDividendResponse.DividendData> dividends = new ArrayList<>();

		for (JsonNode dividendNode : dataNode) {
			AlphaVantageDividendResponse.DividendData dividend = objectMapper.convertValue(dividendNode,
					AlphaVantageDividendResponse.DividendData.class);

			dividends.add(dividend);
		}

		return dividends;
	}

	public boolean isUsEquityMarketOpen() {

		JsonNode root = restClient.get().uri(
				uriBuilder -> uriBuilder.queryParam("function", "MARKET_STATUS").queryParam("apikey", apiKey).build())
				.retrieve().body(JsonNode.class);

		System.out.println("Alpha Vantage MARKET_STATUS response: " + root);

		validateResponse(root, "MARKET_STATUS", null);

		JsonNode markets = root.get("markets");

		if (markets == null || !markets.isArray() || markets.isEmpty()) {
			throw new RuntimeException("No market status data returned");
		}

		for (JsonNode market : markets) {

			String marketType = market.path("market_type").asText();
			String region = market.path("region").asText();

			if ("Equity".equalsIgnoreCase(marketType) && "United States".equalsIgnoreCase(region)) {

				String status = market.path("current_status").asText();

				return "open".equalsIgnoreCase(status);
			}
		}

		throw new RuntimeException("US equity market status not found");
	}

	private void validateResponse(JsonNode root, String function, String symbol) {

		if (root == null || root.isNull()) {
			throw new RuntimeException("Empty response from Alpha Vantage for " + function);
		}

		if (root.has("Information")) {
			throw new RuntimeException(buildErrorMessage(function, symbol, root.get("Information").asText()));
		}

		if (root.has("Note")) {
			throw new RuntimeException(buildErrorMessage(function, symbol, root.get("Note").asText()));
		}

		if (root.has("Error Message")) {
			throw new RuntimeException(buildErrorMessage(function, symbol, root.get("Error Message").asText()));
		}
	}

	private String buildErrorMessage(String function, String symbol, String message) {

		String requestName = symbol == null ? function : function + " for " + symbol;

		return "Alpha Vantage " + requestName + ": " + message;
	}
}