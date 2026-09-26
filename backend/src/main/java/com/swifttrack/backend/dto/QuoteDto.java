package com.swifttrack.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

public class QuoteDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuoteRequest {
        @NotNull
        private UUID journeyId;
        @NotNull
        private UUID fareProductId;
        @Min(1)
        private Integer passengerCount = 1;
        private String promoCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteResponse {
        private UUID quoteId;
        private UUID journeyId;
        private UUID fareProductId;
        private Integer passengerCount;
        private String promoCode;
        private Integer baseAmountMinor;
        private Integer discountAmountMinor;
        private Integer finalAmountMinor;
        private String currency;
        private String signature;
        private ZonedDateTime expiresAt;
    }
}
