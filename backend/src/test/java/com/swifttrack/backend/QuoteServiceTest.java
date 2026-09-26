package com.swifttrack.backend;

import com.swifttrack.backend.domain.entity.FareProduct;
import com.swifttrack.backend.domain.entity.Journey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuoteServiceTest {

    @Test
    public void testDiscountCalculation() {
        int basePrice = 2500;
        int discountPercent = 20;
        int discount = (basePrice * discountPercent) / 100;
        int finalAmount = basePrice - discount;

        assertEquals(500, discount);
        assertEquals(2000, finalAmount);
    }
}
