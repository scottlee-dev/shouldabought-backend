package com.shouldabought.backend.account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.market.StockPrice;
import com.shouldabought.backend.market.StockPriceRepository;
import com.shouldabought.backend.position.PositionResponse;
import com.shouldabought.backend.transaction.Transaction;
import com.shouldabought.backend.transaction.TransactionRepository;
import com.shouldabought.backend.transaction.TransactionResponse;
import com.shouldabought.backend.transaction.TransactionType;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final StockPriceRepository stockPriceRepository;

	public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
			StockPriceRepository stockPriceRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.stockPriceRepository = stockPriceRepository;
	}

	@Transactional
	public Account createAccount(String name, BigDecimal initialCash) {

		Account account = new Account(name, initialCash, AccountType.REALITY);

		accountRepository.save(account);

		Transaction deposit = new Transaction(account, TransactionType.DEPOSIT, initialCash);

		transactionRepository.save(deposit);

		return account;
	}

	@Transactional
	public Transaction sellStock(Long accountId, String symbol, BigDecimal quantity, BigDecimal cashAmount) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
				.orElseThrow(() -> new RuntimeException("Price not found for " + symbol));

		BigDecimal price = stockPrice.getPrice();

		// Must provide exactly one of quantity or cashAmount
		if (quantity != null && cashAmount != null) {
			throw new RuntimeException("Provide either quantity or cashAmount, not both");
		}

		if (quantity == null && cashAmount == null) {
			throw new RuntimeException("Either quantity or cashAmount is required");
		}

		// Calculate how many shares are being sold
		if (cashAmount != null) {

			if (cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Cash amount must be greater than zero");
			}

			quantity = cashAmount.divide(price, MathContext.DECIMAL128);

		} else {

			if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Quantity must be greater than zero");
			}
		}

		// Calculate current holdings
		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		BigDecimal currentQuantity = BigDecimal.ZERO;

		for (Transaction transaction : transactions) {

			if (!symbol.equals(transaction.getSymbol())) {
				continue;
			}

			if (transaction.getType() == TransactionType.BUY) {
				currentQuantity = currentQuantity.add(transaction.getQuantity());
			}

			if (transaction.getType() == TransactionType.SELL) {
				currentQuantity = currentQuantity.subtract(transaction.getQuantity());
			}
		}

		// Check whether the account owns enough shares
		if (currentQuantity.compareTo(quantity) < 0) {
			throw new RuntimeException("Insufficient shares");
		}

		BigDecimal totalProceeds = quantity.multiply(price);

		account.increaseCash(totalProceeds);

		accountRepository.save(account);

		Transaction transaction = new Transaction(account, TransactionType.SELL, totalProceeds, symbol, quantity,
				price);

		return transactionRepository.save(transaction);
	}

	@Transactional
	public Transaction buyStock(Long accountId, String symbol, BigDecimal quantity, BigDecimal cashAmount) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
				.orElseThrow(() -> new RuntimeException("Price not found for " + symbol));

		BigDecimal price = stockPrice.getPrice();

		// Must provide exactly one of quantity or cashAmount
		if (quantity != null && cashAmount != null) {
			throw new RuntimeException("Provide either quantity or cashAmount, not both");
		}

		if (quantity == null && cashAmount == null) {
			throw new RuntimeException("Either quantity or cashAmount is required");
		}

		BigDecimal totalCost;

		if (cashAmount != null) {
			if (cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Cash amount must be greater than zero");
			}

			totalCost = cashAmount;
			quantity = cashAmount.divide(price, MathContext.DECIMAL128);

		} else {
			if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException("Quantity must be greater than zero");
			}

			totalCost = quantity.multiply(price);
		}

		if (account.getCash().compareTo(totalCost) < 0) {
			throw new RuntimeException("Insufficient cash");
		}

		account.decreaseCash(totalCost);

		accountRepository.save(account);

		Transaction transaction = new Transaction(account, TransactionType.BUY, totalCost, symbol, quantity, price);

		return transactionRepository.save(transaction);
	}

	public List<TransactionResponse> getTransactions(Long accountId) {

		if (!accountRepository.existsById(accountId)) {
			throw new RuntimeException("Account not found");
		}

		return transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
				.map(transaction -> new TransactionResponse(transaction.getId(), transaction.getType(),
						transaction.getAmount(), transaction.getSymbol(), transaction.getQuantity(),
						transaction.getPrice(), transaction.getCreatedAt()))
				.toList();
	}

	public List<PositionResponse> getPositions(Long accountId) {

		if (!accountRepository.existsById(accountId)) {
			throw new RuntimeException("Account not found");
		}

		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		Map<String, BigDecimal> positions = new HashMap<>();

		for (Transaction transaction : transactions) {

			if (transaction.getSymbol() == null) {
				continue;
			}

			String symbol = transaction.getSymbol();

			BigDecimal currentQuantity = positions.getOrDefault(symbol, BigDecimal.ZERO);

			if (transaction.getType() == TransactionType.BUY) {
				currentQuantity = currentQuantity.add(transaction.getQuantity());
			}

			if (transaction.getType() == TransactionType.SELL) {
				currentQuantity = currentQuantity.subtract(transaction.getQuantity());
			}

			positions.put(symbol, currentQuantity);
		}

		List<PositionResponse> result = new ArrayList<>();

		for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {

			if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
				result.add(new PositionResponse(entry.getKey(), entry.getValue()));
			}
		}

		return result;
	}

	public AccountBalanceResponse getBalance(Long accountId) {

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		Map<String, BigDecimal> positions = new HashMap<>();

		for (Transaction transaction : transactions) {

			if (transaction.getSymbol() == null) {
				continue;
			}

			String symbol = transaction.getSymbol();

			BigDecimal quantity = positions.getOrDefault(symbol, BigDecimal.ZERO);

			if (transaction.getType() == TransactionType.BUY) {
				quantity = quantity.add(transaction.getQuantity());
			}

			if (transaction.getType() == TransactionType.SELL) {
				quantity = quantity.subtract(transaction.getQuantity());
			}

			positions.put(symbol, quantity);
		}

		BigDecimal stockValue = BigDecimal.ZERO;

		for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {

			BigDecimal quantity = entry.getValue();

			if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			StockPrice stockPrice = stockPriceRepository.findBySymbol(entry.getKey())
					.orElseThrow(() -> new RuntimeException("Price not found for " + entry.getKey()));

			BigDecimal marketValue = quantity.multiply(stockPrice.getPrice());

			stockValue = stockValue.add(marketValue);
		}

		BigDecimal totalBalance = account.getCash().add(stockValue);

		return new AccountBalanceResponse(account.getId(), account.getCash(), stockValue, totalBalance);
	}

	public PortfolioResponse getPortfolio(Long accountId) {

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		Map<String, BigDecimal> quantities = new HashMap<>();
		Map<String, BigDecimal> costBasis = new HashMap<>();
		Map<String, BigDecimal> realizedGainLoss = new HashMap<>();

		// Calculate current quantity and cost basis for each stock
		for (Transaction transaction : transactions) {

			if (transaction.getSymbol() == null) {
				continue;
			}

			String symbol = transaction.getSymbol();

			BigDecimal currentQuantity = quantities.getOrDefault(symbol, BigDecimal.ZERO);

			BigDecimal currentCost = costBasis.getOrDefault(symbol, BigDecimal.ZERO);

			if (transaction.getType() == TransactionType.BUY) {

				currentQuantity = currentQuantity.add(transaction.getQuantity(), MathContext.DECIMAL128);

				currentCost = currentCost.add(transaction.getAmount(), MathContext.DECIMAL128);

			} else if (transaction.getType() == TransactionType.SELL) {

				if (currentQuantity.compareTo(BigDecimal.ZERO) > 0) {

					BigDecimal sellRatio = transaction.getQuantity().divide(currentQuantity, MathContext.DECIMAL128);

					BigDecimal costToRemove = currentCost.multiply(sellRatio, MathContext.DECIMAL128);

					BigDecimal sellGainLoss = transaction.getAmount().subtract(costToRemove);

					BigDecimal currentRealizedGainLoss = realizedGainLoss.getOrDefault(symbol, BigDecimal.ZERO);

					currentRealizedGainLoss = currentRealizedGainLoss.add(sellGainLoss);

					realizedGainLoss.put(symbol, currentRealizedGainLoss);

					currentQuantity = currentQuantity.subtract(transaction.getQuantity());

					currentCost = currentCost.subtract(costToRemove);

					if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
						currentQuantity = BigDecimal.ZERO;
						currentCost = BigDecimal.ZERO;
					}
				}
			}

			quantities.put(symbol, currentQuantity);
			costBasis.put(symbol, currentCost);
		}

		// Build holdings
		List<PortfolioResponse.Holding> holdings = new ArrayList<>();

		BigDecimal stockValue = BigDecimal.ZERO;

		for (Map.Entry<String, BigDecimal> entry : quantities.entrySet()) {

			String symbol = entry.getKey();
			BigDecimal quantity = entry.getValue();

			if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
					.orElseThrow(() -> new RuntimeException("Price not found for " + symbol));

			BigDecimal currentPrice = stockPrice.getPrice();

			BigDecimal marketValue = quantity.multiply(currentPrice, MathContext.DECIMAL128);

			BigDecimal currentCostBasis = costBasis.get(symbol);

			BigDecimal averageCost = currentCostBasis.divide(quantity, MathContext.DECIMAL128);

			BigDecimal gainLoss = marketValue.subtract(currentCostBasis, MathContext.DECIMAL128);

			stockValue = stockValue.add(marketValue, MathContext.DECIMAL128);

			holdings.add(new PortfolioResponse.Holding(symbol, quantity, currentPrice, marketValue, averageCost,
					gainLoss, BigDecimal.ZERO));
		}

		// Calculate total account value
		BigDecimal totalValue = account.getCash().add(stockValue, MathContext.DECIMAL128);

		// Calculate account percentage
		List<PortfolioResponse.Holding> finalHoldings = holdings.stream().map(holding -> {

			BigDecimal accountPercentage = holding.marketValue().divide(totalValue, MathContext.DECIMAL128)
					.multiply(BigDecimal.valueOf(100), MathContext.DECIMAL128);

			return new PortfolioResponse.Holding(holding.symbol(), holding.quantity(), holding.currentPrice(),
					holding.marketValue(), holding.averageCost(), holding.gainLoss(), accountPercentage);
		}).toList();

		// Calculate total gain/loss
		BigDecimal totalUnrealizedGainLoss = finalHoldings.stream().map(PortfolioResponse.Holding::gainLoss)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalRealizedGainLoss = realizedGainLoss.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalGainLoss = totalRealizedGainLoss.add(totalUnrealizedGainLoss);

		return new PortfolioResponse(account.getCash(), totalValue, totalGainLoss, totalRealizedGainLoss,
				finalHoldings);
	}
}