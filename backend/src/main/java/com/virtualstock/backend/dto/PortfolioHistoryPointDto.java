package com.virtualstock.backend.dto;

import java.math.BigDecimal;

public class PortfolioHistoryPointDto {
    private String date;
    private BigDecimal portfolioValue;
    private BigDecimal cashBalance;
    private BigDecimal stockValue;

    public PortfolioHistoryPointDto() {
    }

    public PortfolioHistoryPointDto(String date, BigDecimal portfolioValue, BigDecimal cashBalance, BigDecimal stockValue) {
        this.date = date;
        this.portfolioValue = portfolioValue;
        this.cashBalance = cashBalance;
        this.stockValue = stockValue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getStockValue() {
        return stockValue;
    }

    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }
}
