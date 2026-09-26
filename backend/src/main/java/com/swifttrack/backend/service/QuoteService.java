package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.*;
import com.swifttrack.backend.dto.QuoteDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final JourneyRepository journeyRepository;
    private final FareProductRepository fareProductRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PriceQuoteRepository priceQuoteRepository;

    @Value("${jwt.secret:default_secret_key}")
    private String hmacSecret;

    @Transactional
    public QuoteDto.QuoteResponse createQuote(UUID userId, QuoteDto.CreateQuoteRequest request) {
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ApiException("JOURNEY_NOT_FOUND", "Journey not found", HttpStatus.NOT_FOUND));

        FareProduct fareProduct = fareProductRepository.findById(request.getFareProductId())
                .orElseThrow(() -> new ApiException("FARE_NOT_FOUND", "Fare product not found", HttpStatus.NOT_FOUND));

        int count = request.getPassengerCount() != null ? request.getPassengerCount() : 1;
        int baseAmountMinor = fareProduct.getBasePriceMinor() * count;
        int discountAmountMinor = 0;

        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            PromoCode promo = promoCodeRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getPromoCode().trim())
                    .orElseThrow(() -> new ApiException("INVALID_PROMO", "Promo code is invalid or expired", HttpStatus.UNPROCESSABLE_ENTITY));

            ZonedDateTime now = ZonedDateTime.now();
            if (now.isBefore(promo.getValidFrom()) || now.isAfter(promo.getValidTo()) || promo.getTimesUsed() >= promo.getUsageLimit()) {
                throw new ApiException("PROMO_EXPIRED", "Promo code is no longer active", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            int calcDiscount = (baseAmountMinor * promo.getDiscountPercent()) / 100;
            if (promo.getMaxDiscountMinor() != null && calcDiscount > promo.getMaxDiscountMinor()) {
                calcDiscount = promo.getMaxDiscountMinor();
            }
            discountAmountMinor = calcDiscount;
        }

        int finalAmountMinor = Math.max(0, baseAmountMinor - discountAmountMinor);

        ZonedDateTime expiresAt = ZonedDateTime.now().plusMinutes(15);
        String signature = generateSignature(journey.getId(), fareProduct.getId(), finalAmountMinor, expiresAt);

        PriceQuote quote = PriceQuote.builder()
                .userId(userId)
                .journey(journey)
                .fareProduct(fareProduct)
                .passengerCount(count)
                .promoCode(request.getPromoCode())
                .baseAmountMinor(baseAmountMinor)
                .discountAmountMinor(discountAmountMinor)
                .finalAmountMinor(finalAmountMinor)
                .currency("GBP")
                .signature(signature)
                .expiresAt(expiresAt)
                .build();

        PriceQuote savedQuote = priceQuoteRepository.save(quote);

        return QuoteDto.QuoteResponse.builder()
                .quoteId(savedQuote.getId())
                .journeyId(journey.getId())
                .fareProductId(fareProduct.getId())
                .passengerCount(count)
                .promoCode(request.getPromoCode())
                .baseAmountMinor(baseAmountMinor)
                .discountAmountMinor(discountAmountMinor)
                .finalAmountMinor(finalAmountMinor)
                .currency("GBP")
                .signature(signature)
                .expiresAt(expiresAt)
                .build();
    }

    private String generateSignature(UUID journeyId, UUID fareId, int amount, ZonedDateTime expiresAt) {
        try {
            String raw = journeyId.toString() + ":" + fareId.toString() + ":" + amount + ":" + expiresAt.toEpochSecond() + ":" + hmacSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "signature_error";
        }
    }
}
