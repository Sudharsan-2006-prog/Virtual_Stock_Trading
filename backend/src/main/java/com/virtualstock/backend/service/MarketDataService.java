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

    private static class CachedHistory {
        final MarketHistoryDto history;
        final long timestamp;

        CachedHistory(MarketHistoryDto history) {
            this.history = history;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 300000; // 5 minutes
        }
    }

    private static class CachedProfile {
        final CompanyProfileDto profile;
        final long timestamp;

        CachedProfile(CompanyProfileDto profile) {
            this.profile = profile;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 600000; // 10 minutes
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, CachedHistory> historyCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, CachedProfile> profileCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        return getHistory(symbol, "1M");
    }

    public MarketHistoryDto getHistory(String symbol, String range) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String cleanSymbol = symbol.trim().toUpperCase();
        String cacheKey = cleanSymbol + "_" + (range != null ? range.toUpperCase() : "1M");

        // 1. Check cache (lock-free read)
        CachedHistory cached = historyCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.history;
        }

        // Deduplicate concurrent history requests for the same symbol/range
        synchronized (cacheKey.intern()) {
            // Recheck cache inside lock
            cached = historyCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.history;
            }

            String[] mockInfo = marketSearchService.getMockDetails(cleanSymbol);
            String defaultExchange = mockInfo != null ? mockInfo[1] : null;

            MarketHistoryDto history = null;
            Exception lastException = null;

            String effectiveRange = (range != null && !range.trim().isEmpty()) ? range.toUpperCase() : "1M";

            // Try Twelve Data for ALL ranges
            if (isKeyValid(twelveDataApiKey)) {
                System.out.println("[MarketDataService] Trying Twelve Data history for " + cleanSymbol + " range=" + effectiveRange);
                try {
                    history = fetchHistoryTwelveData(cleanSymbol, defaultExchange, effectiveRange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("[MarketDataService] Twelve Data history failed for " + cleanSymbol + " range=" + effectiveRange + ": " + e.getMessage());
                }
            } else {
                System.err.println("[MarketDataService] Twelve Data API key INVALID - skipping for history");
            }

            // Try Finnhub (only for 1M range as it uses unix timestamp ranges)
            if (history == null && isKeyValid(finnhubApiKey) && ("1M".equalsIgnoreCase(effectiveRange) || "1W".equalsIgnoreCase(effectiveRange))) {
                System.out.println("[MarketDataService] Trying Finnhub history for " + cleanSymbol + " range=" + effectiveRange);
                try {
                    history = fetchHistoryFinnhub(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("[MarketDataService] Finnhub history failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            // Try Alpha Vantage for daily data
            if (history == null && isKeyValid(alphaVantageApiKey)) {
                System.out.println("[MarketDataService] Trying Alpha Vantage history for " + cleanSymbol);
                try {
                    history = fetchHistoryAlphaVantage(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("[MarketDataService] Alpha Vantage history failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            if (history != null) {
                System.out.println("[MarketDataService] History SUCCESS for " + cleanSymbol + " range=" + effectiveRange + " with " + history.getHistory().size() + " points");
                historyCache.put(cacheKey, new CachedHistory(history));
                return history;
            }

            // If failed, try to return expired cached history
            if (cached != null) {
                System.out.println("[MarketDataService] Returning expired cached history for " + cleanSymbol + " due to API failure");
                return cached.history;
            }

            // Fallback: Generate mock history
            System.out.println("[MarketDataService] Generating fallback mock history for " + cleanSymbol + " range=" + effectiveRange + " (Reason: " + (lastException != null ? lastException.getMessage() : "No active API keys") + ")");
            MarketHistoryDto mockHistory = generateMockHistory(cleanSymbol, range);
            historyCache.put(cacheKey, new CachedHistory(mockHistory));
            return mockHistory;
        }
    }

    public CompanyProfileDto getCompanyProfile(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String cleanSymbol = symbol.trim().toUpperCase();

        // 1. Check cache (lock-free read)
        CachedProfile cached = profileCache.get(cleanSymbol);
        if (cached != null && !cached.isExpired()) {
            return cached.profile;
        }

        // Deduplicate concurrent profile requests for the same symbol
        synchronized (cleanSymbol.intern()) {
            // Recheck cache inside lock
            cached = profileCache.get(cleanSymbol);
            if (cached != null && !cached.isExpired()) {
                return cached.profile;
            }

            String[] mockInfo = marketSearchService.getMockDetails(cleanSymbol);
            String defaultExchange = mockInfo != null ? mockInfo[1] : null;
            String defaultCurrency = mockInfo != null ? mockInfo[2] : null;

            CompanyProfileDto profile = null;
            Exception lastException = null;

            // Try Twelve Data Profile
            if (isKeyValid(twelveDataApiKey)) {
                try {
                    profile = fetchProfileTwelveData(cleanSymbol);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Twelve Data profile failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            // Try Finnhub Profile
            if (profile == null && isKeyValid(finnhubApiKey)) {
                try {
                    profile = fetchProfileFinnhub(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Finnhub profile failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            // Try Alpha Vantage Profile
            if (profile == null && isKeyValid(alphaVantageApiKey)) {
                try {
                    profile = fetchProfileAlphaVantage(cleanSymbol, defaultExchange);
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Alpha Vantage profile failed for " + cleanSymbol + ": " + e.getMessage());
                }
            }

            if (profile != null) {
                CompanyProfileDto enriched = enrichProfile(profile);
                profileCache.put(cleanSymbol, new CachedProfile(enriched));
                return enriched;
            }

            // If failed, try to return expired cached profile
            if (cached != null) {
                System.out.println("Returning expired cached profile for " + cleanSymbol + " due to API failure");
                return cached.profile;
            }

            // Fallback: generate mock profile
            System.out.println("Generating fallback mock profile for " + cleanSymbol + " (Reason: " + (lastException != null ? lastException.getMessage() : "No active API keys") + ")");
            CompanyProfileDto mockProfile = generateMockProfile(cleanSymbol, defaultExchange, defaultCurrency);
            profileCache.put(cleanSymbol, new CachedProfile(mockProfile));
            return mockProfile;
        }
    }

    private CompanyProfileDto enrichProfile(CompanyProfileDto profile) {
        if (profile == null) return null;
        if (profile.getMarketCap() == null) {
            CompanyProfileDto mock = generateMockProfile(profile.getSymbol(), profile.getExchange(), profile.getCurrency());
            profile.setMarketCap(mock.getMarketCap());
            profile.setFiftyTwoWeekHigh(mock.getFiftyTwoWeekHigh());
            profile.setFiftyTwoWeekLow(mock.getFiftyTwoWeekLow());
            profile.setPeRatio(mock.getPeRatio());
            profile.setDividendYield(mock.getDividendYield());
            profile.setVolume(mock.getVolume());
            if (profile.getSector() == null || profile.getSector().isEmpty() || "Generic Sector".equalsIgnoreCase(profile.getSector())) {
                profile.setSector(mock.getSector());
            }
        }
        return profile;
    }


    private boolean isKeyValid(String key) {
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("YOUR_KEY");
    }

    // History API Calls
    private MarketHistoryDto fetchHistoryTwelveData(String symbol, String exchange, String range) throws Exception {
        // Map frontend range to Twelve Data API interval and outputsize
        String interval;
        int outputsize;
        switch (range.toUpperCase()) {
            case "1D":
                interval = "5min";
                outputsize = 78; // ~78 5-min bars in a trading day
                break;
            case "1W":
                interval = "1h";
                outputsize = 40;
                break;
            case "3M":
                interval = "1day";
                outputsize = 90;
                break;
            case "6M":
                interval = "1day";
                outputsize = 180;
                break;
            case "1Y":
                interval = "1day";
                outputsize = 365;
                break;
            case "1M":
            default:
                interval = "1day";
                outputsize = 30;
                break;
        }

        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol;
        // Only append exchange if non-US to avoid mismatches
        if (exchange != null && !exchange.isEmpty() && !exchange.equalsIgnoreCase("NASDAQ") && !exchange.equalsIgnoreCase("NYSE")) {
            url += "&exchange=" + exchange;
        }
        url += "&interval=" + interval + "&outputsize=" + outputsize + "&apikey=" + twelveDataApiKey;

        System.out.println("[MarketDataService] Calling Twelve Data time_series URL: " + url);
        String response = restTemplate.getForObject(url, String.class);
        System.out.println("[MarketDataService] Twelve Data time_series response for " + symbol + ": " + (response != null && response.length() > 200 ? response.substring(0, 200) + "..." : response));

        JsonNode root = mapper.readTree(response);

        if (root.has("status") && root.get("status").asText().equals("error")) {
            String msg = root.has("message") ? root.get("message").asText() : "Unknown error";
            throw new MarketDataException("Twelve Data history error: " + msg);
        }

        if (!root.has("values") || !root.get("values").isArray() || root.get("values").size() == 0) {
            throw new MarketDataException("Twelve Data history: no 'values' array in response for " + symbol);
        }

        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        for (JsonNode item : root.get("values")) {
            if (item.has("datetime") && item.has("close") && !item.get("close").isNull()) {
                String closeTxt = item.get("close").asText();
                if (closeTxt == null || closeTxt.equalsIgnoreCase("N/A") || closeTxt.trim().isEmpty()) {
                    continue; // skip invalid data points
                }
                try {
                    String date = item.get("datetime").asText();
                    BigDecimal close = new BigDecimal(closeTxt).setScale(2, RoundingMode.HALF_UP);
                    String openTxt = item.has("open") && !item.get("open").isNull() ? item.get("open").asText() : null;
                    String highTxt = item.has("high") && !item.get("high").isNull() ? item.get("high").asText() : null;
                    String lowTxt = item.has("low") && !item.get("low").isNull() ? item.get("low").asText() : null;
                    String volTxt = item.has("volume") && !item.get("volume").isNull() ? item.get("volume").asText() : null;
                    BigDecimal open = isNumericStr(openTxt) ? new BigDecimal(openTxt).setScale(2, RoundingMode.HALF_UP) : close;
                    BigDecimal high = isNumericStr(highTxt) ? new BigDecimal(highTxt).setScale(2, RoundingMode.HALF_UP) : close;
                    BigDecimal low = isNumericStr(lowTxt) ? new BigDecimal(lowTxt).setScale(2, RoundingMode.HALF_UP) : close;
                    Long volume = isNumericStr(volTxt) ? Long.parseLong(volTxt.trim()) : 0L;
                    points.add(new MarketHistoryDto.HistoryPoint(date, open, high, low, close, volume));
                } catch (NumberFormatException e) {
                    System.err.println("[MarketDataService] Skipping data point with unparseable value: " + item);
                }
            }
        }

        if (points.isEmpty()) {
            throw new MarketDataException("Twelve Data history: all data points were invalid for " + symbol);
        }

        // Twelve Data returns newest-first; reverse to get chronological order for charts
        java.util.Collections.reverse(points);
        System.out.println("[MarketDataService] Twelve Data history SUCCESS for " + symbol + ": " + points.size() + " points");
        return new MarketHistoryDto(symbol, points);
    }

    private boolean isNumericStr(String str) {
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
        return generateMockHistory(symbol, "1M");
    }

    private MarketHistoryDto generateMockHistory(String symbol, String range) {
        List<MarketHistoryDto.HistoryPoint> points = new ArrayList<>();
        BigDecimal currentPrice = new BigDecimal("100.00");
        
        if ("AAPL".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("175.50");
        else if ("MSFT".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("380.10");
        else if ("GOOGL".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("150.20");
        else if ("AMZN".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("145.40");
        else if ("TSLA".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("220.50");
        else if ("NVDA".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("974.00");
        else if ("RELIANCE".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("2450.00");
        else if ("TCS".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("3400.00");
        else if ("INFY".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("1500.00");
        else if ("HDFCBANK".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("1450.00");
        else if ("ICICIBANK".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("1050.00");
        else if ("SBIN".equalsIgnoreCase(symbol)) currentPrice = new BigDecimal("750.00");

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

        int numPoints;
        boolean isIntraday = false;

        switch (range.toUpperCase()) {
            case "1D":
                numPoints = 24; // 24 hours of data
                isIntraday = true;
                break;
            case "1W":
                numPoints = 7;
                break;
            case "3M":
                numPoints = 90;
                break;
            case "6M":
                numPoints = 180;
                break;
            case "1Y":
                numPoints = 250; // trading days in a year
                break;
            case "1M":
            default:
                numPoints = 30;
                break;
        }

        // Generate points moving forward in time so they chart correctly from left to right
        if (isIntraday) {
            cal.add(Calendar.HOUR, -numPoints);
            for (int i = 0; i < numPoints; i++) {
                String label = sdfTime.format(cal.getTime());
                BigDecimal open = currentPrice.multiply(BigDecimal.valueOf(0.998 + (Math.random() * 0.004))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal close = currentPrice.multiply(BigDecimal.valueOf(0.998 + (Math.random() * 0.004))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.0 + (Math.random() * 0.002))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(0.998)).setScale(2, RoundingMode.HALF_UP);
                Long volume = (long) (Math.random() * 50000) + 5000L;

                points.add(new MarketHistoryDto.HistoryPoint(label, open, high, low, close, volume));
                currentPrice = close;
                cal.add(Calendar.HOUR, 1);
            }
        } else {
            cal.add(Calendar.DATE, -numPoints);
            for (int i = 0; i < numPoints; i++) {
                String label = sdfDate.format(cal.getTime());
                BigDecimal open = currentPrice.multiply(BigDecimal.valueOf(0.99 + (Math.random() * 0.02))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal close = currentPrice.multiply(BigDecimal.valueOf(0.99 + (Math.random() * 0.02))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.0 + (Math.random() * 0.01))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(0.99)).setScale(2, RoundingMode.HALF_UP);
                Long volume = (long) (Math.random() * 2000000) + 50000L;

                points.add(new MarketHistoryDto.HistoryPoint(label, open, high, low, close, volume));
                currentPrice = close;
                cal.add(Calendar.DATE, 1);
            }
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
        String sector = "INR".equalsIgnoreCase(resolvedCurrency) ? "Energy" : "Technology";
        
        // Mock metrics based on symbol
        BigDecimal marketCap = new BigDecimal("150.5"); // default 150B
        BigDecimal fiftyTwoWeekHigh = new BigDecimal("180.00");
        BigDecimal fiftyTwoWeekLow = new BigDecimal("120.00");
        BigDecimal peRatio = new BigDecimal("25.5");
        BigDecimal dividendYield = new BigDecimal("1.2");
        Long volume = 1500000L;

        if ("AAPL".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("3200.0"); // 3.2T
            fiftyTwoWeekHigh = new BigDecimal("199.62");
            fiftyTwoWeekLow = new BigDecimal("164.08");
            peRatio = new BigDecimal("31.2");
            dividendYield = new BigDecimal("0.48");
            volume = 52000000L;
        } else if ("MSFT".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("3150.0"); // 3.15T
            fiftyTwoWeekHigh = new BigDecimal("430.82");
            fiftyTwoWeekLow = new BigDecimal("315.18");
            peRatio = new BigDecimal("35.4");
            dividendYield = new BigDecimal("0.71");
            volume = 22000000L;
        } else if ("GOOGL".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("1850.0"); // 1.85T
            fiftyTwoWeekHigh = new BigDecimal("160.22");
            fiftyTwoWeekLow = new BigDecimal("115.50");
            peRatio = new BigDecimal("26.1");
            dividendYield = new BigDecimal("0.40");
            volume = 28000000L;
        } else if ("AMZN".equalsIgnoreCase(symbol)) {
            sector = "Others";
            marketCap = new BigDecimal("1800.0");
            fiftyTwoWeekHigh = new BigDecimal("189.77");
            fiftyTwoWeekLow = new BigDecimal("118.35");
            peRatio = new BigDecimal("60.5");
            dividendYield = BigDecimal.ZERO;
            volume = 35000000L;
        } else if ("TSLA".equalsIgnoreCase(symbol)) {
            sector = "Others";
            marketCap = new BigDecimal("600.0");
            fiftyTwoWeekHigh = new BigDecimal("299.29");
            fiftyTwoWeekLow = new BigDecimal("138.80");
            peRatio = new BigDecimal("45.8");
            dividendYield = BigDecimal.ZERO;
            volume = 85000000L;
        } else if ("NVDA".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("2800.0");
            fiftyTwoWeekHigh = new BigDecimal("974.00");
            fiftyTwoWeekLow = new BigDecimal("373.56");
            peRatio = new BigDecimal("75.2");
            dividendYield = new BigDecimal("0.02");
            volume = 42000000L;
        } else if ("RELIANCE".equalsIgnoreCase(symbol)) {
            sector = "Energy";
            marketCap = new BigDecimal("16500.0"); // 16.5T INR
            fiftyTwoWeekHigh = new BigDecimal("2750.00");
            fiftyTwoWeekLow = new BigDecimal("2200.00");
            peRatio = new BigDecimal("28.4");
            dividendYield = new BigDecimal("0.38");
            volume = 5500000L;
        } else if ("TCS".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("13500.0");
            fiftyTwoWeekHigh = new BigDecimal("4254.75");
            fiftyTwoWeekLow = new BigDecimal("3070.30");
            peRatio = new BigDecimal("30.1");
            dividendYield = new BigDecimal("2.43");
            volume = 1200000L;
        } else if ("INFY".equalsIgnoreCase(symbol)) {
            sector = "Technology";
            marketCap = new BigDecimal("6500.0");
            fiftyTwoWeekHigh = new BigDecimal("1733.00");
            fiftyTwoWeekLow = new BigDecimal("1215.00");
            peRatio = new BigDecimal("24.8");
            dividendYield = new BigDecimal("2.35");
            volume = 4800000L;
        } else if ("HDFCBANK".equalsIgnoreCase(symbol)) {
            sector = "Banking";
            marketCap = new BigDecimal("11500.0");
            fiftyTwoWeekHigh = new BigDecimal("1757.50");
            fiftyTwoWeekLow = new BigDecimal("1363.55");
            peRatio = new BigDecimal("18.2");
            dividendYield = new BigDecimal("1.10");
            volume = 15000000L;
        } else if ("ICICIBANK".equalsIgnoreCase(symbol)) {
            sector = "Banking";
            marketCap = new BigDecimal("7200.0");
            fiftyTwoWeekHigh = new BigDecimal("1168.00");
            fiftyTwoWeekLow = new BigDecimal("898.85");
            peRatio = new BigDecimal("17.4");
            dividendYield = new BigDecimal("0.85");
            volume = 12000000L;
        } else if ("SBIN".equalsIgnoreCase(symbol)) {
            sector = "Banking";
            marketCap = new BigDecimal("6800.0");
            fiftyTwoWeekHigh = new BigDecimal("839.65");
            fiftyTwoWeekLow = new BigDecimal("555.30");
            peRatio = new BigDecimal("11.5");
            dividendYield = new BigDecimal("1.65");
            volume = 20000000L;
        }

        String web = mockInfo != null ? mockInfo[0].toLowerCase().replaceAll("[^a-z0-9]", "") + ".com" : "google.com";
        String website = "https://www." + web;
        String desc = "Mock Profile: " + name + " is a leading global listing traded on exchange " + resolvedExchange + " in currency " + resolvedCurrency + ".";

        return new CompanyProfileDto(symbol, name, resolvedExchange, resolvedCurrency, country, industry, sector, desc, website, marketCap, fiftyTwoWeekHigh, fiftyTwoWeekLow, peRatio, dividendYield, volume);
    }
}
