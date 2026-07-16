package com.virtualstock.backend.service;

import com.virtualstock.backend.entity.Portfolio;
import com.virtualstock.backend.entity.Transaction;
import com.virtualstock.backend.entity.TransactionType;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.repository.PortfolioRepository;
import com.virtualstock.backend.repository.TransactionRepository;
import com.virtualstock.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class TradeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MarketService marketService;

    @Transactional
    public Transaction buyStock(User user, String stockSymbol, String companyName, int quantity, BigDecimal frontendPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        BigDecimal currentPrice = marketService.getCurrentPrice(stockSymbol, null);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        if (user.getWalletBalance() == null) {
            throw new IllegalArgumentException("User wallet balance not initialized");
        }

        BigDecimal totalCost = currentPrice.multiply(new BigDecimal(quantity));

        if (user.getWalletBalance().compareTo(totalCost) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        // Deduct from wallet
        user.setWalletBalance(user.getWalletBalance().subtract(totalCost));
        userRepository.save(user);

        // Update or create portfolio
        Optional<Portfolio> existingPortfolioOpt = portfolioRepository.findByUserAndStockSymbol(user, stockSymbol);
        if (existingPortfolioOpt.isPresent()) {
            Portfolio portfolio = existingPortfolioOpt.get();
            int newQuantity = portfolio.getQuantity() + quantity;
            BigDecimal newInvestedAmount = portfolio.getInvestedAmount().add(totalCost);
            BigDecimal newAverageBuyPrice = newInvestedAmount.divide(new BigDecimal(newQuantity), 2, RoundingMode.HALF_UP);
            
            portfolio.setQuantity(newQuantity);
            portfolio.setInvestedAmount(newInvestedAmount);
            portfolio.setAverageBuyPrice(newAverageBuyPrice);
            portfolio.setCurrentPrice(currentPrice);
            
            // Market value and P/L
            BigDecimal marketValue = currentPrice.multiply(new BigDecimal(newQuantity));
            portfolio.setMarketValue(marketValue);
            portfolio.setProfitLoss(marketValue.subtract(newInvestedAmount));
            
            portfolioRepository.save(portfolio);
        } else {
            Portfolio portfolio = new Portfolio();
            portfolio.setUser(user);
            portfolio.setStockSymbol(stockSymbol);
            portfolio.setCompanyName(companyName);
            portfolio.setQuantity(quantity);
            portfolio.setAverageBuyPrice(currentPrice);
            portfolio.setCurrentPrice(currentPrice);
            portfolio.setInvestedAmount(totalCost);
            portfolio.setMarketValue(totalCost);
            portfolio.setProfitLoss(BigDecimal.ZERO);
            
            portfolioRepository.save(portfolio);
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStockSymbol(stockSymbol);
        transaction.setCompanyName(companyName);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setQuantity(quantity);
        transaction.setPrice(currentPrice);
        transaction.setTotalAmount(totalCost);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction sellStock(User user, String stockSymbol, int quantity, BigDecimal frontendPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Portfolio portfolio = portfolioRepository.findByUserAndStockSymbol(user, stockSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found in portfolio"));

        if (portfolio.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock quantity to sell");
        }

        BigDecimal currentPrice = marketService.getCurrentPrice(stockSymbol, null);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        BigDecimal totalRevenue = currentPrice.multiply(new BigDecimal(quantity));

        // Credit to wallet
        user.setWalletBalance(user.getWalletBalance().add(totalRevenue));
        userRepository.save(user);

        // Update portfolio
        int newQuantity = portfolio.getQuantity() - quantity;
        if (newQuantity == 0) {
            portfolioRepository.delete(portfolio);
        } else {
            BigDecimal remainingInvestedAmount = portfolio.getAverageBuyPrice()
                    .multiply(new BigDecimal(newQuantity))
                    .setScale(2, RoundingMode.HALF_UP);
            portfolio.setQuantity(newQuantity);
            portfolio.setInvestedAmount(remainingInvestedAmount);
            portfolio.setCurrentPrice(currentPrice);
            
            BigDecimal marketValue = currentPrice.multiply(new BigDecimal(newQuantity));
            portfolio.setMarketValue(marketValue);
            portfolio.setProfitLoss(marketValue.subtract(remainingInvestedAmount));
            
            portfolioRepository.save(portfolio);
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStockSymbol(stockSymbol);
        transaction.setCompanyName(portfolio.getCompanyName());
        transaction.setTransactionType(TransactionType.SELL);
        transaction.setQuantity(quantity);
        transaction.setPrice(currentPrice);
        transaction.setTotalAmount(totalRevenue);

        return transactionRepository.save(transaction);
    }
}
