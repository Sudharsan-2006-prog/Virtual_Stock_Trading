package com.virtualstock.backend.controller;

import com.virtualstock.backend.dto.CompanyProfileDto;
import com.virtualstock.backend.dto.MarketHistoryDto;
import com.virtualstock.backend.dto.MarketQuoteDto;
import com.virtualstock.backend.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchStocks(@RequestParam("q") String query) {
        return ResponseEntity.ok(marketDataService.searchStocks(query));
    }

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<MarketQuoteDto> getQuote(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(marketDataService.getQuote(symbol));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<MarketHistoryDto> getHistory(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(marketDataService.getHistory(symbol));
    }

    @GetMapping("/company/{symbol}")
    public ResponseEntity<CompanyProfileDto> getCompanyProfile(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(marketDataService.getCompanyProfile(symbol));
    }

    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getStockPriceAndName(@PathVariable("symbol") String symbol) {
        MarketQuoteDto quote = marketDataService.getQuote(symbol);
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", quote.getSymbol());
        response.put("price", quote.getCurrentPrice());
        response.put("companyName", quote.getCompanyName());
        response.put("dailyChange", quote.getChange());
        response.put("changePercent", quote.getChangePercent());
        return ResponseEntity.ok(response);
    }
}

