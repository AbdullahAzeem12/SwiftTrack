package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.FareProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FareProductRepository extends JpaRepository<FareProduct, UUID> {
    Optional<FareProduct> findByCode(String code);
}
