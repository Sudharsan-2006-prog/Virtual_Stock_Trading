package com.virtualstock.backend.controller;

import com.virtualstock.backend.entity.Portfolio;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.repository.PortfolioRepository;
import com.virtualstock.backend.service.MarketDataService;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping
    public ResponseEntity<List<Portfolio>> getPortfolio(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        List<Portfolio> portfolios = portfolioRepository.findByUser(user);
        
        // Update current prices before returning
        for (Portfolio portfolio : portfolios) {
            try {
                com.virtualstock.backend.dto.MarketQuoteDto quote = marketDataService.getQuote(portfolio.getStockSymbol());
                BigDecimal currentPrice = quote.getCurrentPrice();
                BigDecimal dailyChange = quote.getChange();
                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    portfolio.setCurrentPrice(currentPrice);
                    portfolio.setCurrency(quote.getCurrency());
                    portfolio.setExchange(quote.getExchange());
                    BigDecimal marketValue = currentPrice.multiply(new BigDecimal(portfolio.getQuantity()));
                    portfolio.setMarketValue(marketValue);
                    portfolio.setProfitLoss(marketValue.subtract(portfolio.getInvestedAmount()));
                    if (dailyChange != null) {
                        portfolio.setTodayProfitLoss(dailyChange.multiply(new BigDecimal(portfolio.getQuantity())).setScale(2, java.math.RoundingMode.HALF_UP));
                    } else {
                        portfolio.setTodayProfitLoss(BigDecimal.ZERO);
                    }
                    portfolioRepository.save(portfolio); // Save updated prices to DB
                } else {
                    portfolio.setTodayProfitLoss(BigDecimal.ZERO);
                }
            } catch (Exception e) {
                System.err.println("Error updating portfolio price for " + portfolio.getStockSymbol() + ": " + e.getMessage());
                portfolio.setTodayProfitLoss(BigDecimal.ZERO);
            }
        }

        return ResponseEntity.ok(portfolios);
    }
}
