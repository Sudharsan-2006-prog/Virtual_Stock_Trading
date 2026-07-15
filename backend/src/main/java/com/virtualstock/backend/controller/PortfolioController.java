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
            BigDecimal currentPrice = marketService.getCurrentPrice(portfolio.getStockSymbol(), portfolio.getCurrentPrice());
            if (currentPrice != null) {
                portfolio.setCurrentPrice(currentPrice);
                BigDecimal marketValue = currentPrice.multiply(new BigDecimal(portfolio.getQuantity()));
                portfolio.setMarketValue(marketValue);
                portfolio.setProfitLoss(marketValue.subtract(portfolio.getInvestedAmount()));
                portfolioRepository.save(portfolio); // Optional: Save updated prices to DB
            }
        }

        return ResponseEntity.ok(portfolios);
    }
}
