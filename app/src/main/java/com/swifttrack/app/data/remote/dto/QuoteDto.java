package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class QuoteDto {

    public static class CreateQuoteRequest {
        private UUID journeyId;
        private UUID fareProductId;
        private Integer passengerCount = 1;
        private String promoCode;

        public CreateQuoteRequest() {}

        public UUID getJourneyId() { return journeyId; }
        public void setJourneyId(UUID journeyId) { this.journeyId = journeyId; }

        public UUID getFareProductId() { return fareProductId; }
        public void setFareProductId(UUID fareProductId) { this.fareProductId = fareProductId; }

        public Integer getPassengerCount() { return passengerCount; }
        public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }

        public String getPromoCode() { return promoCode; }
        public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    }

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
        private String expiresAt;

        public QuoteResponse() {}

        public UUID getQuoteId() { return quoteId; }
        public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

        public UUID getJourneyId() { return journeyId; }
        public void setJourneyId(UUID journeyId) { this.journeyId = journeyId; }

        public UUID getFareProductId() { return fareProductId; }
        public void setFareProductId(UUID fareProductId) { this.fareProductId = fareProductId; }

        public Integer getPassengerCount() { return passengerCount; }
        public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }

        public String getPromoCode() { return promoCode; }
        public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

        public Integer getBaseAmountMinor() { return baseAmountMinor; }
        public void setBaseAmountMinor(Integer baseAmountMinor) { this.baseAmountMinor = baseAmountMinor; }

        public Integer getDiscountAmountMinor() { return discountAmountMinor; }
        public void setDiscountAmountMinor(Integer discountAmountMinor) { this.discountAmountMinor = discountAmountMinor; }

        public Integer getFinalAmountMinor() { return finalAmountMinor; }
        public void setFinalAmountMinor(Integer finalAmountMinor) { this.finalAmountMinor = finalAmountMinor; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }

        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    }
}
