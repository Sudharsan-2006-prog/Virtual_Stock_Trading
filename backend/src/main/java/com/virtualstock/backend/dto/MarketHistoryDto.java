package com.virtualstock.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class MarketHistoryDto {
    private String symbol;
    private List<HistoryPoint> history;

    public MarketHistoryDto() {
    }

    public MarketHistoryDto(String symbol, List<HistoryPoint> history) {
        this.symbol = symbol;
        this.history = history;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public List<HistoryPoint> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryPoint> history) {
        this.history = history;
    }

    public static class HistoryPoint {
        private String date;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;

        public HistoryPoint() {
        }

        public HistoryPoint(String date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getOpen() {
            return open;
        }

        public void setOpen(BigDecimal open) {
            this.open = open;
        }

        public BigDecimal getHigh() {
            return high;
        }

        public void setHigh(BigDecimal high) {
            this.high = high;
        }

        public BigDecimal getLow() {
            return low;
        }

        public void setLow(BigDecimal low) {
            this.low = low;
        }

        public BigDecimal getClose() {
            return close;
        }

        public void setClose(BigDecimal close) {
            this.close = close;
        }

        public Long getVolume() {
            return volume;
        }

        public void setVolume(Long volume) {
            this.volume = volume;
        }
    }
}
