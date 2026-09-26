package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.QuoteDto;
import com.swifttrack.backend.security.UserPrincipal;
import com.swifttrack.backend.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping
    public ResponseEntity<QuoteDto.QuoteResponse> createQuote(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody QuoteDto.CreateQuoteRequest request) {
        return ResponseEntity.ok(quoteService.createQuote(currentUser.getId(), request));
    }
}
