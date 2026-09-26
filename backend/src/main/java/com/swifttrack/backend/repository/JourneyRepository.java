package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    @Query("SELECT j FROM Journey j WHERE j.departureStation.id = :originId " +
           "AND j.arrivalStation.id = :destId " +
           "AND j.scheduledDeparture >= :afterTime " +
           "AND j.isActive = true " +
           "ORDER BY j.scheduledDeparture ASC")
    List<Journey> findAvailableJourneys(
            @Param("originId") UUID originId,
            @Param("destId") UUID destId,
            @Param("afterTime") ZonedDateTime afterTime
    );

    @Query("SELECT j FROM Journey j WHERE j.departureStation.id = :originId " +
           "AND j.scheduledDeparture >= :afterTime " +
           "AND j.isActive = true " +
           "ORDER BY j.scheduledDeparture ASC")
    List<Journey> findNextDepartures(
            @Param("originId") UUID originId,
            @Param("afterTime") ZonedDateTime afterTime
    );
}
