package com.shouldabought.backend.portfolio;

import com.shouldabought.backend.account.Account;
import com.shouldabought.backend.account.AccountRepository;
import com.shouldabought.backend.account.AccountService;
import com.shouldabought.backend.account.PortfolioResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PortfolioSnapshotService {

    private static final ZoneId MARKET_ZONE =
            ZoneId.of("America/New_York");

    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public PortfolioSnapshotService(
            PortfolioSnapshotRepository portfolioSnapshotRepository,
            AccountRepository accountRepository,
            AccountService accountService
    ) {
        this.portfolioSnapshotRepository = portfolioSnapshotRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    public PortfolioSnapshot createSnapshot(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        PortfolioResponse portfolio =
                accountService.getPortfolio(accountId);

        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                account,
                portfolio.totalValue(),
                LocalDateTime.now(MARKET_ZONE)
        );

        return portfolioSnapshotRepository.save(snapshot);
    }
    public List<PortfolioSnapshot> getHistory(Long accountId) {
        return portfolioSnapshotRepository
                .findByAccount_IdOrderByRecordedAtAsc(accountId);
    }
}