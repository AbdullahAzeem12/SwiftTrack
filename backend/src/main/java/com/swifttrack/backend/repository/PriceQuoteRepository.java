package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, UUID> {
}
