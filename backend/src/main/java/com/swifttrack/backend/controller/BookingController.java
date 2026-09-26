package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.BookingDto;
import com.swifttrack.backend.security.UserPrincipal;
import com.swifttrack.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto.BookingResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody BookingDto.CreateBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(currentUser.getId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDto.BookingResponse> getBooking(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(currentUser.getId(), id));
    }

    @GetMapping
    public ResponseEntity<List<BookingDto.BookingResponse>> getUserBookings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser.getId()));
    }
}
