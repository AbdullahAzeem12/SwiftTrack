package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.JourneyDto;
import com.swifttrack.backend.service.JourneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    @GetMapping("/search")
    public ResponseEntity<List<JourneyDto>> searchJourneys(
            @RequestParam UUID originId,
            @RequestParam UUID destinationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime date) {
        return ResponseEntity.ok(journeyService.searchJourneys(originId, destinationId, date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JourneyDto> getJourneyDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(journeyService.getJourneyDetails(id));
    }
}
