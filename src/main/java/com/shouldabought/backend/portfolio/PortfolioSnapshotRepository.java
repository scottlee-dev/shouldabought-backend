package com.shouldabought.backend.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioSnapshotRepository
        extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByAccount_IdOrderByRecordedAtAsc(Long accountId);
}