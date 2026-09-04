package com.shouldabought.backend.dividend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

	Optional<Dividend> findByExternalId(String externalId);
}