package com.virtualstock.backend.dto;

import java.util.List;

public class DashboardAnalyticsResponseDto {
    private PortfolioAnalyticsDto portfolioAnalytics;
    private TransactionAnalyticsDto transactionAnalytics;
    private List<PortfolioHistoryPointDto> portfolioHistory;

    public DashboardAnalyticsResponseDto() {
    }

    public DashboardAnalyticsResponseDto(PortfolioAnalyticsDto portfolioAnalytics, TransactionAnalyticsDto transactionAnalytics, List<PortfolioHistoryPointDto> portfolioHistory) {
        this.portfolioAnalytics = portfolioAnalytics;
        this.transactionAnalytics = transactionAnalytics;
        this.portfolioHistory = portfolioHistory;
    }

    public PortfolioAnalyticsDto getPortfolioAnalytics() {
        return portfolioAnalytics;
    }

    public void setPortfolioAnalytics(PortfolioAnalyticsDto portfolioAnalytics) {
        this.portfolioAnalytics = portfolioAnalytics;
    }

    public TransactionAnalyticsDto getTransactionAnalytics() {
        return transactionAnalytics;
    }

    public void setTransactionAnalytics(TransactionAnalyticsDto transactionAnalytics) {
        this.transactionAnalytics = transactionAnalytics;
    }

    public List<PortfolioHistoryPointDto> getPortfolioHistory() {
        return portfolioHistory;
    }

    public void setPortfolioHistory(List<PortfolioHistoryPointDto> portfolioHistory) {
        this.portfolioHistory = portfolioHistory;
    }
}
