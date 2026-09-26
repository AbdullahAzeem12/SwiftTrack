package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {
    Optional<Station> findByCode(String code);
    List<Station> findByIsActiveTrue();
    List<Station> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
