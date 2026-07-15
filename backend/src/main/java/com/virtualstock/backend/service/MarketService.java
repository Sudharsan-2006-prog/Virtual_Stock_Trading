package com.virtualstock.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class MarketService {

    // Simple mock to return prices
    private final Map<String, BigDecimal> mockPrices = new HashMap<>();

    public MarketService() {
        mockPrices.put("AAPL", new BigDecimal("150.00"));
        mockPrices.put("GOOGL", new BigDecimal("2800.00"));
        mockPrices.put("MSFT", new BigDecimal("300.00"));
        mockPrices.put("AMZN", new BigDecimal("3300.00"));
        mockPrices.put("TSLA", new BigDecimal("700.00"));
    }

    public BigDecimal getCurrentPrice(String symbol, BigDecimal fallbackPrice) {
        // If symbol exists in mock, return it, else use fallback price (which could be the one sent from frontend)
        if (mockPrices.containsKey(symbol.toUpperCase())) {
            // Add a small random variation to simulate live prices
            BigDecimal basePrice = mockPrices.get(symbol.toUpperCase());
            double randomFactor = 0.95 + (Math.random() * 0.1); // +/- 5%
            return basePrice.multiply(new BigDecimal(randomFactor)).setScale(2, RoundingMode.HALF_UP);
        }
        return fallbackPrice;
    }
}
