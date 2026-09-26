package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.StationDto;
import com.swifttrack.backend.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public ResponseEntity<List<StationDto>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllActiveStations());
    }

    @GetMapping("/search")
    public ResponseEntity<List<StationDto>> searchStations(@RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(stationService.searchStations(query));
    }
}
