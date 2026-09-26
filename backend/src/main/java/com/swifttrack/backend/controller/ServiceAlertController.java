package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.ServiceAlertDto;
import com.swifttrack.backend.service.ServiceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-alerts")
@RequiredArgsConstructor
public class ServiceAlertController {

    private final ServiceAlertService serviceAlertService;

    @GetMapping
    public ResponseEntity<List<ServiceAlertDto>> getActiveAlerts() {
        return ResponseEntity.ok(serviceAlertService.getActiveAlerts());
    }
}
