package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shouldabought.backend.account.Account;

import jakarta.persistence.*;

@Entity
@Table(name = "dividend_entitlements", uniqueConstraints = {
		@UniqueConstraint(name = "uk_entitlement_account_dividend", columnNames = { "account_id", "dividend_id" }) })
public class DividendEntitlement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dividend_id", nullable = false)
	private Dividend dividend;

	@Column(nullable = false, precision = 19, scale = 12)
	private BigDecimal qualifiedQuantity;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false)
	private boolean paid;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime paidAt;

	protected DividendEntitlement() {
	}

	public DividendEntitlement(Account account, Dividend dividend, BigDecimal qualifiedQuantity, BigDecimal amount) {

		this.account = account;
		this.dividend = dividend;
		this.qualifiedQuantity = qualifiedQuantity;
		this.amount = amount;
		this.paid = false;
		this.createdAt = LocalDateTime.now();
	}

	public void markPaid() {
		this.paid = true;
		this.paidAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public Dividend getDividend() {
		return dividend;
	}

	public BigDecimal getQualifiedQuantity() {
		return qualifiedQuantity;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public boolean isPaid() {
		return paid;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}
}