package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class YahooFinanceService {

	private final RestClient restClient;

	public YahooFinanceService() {

		this.restClient = RestClient.builder()
				.baseUrl("https://query2.finance.yahoo.com")
				.defaultHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
								+ "AppleWebKit/537.36 "
								+ "(KHTML, like Gecko) "
								+ "Chrome/151.0.0.0 Safari/537.36"
				)
				.build();
	}

	public List<DividendData> getDividendHistory(String symbol) {

		symbol = symbol.toUpperCase();

		String finalSymbol = symbol;
		YahooFinanceResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/v8/finance/chart/{symbol}")
						.queryParam("interval", "1d")
						.queryParam("range", "1y")
						.queryParam("events", "div")
						.build(finalSymbol))
				.retrieve()
				.body(YahooFinanceResponse.class);

		if (response == null
				|| response.chart() == null
				|| response.chart().result() == null
				|| response.chart().result().length == 0) {

			throw new RuntimeException(
					"No Yahoo Finance data returned for " + symbol
			);
		}

		YahooFinanceResponse.Result result =
				response.chart().result()[0];

		if (result.events() == null
				|| result.events().dividends() == null
				|| result.events().dividends().isEmpty()) {

			throw new RuntimeException(
					"No dividend data returned for " + symbol
			);
		}

		Map<String, YahooFinanceResponse.DividendEvent> dividends =
				result.events().dividends();

		return dividends.values()
				.stream()
				.sorted(
						Comparator.comparingLong(
								YahooFinanceResponse.DividendEvent::date
						).reversed()
				)
				.map(this::convertToDividendData)
				.toList();
	}

	private DividendData convertToDividendData(
			YahooFinanceResponse.DividendEvent dividendEvent) {

		LocalDate dividendDate = Instant
				.ofEpochSecond(dividendEvent.date())
				.atZone(ZoneId.of("America/New_York"))
				.toLocalDate();

		BigDecimal amountPerShare =
				BigDecimal.valueOf(dividendEvent.amount());

		return new DividendData(
				amountPerShare,
				dividendDate
		);
	}

	public record DividendData(
			BigDecimal amountPerShare,
			LocalDate date
	) {
	}
}