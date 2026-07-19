package com.virtualstock.backend.controller;

import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.repository.UserRepository;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, BigDecimal>> getWalletBalance(Authentication authentication) {
        String email = authentication.getName();
        System.out.println("[WalletController] getWalletBalance called for: " + email);

        User user = userService.getUserByEmail(email);
        BigDecimal balance = user.getWalletBalance();

        // Auto-initialize NULL wallet balance to ₹1,00,000 for legacy/seeded users
        if (balance == null) {
            System.out.println("[WalletController] wallet_balance is NULL for " + email + " — initializing to 100000.00");
            balance = new BigDecimal("100000.00");
            user.setWalletBalance(balance);
            userRepository.save(user);
            System.out.println("[WalletController] Saved default balance for " + email);
        }

        System.out.println("[WalletController] Returning balance=" + balance + " for " + email);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }
}
