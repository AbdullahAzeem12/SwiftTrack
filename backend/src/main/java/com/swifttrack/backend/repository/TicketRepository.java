package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.Ticket;
import com.swifttrack.backend.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Ticket> findByUserIdAndStatus(UUID userId, TicketStatus status);
}
