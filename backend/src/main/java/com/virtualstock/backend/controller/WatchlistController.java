package com.virtualstock.backend.controller;

import com.virtualstock.backend.dto.WatchlistRequest;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.entity.Watchlist;
import com.virtualstock.backend.repository.WatchlistRepository;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private com.virtualstock.backend.service.MarketDataService marketDataService;

    @GetMapping
    public ResponseEntity<List<Watchlist>> getWatchlist(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        List<Watchlist> watchlists = watchlistRepository.findByUser(user);
        for (Watchlist item : watchlists) {
            try {
                com.virtualstock.backend.dto.MarketQuoteDto quote = marketDataService.getQuote(item.getStockSymbol());
                item.setCurrentPrice(quote.getCurrentPrice());
                item.setDailyChange(quote.getChange());
                item.setChangePercent(quote.getChangePercent());
                item.setCurrency(quote.getCurrency());
                item.setExchange(quote.getExchange());
                watchlistRepository.save(item); // Persist updated currency and exchange details
            } catch (Exception e) {
                System.err.println("Error updating watchlist price for " + item.getStockSymbol() + ": " + e.getMessage());
                item.setCurrentPrice(java.math.BigDecimal.ZERO);
                item.setDailyChange(java.math.BigDecimal.ZERO);
                item.setChangePercent(java.math.BigDecimal.ZERO);
            }
        }
        return ResponseEntity.ok(watchlists);
    }

    @PostMapping
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        Optional<Watchlist> existing = watchlistRepository.findByUserAndStockSymbol(user, request.getStockSymbol());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Stock already in watchlist");
        }

        Watchlist watchlist = new Watchlist();
        watchlist.setUser(user);
        watchlist.setStockSymbol(request.getStockSymbol());
        watchlist.setCompanyName(request.getCompanyName());
        
        try {
            com.virtualstock.backend.dto.MarketQuoteDto quote = marketDataService.getQuote(request.getStockSymbol());
            watchlist.setCompanyName(quote.getCompanyName());
            watchlist.setCurrency(quote.getCurrency());
            watchlist.setExchange(quote.getExchange());
        } catch (Exception e) {
            watchlist.setCurrency("NSE".equalsIgnoreCase(request.getStockSymbol()) ? "INR" : "USD");
            watchlist.setExchange("NSE".equalsIgnoreCase(request.getStockSymbol()) ? "NSE" : "NASDAQ");
        }

        Watchlist saved = watchlistRepository.save(watchlist);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(id);
        if (watchlistOpt.isEmpty() || !watchlistOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body("Watchlist item not found");
        }

        watchlistRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
