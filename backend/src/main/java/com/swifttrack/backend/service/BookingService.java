package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.*;
import com.swifttrack.backend.domain.enums.BookingStatus;
import com.swifttrack.backend.dto.BookingDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.BookingRepository;
import com.swifttrack.backend.repository.PriceQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final JourneyService journeyService;

    @Transactional
    public BookingDto.BookingResponse createBooking(UUID userId, BookingDto.CreateBookingRequest request) {
        PriceQuote quote = priceQuoteRepository.findById(request.getQuoteId())
                .orElseThrow(() -> new ApiException("QUOTE_NOT_FOUND", "Price quote not found", HttpStatus.NOT_FOUND));

        if (ZonedDateTime.now().isAfter(quote.getExpiresAt())) {
            throw new ApiException("EXPIRED_QUOTE", "Price quote has expired. Please refresh your journey search.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String ref = generateBookingReference();

        Booking booking = Booking.builder()
                .bookingReference(ref)
                .userId(userId)
                .quote(quote)
                .journey(quote.getJourney())
                .status(BookingStatus.PAYMENT_PENDING)
                .totalAmountMinor(quote.getFinalAmountMinor())
                .currency(quote.getCurrency())
                .contactEmail(request.getContactEmail())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return mapToDto(savedBooking);
    }

    public BookingDto.BookingResponse getBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("UNAUTHORIZED_ACCESS", "You do not have access to this booking", HttpStatus.FORBIDDEN);
        }

        return mapToDto(booking);
    }

    public List<BookingDto.BookingResponse> getUserBookings(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private String generateBookingReference() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);
        return "ST-" + number;
    }

    public BookingDto.BookingResponse mapToDto(Booking b) {
        return BookingDto.BookingResponse.builder()
                .bookingId(b.getId())
                .bookingReference(b.getBookingReference())
                .status(b.getStatus().name())
                .totalAmountMinor(b.getTotalAmountMinor())
                .currency(b.getCurrency())
                .contactEmail(b.getContactEmail())
                .journey(journeyService.mapToDto(b.getJourney(), List.of(b.getQuote().getFareProduct())))
                .createdAt(b.getCreatedAt())
                .build();
    }
}
