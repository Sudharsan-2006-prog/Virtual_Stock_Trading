package com.virtualstock.backend.controller;

import com.virtualstock.backend.entity.Portfolio;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.repository.PortfolioRepository;
import com.virtualstock.backend.service.MarketService;
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
    private MarketService marketService;

    @GetMapping
    public ResponseEntity<List<Portfolio>> getPortfolio(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        List<Portfolio> portfolios = portfolioRepository.findByUser(user);
        
        // Update current prices before returning
        for (Portfolio portfolio : portfolios) {
            java.util.Map<String, Object> quote = marketService.getStockQuote(portfolio.getStockSymbol());
            BigDecimal currentPrice = (BigDecimal) quote.get("price");
            BigDecimal dailyChange = (BigDecimal) quote.get("dailyChange");
            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                portfolio.setCurrentPrice(currentPrice);
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
        }

        return ResponseEntity.ok(portfolios);
    }
}
