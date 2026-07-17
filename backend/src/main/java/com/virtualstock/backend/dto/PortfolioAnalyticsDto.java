package com.virtualstock.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public class PortfolioAnalyticsDto {
    private BigDecimal totalInvestment;
    private BigDecimal currentValue;
    private BigDecimal totalReturn;
    private BigDecimal profitLossPercent;
    private BigDecimal todayProfitLoss;
    private int totalHoldings;
    private BigDecimal cashBalance;
    private String bestPerformingStock;
    private BigDecimal bestPerformingStockReturnPercent;
    private String worstPerformingStock;
    private BigDecimal worstPerformingStockReturnPercent;
    private BigDecimal diversityScore;
    private Map<String, BigDecimal> sectorDistribution;
    private Map<String, BigDecimal> holdingsAllocation;
    private String topGainer;
    private String topLoser;
    private double annualizedReturn;

    public PortfolioAnalyticsDto() {
    }

    public BigDecimal getTotalInvestment() {
        return totalInvestment;
    }

    public void setTotalInvestment(BigDecimal totalInvestment) {
        this.totalInvestment = totalInvestment;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public BigDecimal getProfitLossPercent() {
        return profitLossPercent;
    }

    public void setProfitLossPercent(BigDecimal profitLossPercent) {
        this.profitLossPercent = profitLossPercent;
    }

    public BigDecimal getTodayProfitLoss() {
        return todayProfitLoss;
    }

    public void setTodayProfitLoss(BigDecimal todayProfitLoss) {
        this.todayProfitLoss = todayProfitLoss;
    }

    public int getTotalHoldings() {
        return totalHoldings;
    }

    public void setTotalHoldings(int totalHoldings) {
        this.totalHoldings = totalHoldings;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public String getBestPerformingStock() {
        return bestPerformingStock;
    }

    public void setBestPerformingStock(String bestPerformingStock) {
        this.bestPerformingStock = bestPerformingStock;
    }

    public BigDecimal getBestPerformingStockReturnPercent() {
        return bestPerformingStockReturnPercent;
    }

    public void setBestPerformingStockReturnPercent(BigDecimal bestPerformingStockReturnPercent) {
        this.bestPerformingStockReturnPercent = bestPerformingStockReturnPercent;
    }

    public String getWorstPerformingStock() {
        return worstPerformingStock;
    }

    public void setWorstPerformingStock(String worstPerformingStock) {
        this.worstPerformingStock = worstPerformingStock;
    }

    public BigDecimal getWorstPerformingStockReturnPercent() {
        return worstPerformingStockReturnPercent;
    }

    public void setWorstPerformingStockReturnPercent(BigDecimal worstPerformingStockReturnPercent) {
        this.worstPerformingStockReturnPercent = worstPerformingStockReturnPercent;
    }

    public BigDecimal getDiversityScore() {
        return diversityScore;
    }

    public void setDiversityScore(BigDecimal diversityScore) {
        this.diversityScore = diversityScore;
    }

    public Map<String, BigDecimal> getSectorDistribution() {
        return sectorDistribution;
    }

    public void setSectorDistribution(Map<String, BigDecimal> sectorDistribution) {
        this.sectorDistribution = sectorDistribution;
    }

    public Map<String, BigDecimal> getHoldingsAllocation() {
        return holdingsAllocation;
    }

    public void setHoldingsAllocation(Map<String, BigDecimal> holdingsAllocation) {
        this.holdingsAllocation = holdingsAllocation;
    }

    public String getTopGainer() {
        return topGainer;
    }

    public void setTopGainer(String topGainer) {
        this.topGainer = topGainer;
    }

    public String getTopLoser() {
        return topLoser;
    }

    public void setTopLoser(String topLoser) {
        this.topLoser = topLoser;
    }

    public double getAnnualizedReturn() {
        return annualizedReturn;
    }

    public void setAnnualizedReturn(double annualizedReturn) {
        this.annualizedReturn = annualizedReturn;
    }
}
