package com.virtualstock.backend.controller;

import com.virtualstock.backend.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchStocks(@RequestParam("q") String query) {
        return ResponseEntity.ok(marketService.searchStocks(query));
    }

    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getStockPriceAndName(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(marketService.getStockPriceAndName(symbol));
    }
}
