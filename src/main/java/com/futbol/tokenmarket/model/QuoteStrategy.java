package com.futbol.tokenmarket.model;

import java.util.Arrays;

public enum QuoteStrategy {
    BALANCED("balanced"),
    POSITION_AWARE("position-aware");

    private final String apiValue;

    QuoteStrategy(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static QuoteStrategy fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }

        return Arrays.stream(values())
            .filter(strategy -> strategy.apiValue.equalsIgnoreCase(value.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Estrategia no soportada: " + value + ". Usar balanced o position-aware"));
    }
}