package com.shouldabought.backend.dividend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendEntitlementRepository extends JpaRepository<DividendEntitlement, Long> {

	Optional<DividendEntitlement> findByAccountIdAndDividendId(Long accountId, Long dividendId);

	List<DividendEntitlement> findByAccountIdOrderByCreatedAtAsc(Long accountId);

	List<DividendEntitlement> findByPaidFalse();
}