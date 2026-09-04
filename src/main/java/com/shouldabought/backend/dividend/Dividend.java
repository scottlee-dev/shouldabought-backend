package com.shouldabought.backend.dividend;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "dividends", uniqueConstraints = {
		@UniqueConstraint(
				name = "uk_dividend_external_id",
				columnNames = "external_id"
		)
})
public class Dividend {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "external_id", nullable = false, unique = true)
	private String externalId;

	@Column(nullable = false, length = 10)
	private String symbol;

	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal amountPerShare;

	@Column
	private LocalDate declarationDate;

	@Column
	private LocalDate exDividendDate;

	@Column
	private LocalDate recordDate;

	/*
	 * Yahoo Finance does not provide a payment date
	 * in the dividend event data.
	 */
	@Column
	private LocalDate payDate;

	protected Dividend() {
	}

	public Dividend(
			String externalId,
			String symbol,
			BigDecimal amountPerShare,
			LocalDate declarationDate,
			LocalDate exDividendDate,
			LocalDate recordDate,
			LocalDate payDate) {

		this.externalId = externalId;
		this.symbol = symbol;
		this.amountPerShare = amountPerShare;
		this.declarationDate = declarationDate;
		this.exDividendDate = exDividendDate;
		this.recordDate = recordDate;
		this.payDate = payDate;
	}

	public Long getId() {
		return id;
	}

	public String getExternalId() {
		return externalId;
	}

	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getAmountPerShare() {
		return amountPerShare;
	}

	public LocalDate getDeclarationDate() {
		return declarationDate;
	}

	public LocalDate getExDividendDate() {
		return exDividendDate;
	}

	public LocalDate getRecordDate() {
		return recordDate;
	}

	public LocalDate getPayDate() {
		return payDate;
	}
}