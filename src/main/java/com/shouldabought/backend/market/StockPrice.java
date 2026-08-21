package com.shouldabought.backend.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_prices")
public class StockPrice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 10)
	private String symbol;

	@Column(nullable = false, precision = 19, scale = 6)
	private BigDecimal price;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected StockPrice() {
	}

	public StockPrice(String symbol, BigDecimal price, LocalDateTime updatedAt) {
		this.symbol = symbol;
		this.price = price;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void updatePrice(BigDecimal price, LocalDateTime updatedAt) {
		this.price = price;
		this.updatedAt = updatedAt;
	}
}