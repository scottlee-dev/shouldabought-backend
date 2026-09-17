package com.shouldabought.backend.account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.market.AlphaVantageService;
import com.shouldabought.backend.market.StockPrice;
import com.shouldabought.backend.market.StockPriceRepository;
import com.shouldabought.backend.transaction.Transaction;
import com.shouldabought.backend.transaction.TransactionRepository;
import com.shouldabought.backend.transaction.TransactionResponse;
import com.shouldabought.backend.transaction.TransactionType;

@Service
public class AccountService {
	private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final StockPriceRepository stockPriceRepository;
	private final AlphaVantageService alphaVantageService;

	public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
			StockPriceRepository stockPriceRepository, AlphaVantageService alphaVantageService) {

		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.stockPriceRepository = stockPriceRepository;
		this.alphaVantageService = alphaVantageService;
	}

	@Transactional
	public Account createAccount(String name, BigDecimal initialCash) {

		if (name == null || name.isBlank()) {
			throw new RuntimeException("Account name is required");
		}

		if (initialCash == null) {
			throw new RuntimeException("Initial cash is required");
		}

		if (initialCash.compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("Initial cash cannot be negative");
		}

		Account account = new Account(name.trim(), initialCash, AccountType.REALITY);

		accountRepository.save(account);

		Transaction deposit = new Transaction(account, TransactionType.DEPOSIT, initialCash);

		transactionRepository.save(deposit);

		return account;
	}

	@Transactional
	public Transaction sellStock(Long accountId, String symbol, BigDecimal quantity, BigDecimal cashAmount) {

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		// Must provide exactly one of quantity or cashAmount
		if (quantity != null && cashAmount != null) {
			throw new RuntimeException("Provide either quantity or cashAmount, not both");
		}

		if (quantity == null && cashAmount == null) {
			throw new RuntimeException("Either quantity or cashAmount is required");
		}

		if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("Cash amount must be greater than zero");
		}

		if (quantity != null && quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("Quantity must be greater than zero");
		}

		symbol = symbol.trim().toUpperCase();

		StockPrice stockPrice = refreshStockPrice(symbol);

		BigDecimal price = stockPrice.getPrice();

		// Calculate how many shares are being sold
		if (cashAmount != null) {
			quantity = cashAmount.divide(price, MathContext.DECIMAL128);
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

		if (symbol == null || symbol.isBlank()) {
			throw new RuntimeException("Symbol is required");
		}

		if (quantity != null && cashAmount != null) {
			throw new RuntimeException("Provide either quantity or cashAmount, not both");
		}

		if (quantity == null && cashAmount == null) {
			throw new RuntimeException("Either quantity or cashAmount is required");
		}

		if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("Cash amount must be greater than zero");
		}

		if (quantity != null && quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("Quantity must be greater than zero");
		}

		symbol = symbol.trim().toUpperCase();

		StockPrice stockPrice = refreshStockPrice(symbol);

		BigDecimal price = stockPrice.getPrice();

		BigDecimal totalCost;

		if (cashAmount != null) {

			totalCost = cashAmount;
			quantity = cashAmount.divide(price, MathContext.DECIMAL128);

		} else {

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

	public List<PortfolioResponse.PositionResponse> getPositions(Long accountId) {

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

		List<PortfolioResponse.PositionResponse> result = new ArrayList<>();

		for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {

			if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
				result.add(new PortfolioResponse.PositionResponse(entry.getKey(), entry.getValue()));
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

		BigDecimal roundedStockValue = stockValue.setScale(4, RoundingMode.HALF_UP);

		BigDecimal totalBalance = account.getCash().add(roundedStockValue).setScale(4, RoundingMode.HALF_UP);

		return new AccountBalanceResponse(account.getId(), account.getCash().setScale(4, RoundingMode.HALF_UP),
				roundedStockValue, totalBalance);
	}

	public PortfolioResponse getPortfolio(Long accountId) {

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

		Map<String, List<CostLot>> lotsBySymbol = new HashMap<>();

		Map<String, BigDecimal> realizedGainLoss = new HashMap<>();

		for (Transaction transaction : transactions) {

			if (transaction.getSymbol() == null) {
				continue;
			}

			String symbol = transaction.getSymbol();

			List<CostLot> lots = lotsBySymbol.computeIfAbsent(symbol, key -> new ArrayList<>());

			if (transaction.getType() == TransactionType.BUY) {

				lots.add(new CostLot(transaction.getQuantity(), transaction.getAmount()));

			} else if (transaction.getType() == TransactionType.SELL) {

				BigDecimal remainingToSell = transaction.getQuantity();

				BigDecimal totalCostRemoved = BigDecimal.ZERO;

				while (remainingToSell.compareTo(BigDecimal.ZERO) > 0 && !lots.isEmpty()) {

					CostLot oldestLot = lots.getFirst();

					BigDecimal sharesFromLot = remainingToSell.min(oldestLot.quantity);

					BigDecimal costPerShare = oldestLot.cost.divide(oldestLot.quantity, MathContext.DECIMAL128);

					BigDecimal costRemoved = sharesFromLot.multiply(costPerShare, MathContext.DECIMAL128);

					totalCostRemoved = totalCostRemoved.add(costRemoved, MathContext.DECIMAL128);

					oldestLot.quantity = oldestLot.quantity.subtract(sharesFromLot, MathContext.DECIMAL128);

					oldestLot.cost = oldestLot.cost.subtract(costRemoved, MathContext.DECIMAL128);

					remainingToSell = remainingToSell.subtract(sharesFromLot, MathContext.DECIMAL128);

					if (oldestLot.quantity.compareTo(BigDecimal.ZERO) <= 0) {
						lots.removeFirst();
					}
				}

				if (remainingToSell.compareTo(BigDecimal.ZERO) > 0) {
					throw new RuntimeException("Insufficient shares for FIFO calculation: " + symbol);
				}

				BigDecimal saleProceeds = transaction.getAmount();

				BigDecimal sellGainLoss = saleProceeds.subtract(totalCostRemoved, MathContext.DECIMAL128);

				BigDecimal currentRealizedGainLoss = realizedGainLoss.getOrDefault(symbol, BigDecimal.ZERO);

				realizedGainLoss.put(symbol, currentRealizedGainLoss.add(sellGainLoss, MathContext.DECIMAL128));
			}
		}

		List<PortfolioResponse.Holding> holdings = new ArrayList<>();

		BigDecimal stockValue = BigDecimal.ZERO;

		for (Map.Entry<String, List<CostLot>> entry : lotsBySymbol.entrySet()) {

			String symbol = entry.getKey();

			List<CostLot> lots = entry.getValue();

			if (lots.isEmpty()) {
				continue;
			}

			StockPrice stockPrice = stockPriceRepository.findBySymbol(symbol)
					.orElseThrow(() -> new RuntimeException("Price not found for " + symbol));

			BigDecimal currentPrice = stockPrice.getPrice();

			BigDecimal totalQuantity = BigDecimal.ZERO;
			BigDecimal totalCostBasis = BigDecimal.ZERO;

			for (CostLot lot : lots) {

				if (lot.quantity.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				totalQuantity = totalQuantity.add(lot.quantity, MathContext.DECIMAL128);

				totalCostBasis = totalCostBasis.add(lot.cost, MathContext.DECIMAL128);
			}

			if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			BigDecimal marketValue = totalQuantity.multiply(currentPrice, MathContext.DECIMAL128);

			BigDecimal averageCost = totalCostBasis.divide(totalQuantity, MathContext.DECIMAL128);

			BigDecimal gainLoss = marketValue.subtract(totalCostBasis, MathContext.DECIMAL128);

			stockValue = stockValue.add(marketValue, MathContext.DECIMAL128);

			BigDecimal gainLossPercentage = BigDecimal.ZERO;

			if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {

				gainLossPercentage = gainLoss.divide(totalCostBasis, MathContext.DECIMAL128)
						.multiply(BigDecimal.valueOf(100), MathContext.DECIMAL128);
			}

			holdings.add(new PortfolioResponse.Holding(symbol, totalQuantity.setScale(12, RoundingMode.HALF_UP),
					currentPrice.setScale(4, RoundingMode.HALF_UP), marketValue.setScale(4, RoundingMode.HALF_UP),
					averageCost.setScale(4, RoundingMode.HALF_UP), gainLoss.setScale(4, RoundingMode.HALF_UP),
					gainLossPercentage.setScale(4, RoundingMode.HALF_UP), BigDecimal.ZERO));
		}

		BigDecimal totalValue = account.getCash().add(stockValue, MathContext.DECIMAL128);

		List<PortfolioResponse.Holding> finalHoldings = holdings.stream().map(holding -> {

			BigDecimal accountPercentage = BigDecimal.ZERO;

			if (totalValue.compareTo(BigDecimal.ZERO) > 0) {

				accountPercentage = holding.marketValue().divide(totalValue, MathContext.DECIMAL128)
						.multiply(BigDecimal.valueOf(100), MathContext.DECIMAL128).setScale(4, RoundingMode.HALF_UP);
			}

			return new PortfolioResponse.Holding(holding.symbol(), holding.quantity(), holding.currentPrice(),
					holding.marketValue(), holding.averageCost(), holding.gainLoss(), holding.gainLossPercentage(),
					accountPercentage);

		}).toList();

		BigDecimal totalGainLoss = finalHoldings.stream().map(PortfolioResponse.Holding::gainLoss)
				.reduce(BigDecimal.ZERO, (a, b) -> a.add(b, MathContext.DECIMAL128));

		BigDecimal totalRealizedGainLoss = realizedGainLoss.values().stream().reduce(BigDecimal.ZERO,
				(a, b) -> a.add(b, MathContext.DECIMAL128));

		return new PortfolioResponse(account.getCash().setScale(4, RoundingMode.HALF_UP),
				totalValue.setScale(4, RoundingMode.HALF_UP), totalGainLoss.setScale(4, RoundingMode.HALF_UP),
				totalRealizedGainLoss.setScale(4, RoundingMode.HALF_UP), finalHoldings);
	}

	private static class CostLot {

		private BigDecimal quantity;
		private BigDecimal cost;

		public CostLot(BigDecimal quantity, BigDecimal cost) {
			this.quantity = quantity;
			this.cost = cost;
		}
	}

	public Account getAccount(Long accountId) {

		return accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));
	}

	private StockPrice refreshStockPrice(String symbol) {

		String normalizedSymbol = symbol.trim().toUpperCase();

		BigDecimal price = alphaVantageService.getCurrentPrice(normalizedSymbol);

		LocalDateTime now = LocalDateTime.now(MARKET_ZONE);

		StockPrice stockPrice = stockPriceRepository.findBySymbol(normalizedSymbol)
				.orElseGet(() -> new StockPrice(normalizedSymbol, price, now));

		stockPrice.updatePrice(price, now);

		return stockPriceRepository.save(stockPrice);
	}
}
