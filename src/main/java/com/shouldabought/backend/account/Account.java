package com.shouldabought.backend.account;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal cash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountType type;

	protected Account() {
	}

	public Account(String name, BigDecimal cash, AccountType type) {
		this.name = name;
		this.cash = cash;
		this.type = type;

	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getCash() {
		return cash;
	}

	public AccountType getType() {
		return type;
	}

	public void decreaseCash(BigDecimal amount) {
		this.cash = this.cash.subtract(amount);
	}

	public void increaseCash(BigDecimal amount) {
		this.cash = this.cash.add(amount);
	}
}
