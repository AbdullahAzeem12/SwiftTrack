package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.*;
import com.swifttrack.backend.domain.enums.BookingStatus;
import com.swifttrack.backend.domain.enums.TicketStatus;
import com.swifttrack.backend.dto.TicketDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.BookingRepository;
import com.swifttrack.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final JourneyService journeyService;

    @Value("${jwt.secret:default_secret_key}")
    private String hmacSecret;

    @Transactional
    public Ticket issueTicketForBooking(Booking booking) {
        // Check if ticket already issued
        List<Ticket> existing = ticketRepository.findByUserIdAndStatus(booking.getUserId(), TicketStatus.ISSUED);
        for (Ticket t : existing) {
            if (t.getBooking().getId().equals(booking.getId())) {
                return t;
            }
        }

        String code = generateTicketCode();
        ZonedDateTime validFrom = ZonedDateTime.now();
        ZonedDateTime validUntil = booking.getJourney().getScheduledDeparture().plusDays(30);

        String signedQr = generateSignedQrPayload(code, booking.getBookingReference(), validUntil);

        Ticket ticket = Ticket.builder()
                .ticketCode(code)
                .booking(booking)
                .userId(booking.getUserId())
                .journey(booking.getJourney())
                .fareProductName(booking.getQuote().getFareProduct().getName())
                .travelClass(booking.getQuote().getFareProduct().getTravelClass().name())
                .passengerCategory("ADULT")
                .signedQrPayload(signedQr)
                .status(TicketStatus.ISSUED)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .pdfUrl("/api/v1/tickets/pdf/" + code)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        booking.setStatus(BookingStatus.TICKET_ISSUED);
        bookingRepository.save(booking);

        return saved;
    }

    public Ticket getTicketForBooking(UUID bookingId) {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getBooking().getId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    public TicketDto getTicketByCode(UUID userId, String code) {
        Ticket ticket = ticketRepository.findByTicketCode(code)
                .orElseThrow(() -> new ApiException("TICKET_NOT_FOUND", "Ticket not found", HttpStatus.NOT_FOUND));

        if (!ticket.getUserId().equals(userId)) {
            throw new ApiException("UNAUTHORIZED_ACCESS", "Unauthorized access to ticket", HttpStatus.FORBIDDEN);
        }

        return mapToDto(ticket);
    }

    public List<TicketDto> getUserTickets(UUID userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private String generateTicketCode() {
        SecureRandom random = new SecureRandom();
        int part1 = 1000 + random.nextInt(9000);
        int part2 = 1000 + random.nextInt(9000);
        return "TKT-" + part1 + "-" + part2;
    }

    private String generateSignedQrPayload(String ticketCode, String bookingRef, ZonedDateTime expires) {
        try {
            String data = ticketCode + "|" + bookingRef + "|" + expires.toEpochSecond();
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            return data + "|" + sig;
        } catch (Exception e) {
            return ticketCode + "|" + bookingRef;
        }
    }

    public TicketDto mapToDto(Ticket t) {
        return TicketDto.builder()
                .ticketId(t.getId())
                .ticketCode(t.getTicketCode())
                .bookingReference(t.getBooking().getBookingReference())
                .journey(journeyService.mapToDto(t.getJourney(), List.of(t.getBooking().getQuote().getFareProduct())))
                .fareProductName(t.getFareProductName())
                .travelClass(t.getTravelClass())
                .passengerCategory(t.getPassengerCategory())
                .signedQrPayload(t.getSignedQrPayload())
                .status(t.getStatus().name())
                .validFrom(t.getValidFrom())
                .validUntil(t.getValidUntil())
                .pdfUrl(t.getPdfUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
