package com.virtualstock.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualstock.backend.dto.MarketQuoteDto;
import com.virtualstock.backend.exception.MarketDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketQuoteService {

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    @Value("${finnhub.api.key:}")
    private String finnhubApiKey;

    @Value("${alphavantage.api.key:demo}")
    private String alphaVantageApiKey;

    @Autowired
    private MarketSearchService marketSearchService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public BigDecimal getCurrentPrice(String symbol, BigDecimal fallbackPrice) {
        try {
            MarketQuoteDto quote = getQuote(symbol);
            if (quote != null && quote.getCurrentPrice() != null) {
                return quote.getCurrentPrice();
            }
        } catch (Exception e) {
            System.err.println("Error getting current price for " + symbol + ": " + e.getMessage());
        }
        return fallbackPrice;
    }

    private static class CachedQuote {
        final MarketQuoteDto quote;
        final long timestamp;

        CachedQuote(MarketQuoteDto quote) {
            this.quote = quote;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 120000; // 120 seconds (2 minutes)
        }
    }

    public MarketQuoteDto getQuote(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String cleanSymbol = symbol.trim().toUpperCase();
        System.out.println("[MarketQuoteService] getQuote called for: " + cleanSymbol
                + " | TwelveData key valid: " + isKeyValid(twelveDataApiKey)
                + " | Finnhub key valid: " + isKeyValid(finnhubApiKey)
                + " | AlphaVantage key valid: " + isKeyValid(alphaVantageApiKey));

        // 1. Check cache (lock-free read)
        CachedQuote cached = cache.get(cleanSymbol);
        if (cached != null && !cached.isExpired()) {
            System.out.println("[MarketQuoteService] Returning cached quote for " + cleanSymbol);
            return cached.quote;
        }

        // Deduplicate concurrent requests for the same symbol
        synchronized (cleanSymbol.intern()) {
            // Recheck cache inside lock
            cached = cache.get(cleanSymbol);
            if (cached != null && !cached.isExpired()) {
                System.out.println("[MarketQuoteService] Returning cached quote for " + cleanSymbol + " (inside lock)");
                return cached.quote;
            }

            // 2. Resolve default exchange/currency for formatting if available
            String[] mockInfo = marketSearchService.getMockDetails(cleanSymbol);
            String defaultExchange = mockInfo != null ? mockInfo[1] : null;
            String defaultCurrency = mockInfo != null ? mockInfo[2] : null;
            System.out.println("[MarketQuoteService] Resolved defaultExchange=" + defaultExchange + " defaultCurrency=" + defaultCurrency + " for " + cleanSymbol);

            MarketQuoteDto quote = null;
            Exception lastException = null;

            // Try Twelve Data
            if (isKeyValid(twelveDataApiKey)) {
                System.out.println("[MarketQuoteService] Trying Twelve Data for " + cleanSymbol);
                try {
                    quote = fetchTwelveData(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("[MarketQuoteService] Twelve Data quote failed for " + cleanSymbol + ": " + e.getMessage());
                }
            } else {
                System.err.println("[MarketQuoteService] Twelve Data API key is INVALID or EMPTY. Check application.properties!");
            }

            // Try Finnhub
            if (quote == null && isKeyValid(finnhubApiKey)) {
                try {
                    quote = fetchFinnhub(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Finnhub quote failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            // Try Alpha Vantage
            if (quote == null && isKeyValid(alphaVantageApiKey)) {
                try {
                    quote = fetchAlphaVantage(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Alpha Vantage quote failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            // 3. Fallback logic
            if (quote != null) {
                cache.put(cleanSymbol, new CachedQuote(quote));
                return quote;
            }

            // If rate limited or failed, return expired cached quote if we have one
            if (cached != null) {
                System.out.println("Returning expired cached quote for " + cleanSymbol + " due to API failure");
                return cached.quote;
            }

            // Final fallback: generate a mock quote for the symbol
            System.out.println("Generating fallback mock quote for " + cleanSymbol + " (Reason: " + (lastException != null ? lastException.getMessage() : "No active API keys") + ")");
            MarketQuoteDto mockQuote = generateMockQuote(cleanSymbol, defaultExchange, defaultCurrency);
            cache.put(cleanSymbol, new CachedQuote(mockQuote));
            return mockQuote;
        }
    }

    private boolean isKeyValid(String key) {
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("YOUR_KEY");
    }

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty() || str.equalsIgnoreCase("N/A") || str.equalsIgnoreCase("null")) {
            return false;
        }
        try {
            new BigDecimal(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private MarketQuoteDto fetchTwelveData(String symbol, String exchange) throws Exception {
        String url = "https://api.twelvedata.com/quote?symbol=" + symbol;
        // Only append exchange if it's not a common US exchange to avoid mismatches
        if (exchange != null && !exchange.isEmpty() && !exchange.equalsIgnoreCase("NASDAQ") && !exchange.equalsIgnoreCase("NYSE")) {
            url += "&exchange=" + exchange;
        }
        url += "&apikey=" + twelveDataApiKey;

        System.out.println("[MarketQuoteService] Calling Twelve Data URL: " + url);
        String response = restTemplate.getForObject(url, String.class);
        System.out.println("[MarketQuoteService] Twelve Data raw response for " + symbol + ": " + response);

        JsonNode root = mapper.readTree(response);

        if (root.has("status") && root.get("status").asText().equals("error")) {
            String msg = root.has("message") ? root.get("message").asText() : "Unknown error";
            throw new MarketDataException("Twelve Data error: " + msg);
        }

        // Twelve Data returns "N/A" for close when market is closed or symbol not found.
        // Try close first, then previous_close, then open as fallback for current price.
        String closeTxt = root.has("close") && !root.get("close").isNull() ? root.get("close").asText() : null;
        String prevCloseTxt = root.has("previous_close") && !root.get("previous_close").isNull() ? root.get("previous_close").asText() : null;
        String openTxt = root.has("open") && !root.get("open").isNull() ? root.get("open").asText() : null;

        String priceSource;
        if (isNumeric(closeTxt)) {
            priceSource = closeTxt;
        } else if (isNumeric(prevCloseTxt)) {
            System.out.println("[MarketQuoteService] close is N/A for " + symbol + ", using previous_close=" + prevCloseTxt);
            priceSource = prevCloseTxt;
        } else if (isNumeric(openTxt)) {
            System.out.println("[MarketQuoteService] close is N/A for " + symbol + ", using open=" + openTxt);
            priceSource = openTxt;
        } else {
            throw new MarketDataException("Twelve Data invalid response: no usable price field for " + symbol + " (close=" + closeTxt + ", prev=" + prevCloseTxt + ", open=" + openTxt + ")");
        }

        String name = root.has("name") && !root.get("name").isNull() ? root.get("name").asText() : symbol;
        String resolvedExchange = root.has("exchange") && !root.get("exchange").isNull() ? root.get("exchange").asText() : (exchange != null ? exchange : "US");
        String currency = root.has("currency") && !root.get("currency").isNull() ? root.get("currency").asText() : (resolvedExchange.equalsIgnoreCase("NSE") ? "INR" : "USD");

        BigDecimal currentPrice = new BigDecimal(priceSource).setScale(2, RoundingMode.HALF_UP);
        BigDecimal open = isNumeric(openTxt) ? new BigDecimal(openTxt).setScale(2, RoundingMode.HALF_UP) : currentPrice;
        String highTxt = root.has("high") && !root.get("high").isNull() ? root.get("high").asText() : null;
        String lowTxt = root.has("low") && !root.get("low").isNull() ? root.get("low").asText() : null;
        BigDecimal high = isNumeric(highTxt) ? new BigDecimal(highTxt).setScale(2, RoundingMode.HALF_UP) : currentPrice;
        BigDecimal low = isNumeric(lowTxt) ? new BigDecimal(lowTxt).setScale(2, RoundingMode.HALF_UP) : currentPrice;
        BigDecimal previousClose = isNumeric(prevCloseTxt) ? new BigDecimal(prevCloseTxt).setScale(2, RoundingMode.HALF_UP) : currentPrice;

        String changeTxt = root.has("change") && !root.get("change").isNull() ? root.get("change").asText() : null;
        String pctTxt = root.has("percent_change") && !root.get("percent_change").isNull() ? root.get("percent_change").asText() : null;
        BigDecimal change = isNumeric(changeTxt) ? new BigDecimal(changeTxt).setScale(2, RoundingMode.HALF_UP) : currentPrice.subtract(previousClose).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = isNumeric(pctTxt) ? new BigDecimal(pctTxt).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String volumeTxt = root.has("volume") && !root.get("volume").isNull() ? root.get("volume").asText() : null;
        Long volume = isNumeric(volumeTxt) ? Long.parseLong(volumeTxt.trim()) : 0L;
        Long timestamp = root.has("timestamp") && !root.get("timestamp").isNull() ? root.get("timestamp").asLong() * 1000 : System.currentTimeMillis();

        System.out.println("[MarketQuoteService] Twelve Data SUCCESS for " + symbol + ": price=" + currentPrice + " exchange=" + resolvedExchange + " currency=" + currency);
        return new MarketQuoteDto(symbol, name, resolvedExchange, currency, currentPrice, open, high, low, previousClose, change, changePercent, volume, timestamp);
    }

    private MarketQuoteDto fetchFinnhub(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NS";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BO";
        }

        String url = "https://finnhub.io/api/v1/quote?symbol=" + formattedSymbol + "&token=" + finnhubApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (!root.has("c") || root.get("c").asDouble() == 0.0) {
            throw new MarketDataException("Finnhub quote returned zero/missing price");
        }

        BigDecimal currentPrice = new BigDecimal(root.get("c").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal open = new BigDecimal(root.get("o").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal high = new BigDecimal(root.get("h").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal low = new BigDecimal(root.get("l").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousClose = new BigDecimal(root.get("pc").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = new BigDecimal(root.get("d").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = new BigDecimal(root.get("dp").asText()).setScale(2, RoundingMode.HALF_UP);
        Long timestamp = root.get("t").asLong() * 1000;

        String resolvedExchange = exchange != null ? exchange : "NASDAQ";
        String currency = "INR".equalsIgnoreCase(currencyFromExchange(resolvedExchange)) ? "INR" : "USD";
        String name = fetchFinnhubName(formattedSymbol, symbol);

        return new MarketQuoteDto(symbol, name, resolvedExchange, currency, currentPrice, open, high, low, previousClose, change, changePercent, 0L, timestamp);
    }

    private String fetchFinnhubName(String formattedSymbol, String originalSymbol) {
        try {
            String url = "https://finnhub.io/api/v1/stock/profile2?symbol=" + formattedSymbol + "&token=" + finnhubApiKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);
            if (root.has("name")) {
                return root.get("name").asText();
            }
        } catch (Exception e) {
            System.err.println("Finnhub name profile lookup failed: " + e.getMessage());
        }
        String[] mockInfo = marketSearchService.getMockDetails(originalSymbol);
        return mockInfo != null ? mockInfo[0] : originalSymbol;
    }

    private MarketQuoteDto fetchAlphaVantage(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NSE";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BSE";
        }

        String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + formattedSymbol + "&apikey=" + alphaVantageApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (root.has("Note") || root.has("Information")) {
            throw new MarketDataException("Alpha Vantage rate limit exceeded");
        }

        JsonNode quote = root.path("Global Quote");
        if (quote == null || quote.isMissingNode() || !quote.has("05. price")) {
            throw new MarketDataException("Alpha Vantage invalid quote response");
        }

        BigDecimal currentPrice = new BigDecimal(quote.get("05. price").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal open = new BigDecimal(quote.get("02. open").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal high = new BigDecimal(quote.get("03. high").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal low = new BigDecimal(quote.get("04. low").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousClose = new BigDecimal(quote.get("08. previous close").asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = new BigDecimal(quote.get("09. change").asText()).setScale(2, RoundingMode.HALF_UP);

        String pctStr = quote.get("10. change percent").asText().replace("%", "");
        BigDecimal changePercent = new BigDecimal(pctStr).setScale(2, RoundingMode.HALF_UP);

        Long volume = quote.has("06. volume") ? Long.parseLong(quote.get("06. volume").asText()) : 0L;
        String resolvedExchange = exchange != null ? exchange : "NASDAQ";
        String currency = "INR".equalsIgnoreCase(currencyFromExchange(resolvedExchange)) ? "INR" : "USD";
        
        String[] mockInfo = marketSearchService.getMockDetails(symbol);
        String name = mockInfo != null ? mockInfo[0] : symbol;

        return new MarketQuoteDto(symbol, name, resolvedExchange, currency, currentPrice, open, high, low, previousClose, change, changePercent, volume, System.currentTimeMillis());
    }

    private String currencyFromExchange(String exchange) {
        if ("NSE".equalsIgnoreCase(exchange) || "BSE".equalsIgnoreCase(exchange)) {
            return "INR";
        }
        return "USD";
    }

    private MarketQuoteDto generateMockQuote(String symbol, String exchange, String currency) {
        String resolvedExchange = exchange != null ? exchange : "NASDAQ";
        String resolvedCurrency = currency != null ? currency : currencyFromExchange(resolvedExchange);

        // Fetch mock details
        String[] mockInfo = marketSearchService.getMockDetails(symbol);
        String name = mockInfo != null ? mockInfo[0] : symbol;
        BigDecimal basePrice = "INR".equalsIgnoreCase(resolvedCurrency) ? new BigDecimal("2000.00") : new BigDecimal("150.00");

        if (mockInfo == null) {
            // Check if symbol exists in mock list to load correct values
            if ("AAPL".equalsIgnoreCase(symbol)) { basePrice = new BigDecimal("175.50"); name = "Apple Inc."; }
            else if ("MSFT".equalsIgnoreCase(symbol)) { basePrice = new BigDecimal("380.10"); name = "Microsoft Corporation"; }
            else if ("RELIANCE".equalsIgnoreCase(symbol)) { basePrice = new BigDecimal("2450.00"); name = "Reliance Industries"; }
            else if ("TCS".equalsIgnoreCase(symbol)) { basePrice = new BigDecimal("3400.00"); name = "Tata Consultancy Services"; }
        } else {
            // Use mock prices from service if they exist
            if ("AAPL".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("175.50");
            else if ("MSFT".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("380.10");
            else if ("GOOGL".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("150.20");
            else if ("AMZN".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("145.40");
            else if ("TSLA".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("220.50");
            else if ("RELIANCE".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("2450.00");
            else if ("TCS".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("3400.00");
            else if ("INFY".equalsIgnoreCase(symbol)) basePrice = new BigDecimal("1500.00");
        }

        double randomFactor = 0.98 + (Math.random() * 0.04);
        BigDecimal currentPrice = basePrice.multiply(BigDecimal.valueOf(randomFactor)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal open = basePrice.multiply(BigDecimal.valueOf(0.99 + (Math.random() * 0.02))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal high = currentPrice.max(open).multiply(BigDecimal.valueOf(1.0 + (Math.random() * 0.01))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal low = currentPrice.min(open).multiply(BigDecimal.valueOf(0.99 - (Math.random() * 0.01))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = currentPrice.subtract(basePrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = change.divide(basePrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        Long volume = (long) (Math.random() * 5000000) + 100000L;

        return new MarketQuoteDto(symbol, name, resolvedExchange, resolvedCurrency, currentPrice, open, high, low, basePrice, change, changePercent, volume, System.currentTimeMillis());
    }
}
