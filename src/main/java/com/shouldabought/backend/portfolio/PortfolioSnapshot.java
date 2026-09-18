package com.shouldabought.backend.portfolio;

import com.shouldabought.backend.account.Account;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshots")
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected PortfolioSnapshot() {
    }

    public PortfolioSnapshot(
            Account account,
            BigDecimal totalValue,
            LocalDateTime recordedAt
    ) {
        this.account = account;
        this.totalValue = totalValue;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return account.getId();
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}