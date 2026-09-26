package com.swifttrack.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class PriceCalculationTest {

    @Test
    public void testBasePriceCalculationInPence() {
        int singleBasePricePence = 2500; // £25.00
        int passengerCount = 2;
        int totalPence = singleBasePricePence * passengerCount;

        assertEquals(5000, totalPence);
    }

    @Test
    public void testPromoDiscountApplication() {
        int basePence = 2500;
        int discountPercent = 20; // EXPRESS20
        int discountPence = (basePence * discountPercent) / 100;
        int finalPence = basePence - discountPence;

        assertEquals(500, discountPence);
        assertEquals(2000, finalPence);
    }
}
