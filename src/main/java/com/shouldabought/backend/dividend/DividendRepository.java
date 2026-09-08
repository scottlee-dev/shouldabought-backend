package com.shouldabought.backend.dividend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

	Optional<Dividend> findByExternalId(String externalId);

	List<Dividend> findByExDividendDate(LocalDate exDividendDate);
}