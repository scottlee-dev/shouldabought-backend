package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.account.Account;
import com.shouldabought.backend.account.AccountRepository;
import com.shouldabought.backend.market.AlphaVantageDividendResponse;
import com.shouldabought.backend.market.AlphaVantageService;
import com.shouldabought.backend.transaction.Transaction;
import com.shouldabought.backend.transaction.TransactionRepository;
import com.shouldabought.backend.transaction.TransactionType;

@Service
public class DividendService {

	private final AlphaVantageService alphaVantageService;
	private final DividendRepository dividendRepository;
	private final DividendEntitlementRepository entitlementRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public DividendService(AlphaVantageService alphaVantageService, DividendRepository dividendRepository,
			DividendEntitlementRepository entitlementRepository, AccountRepository accountRepository,
			TransactionRepository transactionRepository) {

		this.alphaVantageService = alphaVantageService;
		this.dividendRepository = dividendRepository;
		this.entitlementRepository = entitlementRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	/*
	 * Fetch dividend history from Alpha Vantage and synchronize it with the local
	 * database.
	 */
	@Transactional
	public List<Dividend> syncDividends(String symbol) {

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		symbol = symbol.trim().toUpperCase();

		List<AlphaVantageDividendResponse.DividendData> dividendData = alphaVantageService.getDividendHistory(symbol);

		List<Dividend> syncedDividends = new ArrayList<>();

		for (AlphaVantageDividendResponse.DividendData data : dividendData) {

			LocalDate exDividendDate = parseDate(data.exDividendDate());

			BigDecimal amountPerShare = parseAmount(data.amount());

			if (exDividendDate == null || amountPerShare == null || amountPerShare.compareTo(BigDecimal.ZERO) <= 0) {

				continue;
			}

			LocalDate declarationDate = parseDate(data.declarationDate());

			LocalDate recordDate = parseDate(data.recordDate());

			LocalDate payDate = parseDate(data.paymentDate());

			String externalId = symbol + "-" + exDividendDate;

			String finalSymbol = symbol;

			Dividend dividend = dividendRepository.findByExternalId(externalId).map(existingDividend -> {

				existingDividend.updateDetails(amountPerShare, declarationDate, exDividendDate, recordDate, payDate);

				return existingDividend;
			}).orElseGet(() -> new Dividend(externalId, finalSymbol, amountPerShare, declarationDate, exDividendDate,
					recordDate, payDate));

			syncedDividends.add(dividendRepository.save(dividend));
		}

		syncedDividends.sort(Comparator.comparing(Dividend::getExDividendDate).reversed());

		return syncedDividends;
	}

	/*
	 * Create dividend entitlements for all accounts for dividends whose ex-dividend
	 * date matches the given date.
	 */
	@Transactional
	public List<DividendEntitlement> createEntitlementsForExDate(LocalDate exDate) {

		List<Dividend> dividends = dividendRepository.findByExDividendDate(exDate);

		List<DividendEntitlement> createdEntitlements = new ArrayList<>();

		for (Dividend dividend : dividends) {

			List<Account> accounts = accountRepository.findAll();

			for (Account account : accounts) {

				/*
				 * If an entitlement already exists for this account/dividend combination, do
				 * nothing.
				 */
				boolean alreadyExists = entitlementRepository
						.findByAccountIdAndDividendId(account.getId(), dividend.getId()).isPresent();

				if (alreadyExists) {
					continue;
				}

				BigDecimal qualifiedQuantity = calculateQualifiedQuantity(account.getId(), dividend.getSymbol(),
						exDate);

				/*
				 * No shares held before the ex-date means there is no dividend entitlement.
				 */
				if (qualifiedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				BigDecimal amount = qualifiedQuantity.multiply(dividend.getAmountPerShare()).setScale(4,
						RoundingMode.HALF_UP);

				DividendEntitlement entitlement = new DividendEntitlement(account, dividend, qualifiedQuantity, amount);

				createdEntitlements.add(entitlementRepository.save(entitlement));
			}
		}

		return createdEntitlements;
	}

	/*
	 * Calculate how many shares the account owned immediately before the
	 * ex-dividend date.
	 */
	private BigDecimal calculateQualifiedQuantity(Long accountId, String symbol, LocalDate exDate) {

		LocalDateTime cutoff = exDate.atStartOfDay();

		List<Transaction> transactions = transactionRepository
				.findByAccountIdAndSymbolAndCreatedAtBeforeOrderByCreatedAtAsc(accountId, symbol, cutoff);

		BigDecimal quantity = BigDecimal.ZERO;

		for (Transaction transaction : transactions) {

			if (transaction.getQuantity() == null) {
				continue;
			}

			if (transaction.getType() == TransactionType.BUY) {

				quantity = quantity.add(transaction.getQuantity());

			} else if (transaction.getType() == TransactionType.SELL) {

				quantity = quantity.subtract(transaction.getQuantity());
			}
		}

		return quantity;
	}

	private LocalDate parseDate(String value) {

		if (value == null || value.isBlank() || "None".equalsIgnoreCase(value)) {

			return null;
		}

		try {
			return LocalDate.parse(value);

		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private BigDecimal parseAmount(String value) {

		if (value == null || value.isBlank() || "None".equalsIgnoreCase(value)) {

			return null;
		}

		try {
			return new BigDecimal(value);

		} catch (NumberFormatException exception) {
			return null;
		}
	}

	@Transactional
	public List<DividendEntitlement> processDividendPayments(LocalDate processDate) {

		List<DividendEntitlement> unpaidEntitlements = entitlementRepository.findByPaidFalse();

		List<DividendEntitlement> paidEntitlements = new ArrayList<>();

		for (DividendEntitlement entitlement : unpaidEntitlements) {

			Dividend dividend = entitlement.getDividend();

			/*
			 * Dividend cannot be paid if Alpha Vantage does not provide a payment date.
			 */
			if (dividend.getPayDate() == null) {
				continue;
			}

			/*
			 * Process dividends whose payment date is today or earlier.
			 *
			 * Using "isAfter" rather than exact equality also allows missed payments to be
			 * recovered if the application was not running on pay date.
			 */
			if (dividend.getPayDate().isAfter(processDate)) {
				continue;
			}

			Account account = entitlement.getAccount();

			/*
			 * Defensive duplicate check.
			 *
			 * Each account should receive each dividend event only once.
			 */
			boolean alreadyPaid = transactionRepository.existsByAccountIdAndTypeAndSymbolAndDividendExternalId(
					account.getId(), TransactionType.DIVIDEND, dividend.getSymbol(), dividend.getExternalId());

			if (alreadyPaid) {

				/*
				 * If a DIVIDEND transaction already exists, treat the entitlement as paid as
				 * well.
				 */
				entitlement.markPaid();

				paidEntitlements.add(entitlementRepository.save(entitlement));

				continue;
			}

			BigDecimal dividendAmount = entitlement.getAmount();

			/*
			 * Add dividend cash to the account.
			 */
			account.increaseCash(dividendAmount);

			accountRepository.save(account);

			/*
			 * Record the dividend payment as a transaction.
			 */
			Transaction transaction = new Transaction(account, TransactionType.DIVIDEND, dividendAmount,
					dividend.getSymbol(), dividend.getExternalId());

			transactionRepository.save(transaction);

			/*
			 * Mark entitlement as completed.
			 */
			entitlement.markPaid();

			paidEntitlements.add(entitlementRepository.save(entitlement));
		}

		return paidEntitlements;
	}
}