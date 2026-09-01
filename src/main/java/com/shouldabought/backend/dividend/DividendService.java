package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.account.Account;
import com.shouldabought.backend.account.AccountRepository;
import com.shouldabought.backend.market.AlphaVantageDividendResponse;
import com.shouldabought.backend.market.AlphaVantageService;
import com.shouldabought.backend.market.StockPrice;
import com.shouldabought.backend.market.StockPriceRepository;
import com.shouldabought.backend.transaction.Transaction;
import com.shouldabought.backend.transaction.TransactionRepository;
import com.shouldabought.backend.transaction.TransactionType;

@Service
public class DividendService {

	private final DividendRepository dividendRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final StockPriceRepository stockPriceRepository;
	private final AlphaVantageService alphaVantageService;

	public DividendService(DividendRepository dividendRepository, AccountRepository accountRepository,
			TransactionRepository transactionRepository, StockPriceRepository stockPriceRepository,
			AlphaVantageService alphaVantageService) {

		this.dividendRepository = dividendRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.stockPriceRepository = stockPriceRepository;
		this.alphaVantageService = alphaVantageService;
	}

	@Transactional
	public Dividend processAutomaticDividend(Long accountId, String symbol, boolean reinvest) {

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		symbol = symbol.toUpperCase();

		/*
		 * Ask Alpha Vantage for the latest dividend.
		 */
		AlphaVantageDividendResponse.DividendData dividendData = alphaVantageService.getLatestDividend(symbol);

		BigDecimal amountPerShare = new BigDecimal(dividendData.amount());

		LocalDate exDividendDate = LocalDate.parse(dividendData.exDividendDate());

		LocalDate declarationDate = parseDate(dividendData.declarationDate());

		LocalDate recordDate = parseDate(dividendData.recordDate());

		LocalDate payDate = parseDate(dividendData.paymentDate());

		/*
		 * Use symbol + ex-dividend date as the external ID.
		 *
		 * Example:
		 *
		 * AAPL-2026-08-10
		 *
		 * This prevents the same dividend from being processed twice.
		 */
		String externalId = symbol + "-" + exDividendDate;

		DividendProcessRequest request = new DividendProcessRequest(symbol, externalId, amountPerShare, declarationDate,
				exDividendDate, recordDate, payDate, reinvest);

		return processDividend(accountId, request);
	}

	@Transactional
	public Dividend processDividend(Long accountId, DividendProcessRequest request) {

		/*
		 * Prevent duplicate processing for this account.
		 */
		if (transactionRepository.existsByAccountIdAndTypeAndSymbolAndDividendExternalId(accountId,
				TransactionType.DIVIDEND, request.symbol(), request.externalId())) {

			throw new RuntimeException("Dividend already processed: " + request.externalId());
		}

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		/*
		 * Save the dividend information if this dividend has not already been stored
		 * globally.
		 */
		Dividend dividend = dividendRepository.findByExternalId(request.externalId()).orElseGet(() -> {

			Dividend newDividend = new Dividend(request.externalId(), request.symbol(), request.amountPerShare(),
					request.declarationDate(), request.exDividendDate(), request.recordDate(), request.payDate());

			return dividendRepository.save(newDividend);
		});

		/*
		 * Calculate the current number of shares owned.
		 *
		 * Only BUY and SELL transactions affect share quantity.
		 *
		 * DIVIDEND does not change the quantity.
		 */
		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		BigDecimal currentQuantity = BigDecimal.ZERO;

		for (Transaction transaction : transactions) {

			if (!request.symbol().equals(transaction.getSymbol())) {
				continue;
			}

			if (transaction.getType() == TransactionType.BUY) {

				currentQuantity = currentQuantity.add(transaction.getQuantity(), MathContext.DECIMAL128);

			} else if (transaction.getType() == TransactionType.SELL) {

				currentQuantity = currentQuantity.subtract(transaction.getQuantity(), MathContext.DECIMAL128);
			}
		}

		if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("No shares owned for " + request.symbol());
		}

		/*
		 * Calculate total dividend.
		 *
		 * Example:
		 *
		 * 35 shares × $0.26 = $9.10
		 */
		BigDecimal dividendAmount = currentQuantity.multiply(dividend.getAmountPerShare(), MathContext.DECIMAL128);

		/*
		 * Record the dividend as cash first.
		 */
		account.increaseCash(dividendAmount);

		accountRepository.save(account);

		/*
		 * Create the DIVIDEND transaction.
		 */
		Transaction dividendTransaction = new Transaction(account, TransactionType.DIVIDEND, dividendAmount,
				dividend.getSymbol(), dividend.getExternalId());

		transactionRepository.save(dividendTransaction);

		/*
		 * DRIP
		 *
		 * Dividend cash is immediately used to buy the same stock.
		 */
		if (request.reinvest()) {

			StockPrice stockPrice = stockPriceRepository.findBySymbol(request.symbol())
					.orElseThrow(() -> new RuntimeException("Price not found for " + request.symbol()));

			BigDecimal currentPrice = stockPrice.getPrice();

			if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Stock price must be greater than zero");
			}

			BigDecimal reinvestQuantity = dividendAmount.divide(currentPrice, MathContext.DECIMAL128);

			/*
			 * Remove the dividend cash immediately.
			 */
			account.decreaseCash(dividendAmount);

			accountRepository.save(account);

			/*
			 * The DRIP purchase is recorded as a normal BUY.
			 *
			 * Therefore it automatically becomes a new FIFO cost lot in
			 * AccountService.getPortfolio().
			 */
			Transaction reinvestTransaction = new Transaction(account, TransactionType.BUY, dividendAmount,
					request.symbol(), reinvestQuantity, currentPrice);

			transactionRepository.save(reinvestTransaction);
		}

		return dividend;
	}

	private LocalDate parseDate(String value) {

		if (value == null || value.isBlank()) {
			return null;
		}

		return LocalDate.parse(value);
	}
}