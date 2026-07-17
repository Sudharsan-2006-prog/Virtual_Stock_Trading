package com.virtualstock.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MarketSearchService {

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    @Value("${finnhub.api.key:}")
    private String finnhubApiKey;

    @Value("${alphavantage.api.key:demo}")
    private String alphaVantageApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String[]> mockCompanyDetails = new HashMap<>();

    public MarketSearchService() {
        // [Name, Exchange, Currency, Country]
        mockCompanyDetails.put("RELIANCE", new String[]{"Reliance Industries", "NSE", "INR", "India"});
        mockCompanyDetails.put("TCS", new String[]{"Tata Consultancy Services", "NSE", "INR", "India"});
        mockCompanyDetails.put("INFY", new String[]{"Infosys Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("HDFCBANK", new String[]{"HDFC Bank Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("ICICIBANK", new String[]{"ICICI Bank Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("SBIN", new String[]{"State Bank of India", "NSE", "INR", "India"});
        mockCompanyDetails.put("LT", new String[]{"Larsen & Toubro Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("ITC", new String[]{"ITC Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("BHARTIARTL", new String[]{"Bharti Airtel Limited", "NSE", "INR", "India"});
        mockCompanyDetails.put("AXISBANK", new String[]{"Axis Bank Limited", "NSE", "INR", "India"});

        mockCompanyDetails.put("AAPL", new String[]{"Apple Inc.", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("MSFT", new String[]{"Microsoft Corporation", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("GOOGL", new String[]{"Alphabet Inc.", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("AMZN", new String[]{"Amazon.com, Inc.", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("NVDA", new String[]{"NVIDIA Corporation", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("META", new String[]{"Meta Platforms, Inc.", "NASDAQ", "USD", "United States"});
        mockCompanyDetails.put("TSLA", new String[]{"Tesla, Inc.", "NASDAQ", "USD", "United States"});
    }

    public List<Map<String, String>> searchStocks(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String cleanQuery = query.trim().toLowerCase();

        // 1. Try Twelve Data
        if (isKeyValid(twelveDataApiKey)) {
            try {
                results = searchTwelveData(cleanQuery);
            } catch (Exception e) {
                System.err.println("Twelve Data search failed: " + e.getMessage());
            }
        }

        // 2. Try Finnhub
        if (results.isEmpty() && isKeyValid(finnhubApiKey)) {
            try {
                results = searchFinnhub(cleanQuery);
            } catch (Exception e) {
                System.err.println("Finnhub search failed: " + e.getMessage());
            }
        }

        // 3. Try Alpha Vantage
        if (results.isEmpty() && isKeyValid(alphaVantageApiKey)) {
            try {
                results = searchAlphaVantage(cleanQuery);
            } catch (Exception e) {
                System.err.println("Alpha Vantage search failed: " + e.getMessage());
            }
        }

        // 4. Fallback search using local mock details
        if (results.isEmpty()) {
            for (Map.Entry<String, String[]> entry : mockCompanyDetails.entrySet()) {
                String symbol = entry.getKey();
                String[] details = entry.getValue();
                if (symbol.toLowerCase().contains(cleanQuery) || details[0].toLowerCase().contains(cleanQuery)) {
                    Map<String, String> item = new HashMap<>();
                    item.put("symbol", symbol);
                    item.put("name", details[0]);
                    item.put("exchange", details[1]);
                    item.put("currency", details[2]);
                    item.put("country", details[3]);
                    results.add(item);
                }
            }
        }

        return results;
    }

    private boolean isKeyValid(String key) {
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("YOUR_KEY");
    }

    private List<Map<String, String>> searchTwelveData(String query) throws Exception {
        String url = "https://api.twelvedata.com/symbol_search?symbol=" + query + "&apikey=" + twelveDataApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);
        List<Map<String, String>> results = new ArrayList<>();

        if (root.has("data") && root.get("data").isArray()) {
            for (JsonNode item : root.get("data")) {
                Map<String, String> map = new HashMap<>();
                String symbol = item.path("symbol").asText();
                String name = item.path("instrument_name").asText();
                String exchange = item.path("exchange").asText();
                String currency = item.path("currency").asText();
                String country = item.path("country").asText();

                map.put("symbol", symbol);
                map.put("name", name);
                map.put("exchange", exchange);
                map.put("currency", currency);
                map.put("country", country);
                results.add(map);
            }
        }
        return results;
    }

    private List<Map<String, String>> searchFinnhub(String query) throws Exception {
        String url = "https://finnhub.io/api/v1/search?q=" + query + "&token=" + finnhubApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);
        List<Map<String, String>> results = new ArrayList<>();

        if (root.has("result") && root.get("result").isArray()) {
            for (JsonNode item : root.get("result")) {
                Map<String, String> map = new HashMap<>();
                String symbol = item.path("symbol").asText();
                String name = item.path("description").asText();
                
                // Smart parser for Finnhub listings
                String exchange = "US";
                String currency = "USD";
                String country = "United States";

                if (symbol.endsWith(".NS")) {
                    exchange = "NSE";
                    currency = "INR";
                    country = "India";
                    symbol = symbol.substring(0, symbol.indexOf(".NS")); // remove suffix for clean display
                } else if (symbol.endsWith(".BO")) {
                    exchange = "BSE";
                    currency = "INR";
                    country = "India";
                    symbol = symbol.substring(0, symbol.indexOf(".BO"));
                } else {
                    exchange = item.path("type").asText().contains("Common Stock") ? "NASDAQ" : "NYSE";
                }

                map.put("symbol", symbol);
                map.put("name", name);
                map.put("exchange", exchange);
                map.put("currency", currency);
                map.put("country", country);
                results.add(map);
            }
        }
        return results;
    }

    private List<Map<String, String>> searchAlphaVantage(String query) throws Exception {
        String url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + query + "&apikey=" + alphaVantageApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);
        List<Map<String, String>> results = new ArrayList<>();

        JsonNode bestMatches = root.path("bestMatches");
        if (bestMatches != null && bestMatches.isArray()) {
            for (JsonNode match : bestMatches) {
                Map<String, String> map = new HashMap<>();
                String symbol = match.path("1. symbol").asText();
                String name = match.path("2. name").asText();
                String region = match.path("4. region").asText();
                String currency = match.path("8. currency").asText();

                String exchange = "US";
                String country = "United States";

                if (region.toLowerCase().contains("india") || currency.equalsIgnoreCase("INR")) {
                    exchange = region.toLowerCase().contains("bombay") ? "BSE" : "NSE";
                    country = "India";
                    if (symbol.contains(".")) {
                        symbol = symbol.substring(0, symbol.indexOf("."));
                    }
                } else {
                    exchange = "NASDAQ";
                }

                map.put("symbol", symbol);
                map.put("name", name);
                map.put("exchange", exchange);
                map.put("currency", currency);
                map.put("country", country);
                results.add(map);
            }
        }
        return results;
    }

    public String[] getMockDetails(String symbol) {
        return mockCompanyDetails.get(symbol.toUpperCase());
    }
}
