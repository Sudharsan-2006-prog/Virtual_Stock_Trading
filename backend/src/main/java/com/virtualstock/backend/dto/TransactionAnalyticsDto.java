package com.virtualstock.backend.dto;

import java.math.BigDecimal;

public class TransactionAnalyticsDto {
    private int numTrades;
    private BigDecimal totalBuys;
    private BigDecimal totalSells;
    private double averageHoldingPeriodDays;
    private BigDecimal averagePurchaseValue;
    private BigDecimal largestGain;
    private BigDecimal largestLoss;

    public TransactionAnalyticsDto() {
    }

    public int getNumTrades() {
        return numTrades;
    }

    public void setNumTrades(int numTrades) {
        this.numTrades = numTrades;
    }

    public BigDecimal getTotalBuys() {
        return totalBuys;
    }

    public void setTotalBuys(BigDecimal totalBuys) {
        this.totalBuys = totalBuys;
    }

    public BigDecimal getTotalSells() {
        return totalSells;
    }

    public void setTotalSells(BigDecimal totalSells) {
        this.totalSells = totalSells;
    }

    public double getAverageHoldingPeriodDays() {
        return averageHoldingPeriodDays;
    }

    public void setAverageHoldingPeriodDays(double averageHoldingPeriodDays) {
        this.averageHoldingPeriodDays = averageHoldingPeriodDays;
    }

    public BigDecimal getAveragePurchaseValue() {
        return averagePurchaseValue;
    }

    public void setAveragePurchaseValue(BigDecimal averagePurchaseValue) {
        this.averagePurchaseValue = averagePurchaseValue;
    }

    public BigDecimal getLargestGain() {
        return largestGain;
    }

    public void setLargestGain(BigDecimal largestGain) {
        this.largestGain = largestGain;
    }

    public BigDecimal getLargestLoss() {
        return largestLoss;
    }

    public void setLargestLoss(BigDecimal largestLoss) {
        this.largestLoss = largestLoss;
    }
}
