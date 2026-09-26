package com.swifttrack.app.core.payment;

import java.io.Serializable;
import java.util.Locale;

public class CurrencyConfig implements Serializable {

    private String currencyCode; // "GBP", "USD", "PKR"
    private String symbol;       // "£", "$", "Rs"
    private int minorUnitFactor; // 100 for GBP/USD/PKR (pence/cents/paisa)

    public CurrencyConfig(String currencyCode, String symbol, int minorUnitFactor) {
        this.currencyCode = currencyCode;
        this.symbol = symbol;
        this.minorUnitFactor = minorUnitFactor;
    }

    public static CurrencyConfig GBP() { return new CurrencyConfig("GBP", "£", 100); }
    public static CurrencyConfig USD() { return new CurrencyConfig("USD", "$", 100); }
    public static CurrencyConfig PKR() { return new CurrencyConfig("PKR", "Rs ", 100); }

    public static CurrencyConfig fromCode(String code) {
        if (code == null) return GBP();
        if ("USD".equalsIgnoreCase(code)) return USD();
        if ("PKR".equalsIgnoreCase(code)) return PKR();
        return GBP();
    }

    public String getCurrencyCode() { return currencyCode; }
    public String getSymbol() { return symbol; }
    public int getMinorUnitFactor() { return minorUnitFactor; }

    public long toMinorUnits(double amount) {
        return Math.round(amount * minorUnitFactor);
    }

    public String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%s%.2f", symbol, amount);
    }
}
