package com.virtualstock.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketService {

    @Value("${alphavantage.api.key}")
    private String apiKey;

    private final Map<String, BigDecimal> mockPrices = new HashMap<>();
    private final Map<String, String> mockCompanyNames = new HashMap<>();
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    private static class CachedQuote {
        BigDecimal price;
        String companyName;
        BigDecimal dailyChange;
        BigDecimal changePercent;
        long timestamp;

        CachedQuote(BigDecimal price, String companyName, BigDecimal dailyChange, BigDecimal changePercent) {
            this.price = price;
            this.companyName = companyName;
            this.dailyChange = dailyChange;
            this.changePercent = changePercent;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 60000; // 1 minute cache
        }
    }

    public MarketService() {
        mockPrices.put("AAPL", new BigDecimal("150.00"));
        mockPrices.put("GOOGL", new BigDecimal("2800.00"));
        mockPrices.put("MSFT", new BigDecimal("300.00"));
        mockPrices.put("AMZN", new BigDecimal("3300.00"));
        mockPrices.put("TSLA", new BigDecimal("700.00"));

        mockCompanyNames.put("AAPL", "Apple Inc.");
        mockCompanyNames.put("GOOGL", "Alphabet Inc.");
        mockCompanyNames.put("MSFT", "Microsoft Corporation");
        mockCompanyNames.put("AMZN", "Amazon.com, Inc.");
        mockCompanyNames.put("TSLA", "Tesla, Inc.");
    }

    public BigDecimal getCurrentPrice(String symbol, BigDecimal fallbackPrice) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return fallbackPrice;
        }

        String cleanSymbol = symbol.trim().toUpperCase();

        // 1. Check cache first
        CachedQuote cached = cache.get(cleanSymbol);
        if (cached != null && !cached.isExpired()) {
            return cached.price;
        }

        // 2. Fetch live price
        Map<String, Object> quoteMap = getStockQuote(cleanSymbol);
        BigDecimal price = (BigDecimal) quoteMap.get("price");
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            return price;
        }

        if (fallbackPrice != null && fallbackPrice.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackPrice;
        }

        return price;
    }

    public Map<String, Object> getStockQuote(String symbol) {
        Map<String, Object> quoteData = new HashMap<>();
        if (symbol == null || symbol.trim().isEmpty()) {
            quoteData.put("price", BigDecimal.ZERO);
            quoteData.put("companyName", "");
            quoteData.put("dailyChange", BigDecimal.ZERO);
            quoteData.put("changePercent", BigDecimal.ZERO);
            return quoteData;
        }

        String cleanSymbol = symbol.trim().toUpperCase();

        // 1. Check cache first
        CachedQuote cached = cache.get(cleanSymbol);
        if (cached != null && !cached.isExpired()) {
            quoteData.put("price", cached.price);
            quoteData.put("companyName", cached.companyName);
            quoteData.put("dailyChange", cached.dailyChange);
            quoteData.put("changePercent", cached.changePercent);
            return quoteData;
        }

        // 2. Fetch live price from Alpha Vantage
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal change = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;
        String name = mockCompanyNames.getOrDefault(cleanSymbol, cleanSymbol);
        boolean success = false;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + cleanSymbol + "&apikey=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode quote = root.path("Global Quote");

            if (quote != null && !quote.isMissingNode() && quote.has("05. price")) {
                price = new BigDecimal(quote.get("05. price").asText()).setScale(2, RoundingMode.HALF_UP);
                change = new BigDecimal(quote.get("09. change").asText()).setScale(2, RoundingMode.HALF_UP);
                
                String pctStr = quote.get("10. change percent").asText().replace("%", "");
                changePercent = new BigDecimal(pctStr).setScale(2, RoundingMode.HALF_UP);
                
                // Update cache
                cache.put(cleanSymbol, new CachedQuote(price, name, change, changePercent));
                success = true;
            } else {
                System.out.println("Alpha Vantage quote warning (using fallback): " + response);
            }
        } catch (Exception e) {
            System.err.println("Error calling Alpha Vantage Quote API: " + e.getMessage());
        }

        // 3. Fallbacks if API fails or rate limited
        if (success) {
            quoteData.put("price", price);
            quoteData.put("companyName", name);
            quoteData.put("dailyChange", change);
            quoteData.put("changePercent", changePercent);
            return quoteData;
        }

        // If expired cache exists, return it as fallback
        if (cached != null) {
            quoteData.put("price", cached.price);
            quoteData.put("companyName", cached.companyName);
            quoteData.put("dailyChange", cached.dailyChange);
            quoteData.put("changePercent", cached.changePercent);
            return quoteData;
        }

        // Generate mock quote with small random variation
        BigDecimal basePrice = mockPrices.getOrDefault(cleanSymbol, new BigDecimal("150.00"));
        double randomFactor = 0.95 + (Math.random() * 0.1); // +/- 5%
        BigDecimal finalPrice = basePrice.multiply(new BigDecimal(randomFactor)).setScale(2, RoundingMode.HALF_UP);
        
        // Mock daily change and percent
        double changeVal = (Math.random() - 0.5) * 10.0; // +/- 5.00
        BigDecimal mockChange = BigDecimal.valueOf(changeVal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mockPercent = mockChange.divide(basePrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        quoteData.put("price", finalPrice);
        quoteData.put("companyName", name);
        quoteData.put("dailyChange", mockChange);
        quoteData.put("changePercent", mockPercent);

        return quoteData;
    }

    public List<Map<String, String>> searchStocks(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String cleanQuery = query.trim();

        // 1. Query Alpha Vantage symbol search
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + cleanQuery + "&apikey=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode bestMatches = root.path("bestMatches");

            if (bestMatches != null && bestMatches.isArray() && bestMatches.size() > 0) {
                for (JsonNode match : bestMatches) {
                    String symbol = match.path("1. symbol").asText();
                    String name = match.path("2. name").asText();

                    Map<String, String> item = new HashMap<>();
                    item.put("symbol", symbol);
                    item.put("name", name);
                    results.add(item);

                    // Update mockNames cache so we can associate company names with symbols later
                    mockCompanyNames.put(symbol.toUpperCase(), name);
                }
            } else {
                System.out.println("Alpha Vantage search warning (using local fallback): " + response);
            }
        } catch (Exception e) {
            System.err.println("Error calling Alpha Vantage Search API: " + e.getMessage());
        }

        // 2. Local fallback if API is rate limited or returns no results
        if (results.isEmpty()) {
            for (Map.Entry<String, String> entry : mockCompanyNames.entrySet()) {
                if (entry.getKey().toLowerCase().contains(cleanQuery.toLowerCase()) ||
                    entry.getValue().toLowerCase().contains(cleanQuery.toLowerCase())) {
                    Map<String, String> item = new HashMap<>();
                    item.put("symbol", entry.getKey());
                    item.put("name", entry.getValue());
                    results.add(item);
                }
            }
        }

        return results;
    }

    public Map<String, Object> getStockPriceAndName(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase();
        Map<String, Object> quote = getStockQuote(cleanSymbol);

        Map<String, Object> data = new HashMap<>();
        data.put("symbol", cleanSymbol);
        data.put("price", quote.get("price"));
        data.put("companyName", quote.get("companyName"));
        return data;
    }
}
