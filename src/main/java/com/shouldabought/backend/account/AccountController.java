package com.shouldabought.backend.account;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.shouldabought.backend.position.PositionResponse;
import com.shouldabought.backend.transaction.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
	private final AccountService accountService;
	private final AccountRepository accountRepository;

	public AccountController(AccountService accountService, AccountRepository accountRepository) {
		this.accountService = accountService;
		this.accountRepository = accountRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Account createAccount(@RequestBody CreateAccountRequest request) {
		return accountService.createAccount(request.name(), request.initialCash());
	}

	@GetMapping("/{id}")
	public Account getAccount(@PathVariable Long id) {
		return accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found"));
	}

	public record CreateAccountRequest(String name, BigDecimal initialCash) {
	}

	@PostMapping("/{id}/buy")
	public TransactionResponse buyStock(@PathVariable Long id, @RequestBody TransactionBuyRequest request) {
		Transaction transaction = accountService.buyStock(id, request.symbol(), request.quantity(),
				request.cashAmount());
		return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
				transaction.getSymbol(), transaction.getQuantity(), transaction.getPrice(), transaction.getCreatedAt());
	}

	@PostMapping("/{id}/sell")
	public TransactionResponse sellStock(@PathVariable Long id, @RequestBody TransactionSellRequest request) {
		Transaction transaction = accountService.sellStock(id, request.symbol(), request.quantity(),
				request.cashAmount());
		return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
				transaction.getSymbol(), transaction.getQuantity(), transaction.getPrice(), transaction.getCreatedAt());
	}

	@PostMapping("/{id}/dividend")
	public TransactionResponse processDividend(@PathVariable Long id, @RequestBody DividendRequest request) {
		Transaction transaction = accountService.processDividend(id, request.symbol(), request.amount(),
				request.reinvest());
		return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
				transaction.getSymbol(), transaction.getQuantity(), transaction.getPrice(), transaction.getCreatedAt());
	}

	@GetMapping("/{id}/transactions")
	public List<TransactionResponse> getTransactions(@PathVariable Long id) {
		return accountService.getTransactions(id);
	}

	@GetMapping("/{id}/positions")
	public List<PositionResponse> getPositions(@PathVariable Long id) {
		return accountService.getPositions(id);
	}

	@GetMapping("/{id}/balance")
	public AccountBalanceResponse getBalance(@PathVariable Long id) {
		return accountService.getBalance(id);
	}

	@GetMapping("/{id}/portfolio")
	public PortfolioResponse getPortfolio(@PathVariable Long id) {
		return accountService.getPortfolio(id);
	}
}