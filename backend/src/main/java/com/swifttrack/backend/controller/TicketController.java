package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.TicketDto;
import com.swifttrack.backend.security.UserPrincipal;
import com.swifttrack.backend.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getUserTickets(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.getUserTickets(currentUser.getId()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<TicketDto> getTicketByCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String code) {
        return ResponseEntity.ok(ticketService.getTicketByCode(currentUser.getId(), code));
    }
}
