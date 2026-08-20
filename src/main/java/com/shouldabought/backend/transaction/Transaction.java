package com.shouldabought.backend.transaction;

import com.shouldabought.backend.account.Account;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(length = 10)
	private String symbol;

	@Column(precision = 19, scale = 6)
	private BigDecimal quantity;

	@Column(precision = 19, scale = 6)
	private BigDecimal price;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected Transaction() {
	}

	// DEPOSIT / WITHDRAW / etc.
	public Transaction(
			Account account,
			TransactionType type,
			BigDecimal amount
	) {
		this.account = account;
		this.type = type;
		this.amount = amount;
		this.createdAt = LocalDateTime.now();
	}

	// BUY / SELL
	public Transaction(
			Account account,
			TransactionType type,
			BigDecimal amount,
			String symbol,
			BigDecimal quantity,
			BigDecimal price
	) {
		this.account = account;
		this.type = type;
		this.amount = amount;
		this.symbol = symbol;
		this.quantity = quantity;
		this.price = price;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public TransactionType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}