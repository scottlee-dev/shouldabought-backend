package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.account.Account;
import com.shouldabought.backend.account.AccountRepository;
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

	public DividendService(DividendRepository dividendRepository, AccountRepository accountRepository,
			TransactionRepository transactionRepository, StockPriceRepository stockPriceRepository) {

		this.dividendRepository = dividendRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.stockPriceRepository = stockPriceRepository;
	}

	@Transactional
	public Dividend processDividend(Long accountId, DividendProcessRequest request) {

		if (transactionRepository.existsByAccountIdAndTypeAndSymbolAndDividendExternalId(accountId,
				TransactionType.DIVIDEND, request.symbol(), request.externalId())) {

			throw new RuntimeException("Dividend already processed: " + request.externalId());
		}

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		Dividend dividend = dividendRepository.findByExternalId(request.externalId()).orElseGet(() -> {

			Dividend newDividend = new Dividend(request.externalId(), request.symbol(), request.amountPerShare(),
					request.declarationDate(), request.exDividendDate(), request.recordDate(), request.payDate());

			return dividendRepository.save(newDividend);
		});

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

		BigDecimal dividendAmount = currentQuantity.multiply(dividend.getAmountPerShare(), MathContext.DECIMAL128);

		account.increaseCash(dividendAmount);

		accountRepository.save(account);

		Transaction dividendTransaction = new Transaction(account, TransactionType.DIVIDEND, dividendAmount,
				dividend.getSymbol(), dividend.getExternalId());

		Transaction savedDividend = transactionRepository.save(dividendTransaction);

		if (request.reinvest()) {

			StockPrice stockPrice = stockPriceRepository.findBySymbol(request.symbol())
					.orElseThrow(() -> new RuntimeException("Price not found for " + request.symbol()));

			BigDecimal currentPrice = stockPrice.getPrice();

			if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Stock price must be greater than zero");
			}

			BigDecimal reinvestQuantity = dividendAmount.divide(currentPrice, MathContext.DECIMAL128);

			account.decreaseCash(dividendAmount);

			accountRepository.save(account);

			Transaction reinvestTransaction = new Transaction(account, TransactionType.BUY, dividendAmount,
					request.symbol(), reinvestQuantity, currentPrice);

			transactionRepository.save(reinvestTransaction);
		}

		return dividend;
	}
}