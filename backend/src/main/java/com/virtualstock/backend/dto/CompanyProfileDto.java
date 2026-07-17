package com.virtualstock.backend.dto;

import java.math.BigDecimal;

public class CompanyProfileDto {
    private String symbol;
    private String companyName;
    private String exchange;
    private String currency;
    private String country;
    private String industry;
    private String sector;
    private String description;
    private String website;
    
    // Enriched profile fields
    private BigDecimal marketCap;
    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
    private BigDecimal peRatio;
    private BigDecimal dividendYield;
    private Long volume;

    public CompanyProfileDto() {
    }

    public CompanyProfileDto(String symbol, String companyName, String exchange, String currency, String country, String industry, String sector, String description, String website) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.currency = currency;
        this.country = country;
        this.industry = industry;
        this.sector = sector;
        this.description = description;
        this.website = website;
    }

    public CompanyProfileDto(String symbol, String companyName, String exchange, String currency, String country, String industry, String sector, String description, String website, BigDecimal marketCap, BigDecimal fiftyTwoWeekHigh, BigDecimal fiftyTwoWeekLow, BigDecimal peRatio, BigDecimal dividendYield, Long volume) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.currency = currency;
        this.country = country;
        this.industry = industry;
        this.sector = sector;
        this.description = description;
        this.website = website;
        this.marketCap = marketCap;
        this.fiftyTwoWeekHigh = fiftyTwoWeekHigh;
        this.fiftyTwoWeekLow = fiftyTwoWeekLow;
        this.peRatio = peRatio;
        this.dividendYield = dividendYield;
        this.volume = volume;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public BigDecimal getFiftyTwoWeekHigh() {
        return fiftyTwoWeekHigh;
    }

    public void setFiftyTwoWeekHigh(BigDecimal fiftyTwoWeekHigh) {
        this.fiftyTwoWeekHigh = fiftyTwoWeekHigh;
    }

    public BigDecimal getFiftyTwoWeekLow() {
        return fiftyTwoWeekLow;
    }

    public void setFiftyTwoWeekLow(BigDecimal fiftyTwoWeekLow) {
        this.fiftyTwoWeekLow = fiftyTwoWeekLow;
    }

    public BigDecimal getPeRatio() {
        return peRatio;
    }

    public void setPeRatio(BigDecimal peRatio) {
        this.peRatio = peRatio;
    }

    public BigDecimal getDividendYield() {
        return dividendYield;
    }

    public void setDividendYield(BigDecimal dividendYield) {
        this.dividendYield = dividendYield;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}
