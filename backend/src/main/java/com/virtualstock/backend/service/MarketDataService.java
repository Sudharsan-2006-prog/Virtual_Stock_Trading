package com.virtualstock.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualstock.backend.dto.CompanyProfileDto;
import com.virtualstock.backend.dto.MarketHistoryDto;
import com.virtualstock.backend.dto.MarketQuoteDto;
import com.virtualstock.backend.exception.MarketDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MarketDataService {

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    @Value("${finnhub.api.key:}")
    private String finnhubApiKey;

    @Value("${alphavantage.api.key:demo}")
    private String alphaVantageApiKey;

    @Autowired
    private MarketSearchService marketSearchService;

    @Autowired
    private MarketQuoteService marketQuoteService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public BigDecimal getCurrentPrice(String symbol, BigDecimal fallbackPrice) {
        return marketQuoteService.getCurrentPrice(symbol, fallbackPrice);
    }

    public MarketQuoteDto getQuote(String symbol) {
        return marketQuoteService.getQuote(symbol);
    }

    public List<Map<String, String>> searchStocks(String query) {
        return marketSearchService.searchStocks(query);
    }

    public MarketHistoryDto getHistory(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String cleanSymbol = symbol.trim().toUpperCase();
        String[] mockInfo = marketSearchService.getMockDetails(cleanSymbol);
        String defaultExchange = mockInfo != null ? mockInfo[1] : null;

        MarketHistoryDto history = null;

        // Try Twelve Data
        if (isKeyValid(twelveDataApiKey)) {
            try {
                history = fetchHistoryTwelveData(cleanSymbol, defaultExchange);
            } catch (Exception e) {
                System.err.println("Twelve Data history failed: " + e.getMessage());
            }
        }

        // Try Finnhub
        if (history == null && isKeyValid(finnhubApiKey)) {
            try {
                history = fetchHistoryFinnhub(cleanSymbol, defaultExchange);
            } catch (Exception e) {
                System.err.println("Finnhub history failed: " + e.getMessage());
            }
        }

        // Try Alpha Vantage
        if (history == null && isKeyValid(alphaVantageApiKey)) {
            try {
                history = fetchHistoryAlphaVantage(cleanSymbol, defaultExchange);
            } catch (Exception e) {
                System.err.println("Alpha Vantage history failed: " + e.getMessage());
            }
        }

        if (history != null) {
            return history;
        }

        // Fallback: Generate mock history
        System.out.println("Generating fallback mock history for " + cleanSymbol);
        return generateMockHistory(cleanSymbol);
    }

    public CompanyProfileDto getCompanyProfile(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String cleanSymbol = symbol.trim().toUpperCase();
        String[] mockInfo = marketSearchService.getMockDetails(cleanSymbol);
        String defaultExchange = mockInfo != null ? mockInfo[1] : null;
        String defaultCurrency = mockInfo != null ? mockInfo[2] : null;

        CompanyProfileDto profile = null;

        // Try Twelve Data Profile
        if (isKeyValid(twelveDataApiKey)) {
            try {
                profile = fetchProfileTwelveData(cleanSymbol);
            } catch (Exception e) {
                System.err.println("Twelve Data profile failed for " + cleanSymbol + ": " + e.getMessage());
            }
        }

        // Try Finnhub Profile
        if (profile == null && isKeyValid(finnhubApiKey)) {
            try {
                profile = fetchProfileFinnhub(cleanSymbol, defaultExchange);
            } catch (Exception e) {
                System.err.println("Finnhub profile failed for " + cleanSymbol + ": " + e.getMessage());
            }
        }

        // Try Alpha Vantage Profile
        if (profile == null && isKeyValid(alphaVantageApiKey)) {
            try {
                profile = fetchProfileAlphaVantage(cleanSymbol, defaultExchange);
            } catch (Exception e) {
                System.err.println("Alpha Vantage profile failed for " + cleanSymbol + ": " + e.getMessage());
            }
        }

        if (profile != null) {
            return profile;
        }

        // Fallback: generate mock profile
        System.out.println("Generating fallback mock profile for " + cleanSymbol);
        return generateMockProfile(cleanSymbol, defaultExchange, defaultCurrency);
    }

    private boolean isKeyValid(String key) {
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("YOUR_KEY");
    }

    // History API Calls
    private MarketHistoryDto fetchHistoryTwelveData(String symbol, String exchange) throws Exception {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol;
        if (exchange != null && !exchange.isEmpty()) {
            url += "&exchange=" + exchange;
        }
        url += "&interval=1day&outputsize=30&apikey=" + twelveDataApiKey;

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (root.has("status") && root.get("status").asText().equals("error")) {
            throw new MarketDataException("Twelve Data history error: " + root.get("message").asText());
        }

        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        if (root.has("values") && root.get("values").isArray()) {
            for (JsonNode item : root.get("values")) {
                String date = item.get("datetime").asText();
                BigDecimal open = new BigDecimal(item.get("open").asText()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal high = new BigDecimal(item.get("high").asText()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal low = new BigDecimal(item.get("low").asText()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal close = new BigDecimal(item.get("close").asText()).setScale(2, RoundingMode.HALF_UP);
                Long volume = item.has("volume") ? Long.parseLong(item.get("volume").asText()) : 0L;
                points.add(new MarketHistoryDto.HistoryPoint(date, open, high, low, close, volume));
            }
        }

        return new MarketHistoryDto(symbol, points);
    }

    private MarketHistoryDto fetchHistoryFinnhub(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NS";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BO";
        }

        long to = System.currentTimeMillis() / 1000;
        long from = to - (30L * 24 * 3600); // 30 days
        String url = "https://finnhub.io/api/v1/stock/candle?symbol=" + formattedSymbol + "&resolution=D&from=" + from + "&to=" + to + "&token=" + finnhubApiKey;

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (!root.has("s") || !root.get("s").asText().equals("ok")) {
            throw new MarketDataException("Finnhub history error");
        }

        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        JsonNode o = root.get("o");
        JsonNode h = root.get("h");
        JsonNode l = root.get("l");
        JsonNode c = root.get("c");
        JsonNode v = root.get("v");
        JsonNode t = root.get("t");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < c.size(); i++) {
            String date = sdf.format(new Date(t.get(i).asLong() * 1000));
            BigDecimal openPrice = new BigDecimal(o.get(i).asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal highPrice = new BigDecimal(h.get(i).asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lowPrice = new BigDecimal(l.get(i).asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closePrice = new BigDecimal(c.get(i).asText()).setScale(2, RoundingMode.HALF_UP);
            Long volume = v.get(i).asLong();
            points.add(new MarketHistoryDto.HistoryPoint(date, openPrice, highPrice, lowPrice, closePrice, volume));
        }

        return new MarketHistoryDto(symbol, points);
    }

    private MarketHistoryDto fetchHistoryAlphaVantage(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NSE";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BSE";
        }

        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + formattedSymbol + "&outputsize=compact&apikey=" + alphaVantageApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (root.has("Note") || root.has("Information")) {
            throw new MarketDataException("Alpha Vantage rate limit exceeded");
        }

        JsonNode timeSeries = root.path("Time Series (Daily)");
        if (timeSeries == null || timeSeries.isMissingNode()) {
            throw new MarketDataException("Alpha Vantage daily series missing");
        }

        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
        int count = 0;
        while (fields.hasNext() && count < 30) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String date = entry.getKey();
            JsonNode node = entry.getValue();

            BigDecimal open = new BigDecimal(node.get("1. open").asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal high = new BigDecimal(node.get("2. high").asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal low = new BigDecimal(node.get("3. low").asText()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal close = new BigDecimal(node.get("4. close").asText()).setScale(2, RoundingMode.HALF_UP);
            Long volume = Long.parseLong(node.get("5. volume").asText());

            points.add(new MarketHistoryDto.HistoryPoint(date, open, high, low, close, volume));
            count++;
        }

        return new MarketHistoryDto(symbol, points);
    }

    // Profile API Calls
    private CompanyProfileDto fetchProfileTwelveData(String symbol) throws Exception {
        String url = "https://api.twelvedata.com/profile?symbol=" + symbol + "&apikey=" + twelveDataApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (root.has("status") && root.get("status").asText().equals("error")) {
            throw new MarketDataException("Twelve Data profile error: " + root.get("message").asText());
        }

        String name = root.path("name").asText(symbol);
        String exchange = root.path("exchange").asText("US");
        String currency = "NSE".equalsIgnoreCase(exchange) ? "INR" : "USD";
        String country = root.path("country").asText("United States");
        String sector = root.path("sector").asText("Technology");
        String industry = root.path("industry").asText("Software");
        String desc = root.path("description").asText("A leading multinational corporation.");
        String web = root.path("website").asText("http://www.google.com");

        return new CompanyProfileDto(symbol, name, exchange, currency, country, industry, sector, desc, web);
    }

    private CompanyProfileDto fetchProfileFinnhub(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NS";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BO";
        }

        String url = "https://finnhub.io/api/v1/stock/profile2?symbol=" + formattedSymbol + "&token=" + finnhubApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (!root.has("name")) {
            throw new MarketDataException("Finnhub profile lookup failed");
        }

        String name = root.path("name").asText(symbol);
        String resolvedExchange = exchange != null ? exchange : root.path("exchange").asText("NASDAQ");
        String currency = root.path("currency").asText("NSE".equalsIgnoreCase(resolvedExchange) ? "INR" : "USD");
        String country = root.path("country").asText("NSE".equalsIgnoreCase(resolvedExchange) ? "India" : "United States");
        String industry = root.path("finnhubIndustry").asText("Technology");
        String web = root.path("weburl").asText("");

        return new CompanyProfileDto(symbol, name, resolvedExchange, currency, country, industry, "Generic Sector", "Detailed description not provided.", web);
    }

    private CompanyProfileDto fetchProfileAlphaVantage(String symbol, String exchange) throws Exception {
        String formattedSymbol = symbol;
        if ("NSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".NSE";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            formattedSymbol += ".BSE";
        }

        String url = "https://www.alphavantage.co/query?function=OVERVIEW&symbol=" + formattedSymbol + "&apikey=" + alphaVantageApiKey;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (root.has("Note") || root.has("Information")) {
            throw new MarketDataException("Alpha Vantage rate limit exceeded");
        }

        if (!root.has("Name")) {
            throw new MarketDataException("Alpha Vantage profile lookup failed");
        }

        String name = root.path("Name").asText();
        String resolvedExchange = exchange != null ? exchange : root.path("Exchange").asText();
        String currency = root.path("Currency").asText("USD");
        String country = root.path("Country").asText("United States");
        String sector = root.path("Sector").asText("Technology");
        String industry = root.path("Industry").asText("Software");
        String desc = root.path("Description").asText("");
        String web = root.path("OfficialSite").asText("");

        return new CompanyProfileDto(symbol, name, resolvedExchange, currency, country, industry, sector, desc, web);
    }

    // Fallback Generators
    private MarketHistoryDto generateMockHistory(String symbol) {
        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        BigDecimal currentPrice = new BigDecimal("100.00");
        
        // Match specific prices
        if ("AAPL".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("175.50");
        else if ("MSFT".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("380.10");
        else if ("RELIANCE".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("2450.00");
        else if ("TCS".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("3400.00");

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 30; i++) {
            String date = sdf.format(cal.getTime());
            BigDecimal open = currentPrice.multiply(BigDecimal.valueOf(0.99 + (Math.random() * 0.02))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal close = currentPrice.multiply(BigDecimal.valueOf(0.99 + (Math.random() * 0.02))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.0 + (Math.random() * 0.01))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(0.99)).setScale(2, RoundingMode.HALF_UP);
            Long volume = (long) (Math.random() * 2000000) + 50000L;

            points.add(new MarketHistoryDto.HistoryPoint(date, open, high, low, close, volume));

            currentPrice = close;
            cal.add(Calendar.DATE, -1);
        }

        return new MarketHistoryDto(symbol, points);
    }

    private CompanyProfileDto generateMockProfile(String symbol, String exchange, String currency) {
        String[] mockInfo = marketSearchService.getMockDetails(symbol);
        String name = mockInfo != null ? mockInfo[0] : symbol;
        String resolvedExchange = exchange != null ? exchange : (mockInfo != null ? mockInfo[1] : "NASDAQ");
        String resolvedCurrency = currency != null ? currency : (mockInfo != null ? mockInfo[2] : "USD");
        String country = "INR".equalsIgnoreCase(resolvedCurrency) ? "India" : "United States";
        String industry = "INR".equalsIgnoreCase(resolvedCurrency) ? "Conglomerate" : "Consumer Electronics / Software";
        String sector = "INR".equalsIgnoreCase(resolvedCurrency) ? "Diverse" : "Technology";
        String website = "INR".equalsIgnoreCase(resolvedCurrency) ? "https://www.ril.com" : "https://www.apple.com";
        String desc = "Mock Profile: " + name + " is a leading global listing traded on exchange " + resolvedExchange + " in currency " + resolvedCurrency + ".";

        return new CompanyProfileDto(symbol, name, resolvedExchange, resolvedCurrency, country, industry, sector, desc, website);
    }
}
