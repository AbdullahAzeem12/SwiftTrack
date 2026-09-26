package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.ServiceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceAlertRepository extends JpaRepository<ServiceAlert, UUID> {
    List<ServiceAlert> findByIsActiveTrue();
}
