package com.shouldabought.backend.account;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shouldabought.backend.transaction.Transaction;
import com.shouldabought.backend.transaction.TransactionRepository;
import com.shouldabought.backend.transaction.TransactionResponse;
import com.shouldabought.backend.transaction.TransactionType;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
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
	public Transaction buyStock(Long accountId, String symbol, BigDecimal quantity, BigDecimal price) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new RuntimeException("Account not found"));
		BigDecimal totalCost = quantity.multiply(price);

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
}