	package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;

import com.shouldabought.backend.market.YahooFinanceResponse;
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

	public DividendData getLatestDividend(String symbol) {

		YahooFinanceResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/v8/finance/chart/{symbol}")
						.queryParam("interval", "1d")
						.queryParam("range", "1y")
						.queryParam("events", "div")
						.build(symbol.toUpperCase()))
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

		YahooFinanceResponse.DividendEvent latestDividend =
				dividends.values()
						.stream()
						.max(Comparator.comparingLong(
								YahooFinanceResponse.DividendEvent::date))
						.orElseThrow(() ->
								new RuntimeException(
										"No valid dividend data returned for " + symbol
								));

		LocalDate dividendDate = Instant
				.ofEpochSecond(latestDividend.date())
				.atZone(ZoneId.of("America/New_York"))
				.toLocalDate();

		BigDecimal amountPerShare =
				BigDecimal.valueOf(latestDividend.amount());

		return new DividendData(
				symbol.toUpperCase(),
				amountPerShare,
				dividendDate
		);
	}

	public record DividendData(
			String symbol,
			BigDecimal amountPerShare,
			LocalDate date
	) {
	}
}