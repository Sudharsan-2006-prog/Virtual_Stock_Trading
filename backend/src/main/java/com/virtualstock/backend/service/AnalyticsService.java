package com.virtualstock.backend.service;

import com.virtualstock.backend.dto.*;
import com.virtualstock.backend.entity.*;
import com.virtualstock.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MarketDataService marketDataService;

    public DashboardAnalyticsResponseDto getDashboardAnalytics(User user) {
        List<Portfolio> portfolios = portfolioRepository.findByUser(user);
        List<Transaction> chronTransactions = transactionRepository.findByUserOrderByTimestampDesc(user);

        PortfolioAnalyticsDto portAnalytics = calculatePortfolioAnalytics(portfolios, user, chronTransactions);
        TransactionAnalyticsDto txAnalytics = calculateTransactionAnalytics(chronTransactions);
        List<PortfolioHistoryPointDto> history = calculatePortfolioHistory(portfolios, user, chronTransactions);

        return new DashboardAnalyticsResponseDto(portAnalytics, txAnalytics, history);
    }

    private PortfolioAnalyticsDto calculatePortfolioAnalytics(List<Portfolio> portfolios, User user, List<Transaction> transactions) {
        PortfolioAnalyticsDto dto = new PortfolioAnalyticsDto();
        dto.setCashBalance(user.getWalletBalance());

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal todayProfitLoss = BigDecimal.ZERO;
        int totalHoldings = portfolios.size();

        Map<String, BigDecimal> sectorDistribution = new HashMap<>();
        sectorDistribution.put("Technology", BigDecimal.ZERO);
        sectorDistribution.put("Banking", BigDecimal.ZERO);
        sectorDistribution.put("Energy", BigDecimal.ZERO);
        sectorDistribution.put("Healthcare", BigDecimal.ZERO);
        sectorDistribution.put("Others", BigDecimal.ZERO);

        Map<String, BigDecimal> holdingsAllocation = new HashMap<>();

        String bestPerformingStock = null;
        BigDecimal bestReturnPercent = BigDecimal.valueOf(-Double.MAX_VALUE);
        String worstPerformingStock = null;
        BigDecimal worstReturnPercent = BigDecimal.valueOf(Double.MAX_VALUE);

        String topGainer = null;
        BigDecimal maxTodayGain = BigDecimal.valueOf(-Double.MAX_VALUE);
        String topLoser = null;
        BigDecimal maxTodayLoss = BigDecimal.valueOf(Double.MAX_VALUE);

        for (Portfolio item : portfolios) {
            BigDecimal invested = item.getInvestedAmount();
            BigDecimal marketVal = item.getMarketValue();
            BigDecimal todayPL = item.getTodayProfitLoss() != null ? item.getTodayProfitLoss() : BigDecimal.ZERO;

            // Convert USD holdings to INR for unified calculations
            if ("USD".equalsIgnoreCase(item.getCurrency())) {
                invested = invested.multiply(new BigDecimal("83.0"));
                marketVal = marketVal.multiply(new BigDecimal("83.0"));
                todayPL = todayPL.multiply(new BigDecimal("83.0"));
            }

            totalInvestment = totalInvestment.add(invested);
            currentValue = currentValue.add(marketVal);
            todayProfitLoss = todayProfitLoss.add(todayPL);

            // Fetch company profile for sector details
            String sector = "Others";
            try {
                CompanyProfileDto profile = marketDataService.getCompanyProfile(item.getStockSymbol());
                sector = determineSector(item.getStockSymbol(), profile != null ? profile.getSector() : null);
            } catch (Exception e) {
                sector = determineSector(item.getStockSymbol(), null);
            }

            sectorDistribution.put(sector, sectorDistribution.getOrDefault(sector, BigDecimal.ZERO).add(marketVal));
            holdingsAllocation.put(item.getStockSymbol(), marketVal);

            // Calculate profit percentages
            BigDecimal profitPercent = BigDecimal.ZERO;
            if (invested.compareTo(BigDecimal.ZERO) > 0) {
                profitPercent = marketVal.subtract(invested).divide(invested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }

            if (profitPercent.compareTo(bestReturnPercent) > 0) {
                bestReturnPercent = profitPercent;
                bestPerformingStock = item.getStockSymbol();
            }
            if (profitPercent.compareTo(worstReturnPercent) < 0) {
                worstReturnPercent = profitPercent;
                worstPerformingStock = item.getStockSymbol();
            }

            // Top Gainer and Top Loser today based on today's P/L
            if (todayPL.compareTo(maxTodayGain) > 0) {
                maxTodayGain = todayPL;
                topGainer = item.getStockSymbol();
            }
            if (todayPL.compareTo(maxTodayLoss) < 0) {
                maxTodayLoss = todayPL;
                topLoser = item.getStockSymbol();
            }
        }

        dto.setTotalInvestment(totalInvestment.setScale(2, RoundingMode.HALF_UP));
        dto.setCurrentValue(currentValue.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalHoldings(totalHoldings);
        dto.setTodayProfitLoss(todayProfitLoss.setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalReturn = currentValue.subtract(totalInvestment);
        dto.setTotalReturn(totalReturn.setScale(2, RoundingMode.HALF_UP));

        BigDecimal profitLossPercent = BigDecimal.ZERO;
        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercent = totalReturn.divide(totalInvestment, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }
        dto.setProfitLossPercent(profitLossPercent.setScale(2, RoundingMode.HALF_UP));

        // Format gainers/losers
        dto.setBestPerformingStock(bestPerformingStock != null ? bestPerformingStock + " (" + bestReturnPercent.setScale(2, RoundingMode.HALF_UP) + "%)" : "N/A");
        dto.setWorstPerformingStock(worstPerformingStock != null ? worstPerformingStock + " (" + worstReturnPercent.setScale(2, RoundingMode.HALF_UP) + "%)" : "N/A");
        dto.setTopGainer(topGainer != null ? topGainer + " (₹" + maxTodayGain.setScale(2, RoundingMode.HALF_UP) + ")" : "N/A");
        dto.setTopLoser(topLoser != null ? topLoser + " (₹" + maxTodayLoss.setScale(2, RoundingMode.HALF_UP) + ")" : "N/A");

        // Calculate Diversity Score (Herfindahl-Hirschman Index mapping)
        BigDecimal diversityScore = BigDecimal.valueOf(100);
        if (currentValue.compareTo(BigDecimal.ZERO) > 0) {
            double hhi = 0;
            for (BigDecimal val : holdingsAllocation.values()) {
                double weight = val.doubleValue() / currentValue.doubleValue() * 100;
                hhi += weight * weight;
            }
            double score = Math.max(0, 100 - (hhi / 150.0));
            diversityScore = BigDecimal.valueOf(score);
        } else {
            diversityScore = BigDecimal.ZERO;
        }
        dto.setDiversityScore(diversityScore.setScale(2, RoundingMode.HALF_UP));

        dto.setSectorDistribution(sectorDistribution);
        dto.setHoldingsAllocation(holdingsAllocation);

        // Annualized Return (CAGR)
        double annualizedReturn = 0.0;
        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            long days = 1;
            if (!transactions.isEmpty()) {
                LocalDateTime firstTradeTime = transactions.get(transactions.size() - 1).getTimestamp();
                days = java.time.temporal.ChronoUnit.DAYS.between(firstTradeTime, LocalDateTime.now());
                if (days <= 0) days = 1;
            }
            double ratio = currentValue.doubleValue() / totalInvestment.doubleValue();
            if (ratio > 0) {
                annualizedReturn = (Math.pow(ratio, 365.0 / days) - 1.0) * 100.0;
            }
        }
        dto.setAnnualizedReturn(Double.isInfinite(annualizedReturn) || Double.isNaN(annualizedReturn) ? 0.0 : annualizedReturn);

        return dto;
    }

    private TransactionAnalyticsDto calculateTransactionAnalytics(List<Transaction> chronTransactions) {
        TransactionAnalyticsDto dto = new TransactionAnalyticsDto();
        dto.setNumTrades(chronTransactions.size());

        List<Transaction> ascTransactions = new ArrayList<>(chronTransactions);
        Collections.reverse(ascTransactions);

        BigDecimal totalBuys = BigDecimal.ZERO;
        BigDecimal totalSells = BigDecimal.ZERO;
        BigDecimal largestGain = BigDecimal.ZERO;
        BigDecimal largestLoss = BigDecimal.ZERO;

        Map<String, Queue<BuyRecord>> buyQueues = new HashMap<>();
        long totalDaysHeld = 0;
        long totalQtySoldForPeriod = 0;
        int buyCount = 0;

        for (Transaction tx : ascTransactions) {
            String sym = tx.getStockSymbol();
            BigDecimal price = tx.getPrice();
            BigDecimal totalAmt = tx.getTotalAmount();

            if ("USD".equalsIgnoreCase(tx.getCurrency())) {
                price = price.multiply(new BigDecimal("83.0"));
                totalAmt = totalAmt.multiply(new BigDecimal("83.0"));
            }

            if (tx.getTransactionType() == TransactionType.BUY) {
                totalBuys = totalBuys.add(totalAmt);
                buyCount++;
                buyQueues.computeIfAbsent(sym, k -> new LinkedList<>()).add(new BuyRecord(tx.getQuantity(), price, tx.getTimestamp()));
            } else {
                totalSells = totalSells.add(totalAmt);

                int sellQty = tx.getQuantity();
                BigDecimal realizedCost = BigDecimal.ZERO;
                Queue<BuyRecord> queue = buyQueues.get(sym);

                while (sellQty > 0 && queue != null && !queue.isEmpty()) {
                    BuyRecord oldestBuy = queue.peek();
                    int matchQty = Math.min(sellQty, oldestBuy.qty);

                    long days = java.time.temporal.ChronoUnit.DAYS.between(oldestBuy.timestamp, tx.getTimestamp());
                    if (days < 0) days = 0;
                    totalDaysHeld += days * matchQty;
                    totalQtySoldForPeriod += matchQty;

                    realizedCost = realizedCost.add(oldestBuy.price.multiply(new BigDecimal(matchQty)));

                    oldestBuy.qty -= matchQty;
                    sellQty -= matchQty;
                    if (oldestBuy.qty == 0) {
                        queue.poll();
                    }
                }

                // Profit/Loss calculations for this sale
                BigDecimal saleAmount = tx.getPrice().multiply(new BigDecimal(tx.getQuantity()));
                if ("USD".equalsIgnoreCase(tx.getCurrency())) {
                    saleAmount = saleAmount.multiply(new BigDecimal("83.0"));
                }
                BigDecimal pnl = saleAmount.subtract(realizedCost);

                if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                    if (pnl.compareTo(largestGain) > 0) {
                        largestGain = pnl;
                    }
                } else {
                    if (pnl.compareTo(largestLoss) < 0) {
                        largestLoss = pnl;
                    }
                }
            }
        }

        dto.setTotalBuys(totalBuys.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalSells(totalSells.setScale(2, RoundingMode.HALF_UP));
        dto.setLargestGain(largestGain.setScale(2, RoundingMode.HALF_UP));
        dto.setLargestLoss(largestLoss.setScale(2, RoundingMode.HALF_UP)); // stored as negative

        BigDecimal avgPurchase = BigDecimal.ZERO;
        if (buyCount > 0) {
            avgPurchase = totalBuys.divide(new BigDecimal(buyCount), 2, RoundingMode.HALF_UP);
        }
        dto.setAveragePurchaseValue(avgPurchase);

        double avgHolding = 0.0;
        if (totalQtySoldForPeriod > 0) {
            avgHolding = (double) totalDaysHeld / totalQtySoldForPeriod;
        }
        dto.setAverageHoldingPeriodDays(avgHolding);

        return dto;
    }

    private List<PortfolioHistoryPointDto> calculatePortfolioHistory(List<Portfolio> portfolios, User user, List<Transaction> chronTransactions) {
        List<PortfolioHistoryPointDto> historyPoints = new ArrayList<>();
        BigDecimal runningCash = user.getWalletBalance();
        
        Map<String, Integer> runningQty = new HashMap<>();
        for (Portfolio p : portfolios) {
            runningQty.put(p.getStockSymbol(), p.getQuantity());
        }

        // Prepare 30 dates descending
        List<String> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < 30; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DATE, -1);
        }

        // Prefetch price history maps
        List<String> symbols = new ArrayList<>(runningQty.keySet());
        Map<String, Map<String, BigDecimal>> priceHistory = buildHistoryMap(symbols);

        int txIdx = 0;

        for (String dateStr : dates) {
            BigDecimal stockVal = BigDecimal.ZERO;

            for (Map.Entry<String, Integer> entry : runningQty.entrySet()) {
                String sym = entry.getKey();
                int qty = entry.getValue();
                if (qty > 0) {
                    BigDecimal price = BigDecimal.ZERO;
                    if (priceHistory.containsKey(sym) && priceHistory.get(sym).containsKey(dateStr)) {
                        price = priceHistory.get(sym).get(dateStr);
                    } else {
                        price = getPortfolioCurrentPrice(portfolios, sym);
                    }

                    BigDecimal value = price.multiply(new BigDecimal(qty));
                    if (isStockUsd(portfolios, sym)) {
                        value = value.multiply(new BigDecimal("83.0"));
                    }
                    stockVal = stockVal.add(value);
                }
            }

            BigDecimal totalVal = runningCash.add(stockVal);
            historyPoints.add(0, new PortfolioHistoryPointDto(
                    dateStr,
                    totalVal.setScale(2, RoundingMode.HALF_UP),
                    runningCash.setScale(2, RoundingMode.HALF_UP),
                    stockVal.setScale(2, RoundingMode.HALF_UP)
            ));

            // Rollback transactions that happened on dateStr
            while (txIdx < chronTransactions.size()) {
                Transaction tx = chronTransactions.get(txIdx);
                String txDateStr = tx.getTimestamp().toLocalDate().toString();
                
                if (txDateStr.equals(dateStr)) {
                    BigDecimal amt = tx.getTotalAmount();
                    if ("USD".equalsIgnoreCase(tx.getCurrency())) {
                        amt = amt.multiply(new BigDecimal("83.0"));
                    }

                    if (tx.getTransactionType() == TransactionType.BUY) {
                        runningCash = runningCash.add(amt);
                        runningQty.put(tx.getStockSymbol(), runningQty.getOrDefault(tx.getStockSymbol(), 0) - tx.getQuantity());
                    } else {
                        runningCash = runningCash.subtract(amt);
                        runningQty.put(tx.getStockSymbol(), runningQty.getOrDefault(tx.getStockSymbol(), 0) + tx.getQuantity());
                    }
                    txIdx++;
                } else {
                    break;
                }
            }
        }

        return historyPoints;
    }

    private Map<String, Map<String, BigDecimal>> buildHistoryMap(List<String> symbols) {
        Map<String, Map<String, BigDecimal>> historyMap = new HashMap<>();
        for (String symbol : symbols) {
            try {
                MarketHistoryDto historyDto = marketDataService.getHistory(symbol, "1M");
                Map<String, BigDecimal> datePriceMap = new HashMap<>();
                if (historyDto != null && historyDto.getHistory() != null) {
                    for (MarketHistoryDto.HistoryPoint pt : historyDto.getHistory()) {
                        datePriceMap.put(pt.getDate(), pt.getClose());
                    }
                }
                historyMap.put(symbol, datePriceMap);
            } catch (Exception e) {
                System.err.println("Error pre-fetching history for " + symbol + ": " + e.getMessage());
            }
        }
        return historyMap;
    }

    private BigDecimal getPortfolioCurrentPrice(List<Portfolio> portfolios, String symbol) {
        for (Portfolio p : portfolios) {
            if (p.getStockSymbol().equalsIgnoreCase(symbol)) {
                return p.getCurrentPrice() != null ? p.getCurrentPrice() : p.getAverageBuyPrice();
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean isStockUsd(List<Portfolio> portfolios, String symbol) {
        for (Portfolio p : portfolios) {
            if (p.getStockSymbol().equalsIgnoreCase(symbol)) {
                return "USD".equalsIgnoreCase(p.getCurrency());
            }
        }
        return false;
    }

    private String determineSector(String symbol, String apiSector) {
        if (symbol == null) return "Others";
        String clean = symbol.trim().toUpperCase();
        if (clean.equals("AAPL") || clean.equals("MSFT") || clean.equals("GOOGL") || clean.equals("NVDA") || clean.equals("META") || clean.equals("TCS") || clean.equals("INFY")) {
            return "Technology";
        }
        if (clean.equals("HDFCBANK") || clean.equals("ICICIBANK") || clean.equals("SBIN") || clean.equals("AXISBANK")) {
            return "Banking";
        }
        if (clean.equals("RELIANCE")) {
            return "Energy";
        }
        if (apiSector != null && !apiSector.isEmpty()) {
            String lower = apiSector.toLowerCase();
            if (lower.contains("tech")) return "Technology";
            if (lower.contains("bank") || lower.contains("financial")) return "Banking";
            if (lower.contains("energy") || lower.contains("oil") || lower.contains("gas") || lower.contains("petro")) return "Energy";
            if (lower.contains("health") || lower.contains("pharm") || lower.contains("biotech") || lower.contains("medic")) return "Healthcare";
        }
        return "Others";
    }

    private static class BuyRecord {
        int qty;
        BigDecimal price;
        LocalDateTime timestamp;

        BuyRecord(int qty, BigDecimal price, LocalDateTime timestamp) {
            this.qty = qty;
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
