package com.virtualstock.backend.controller;

import com.virtualstock.backend.dto.TradeRequest;
import com.virtualstock.backend.entity.Transaction;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.service.TradeService;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserService userService;

    @PostMapping("/buy")
    public ResponseEntity<Transaction> buyStock(@RequestBody TradeRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        Transaction transaction = tradeService.buyStock(
                user,
                request.getStockSymbol(),
                request.getCompanyName(),
                request.getQuantity(),
                request.getPrice()
        );

        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/sell")
    public ResponseEntity<Transaction> sellStock(@RequestBody TradeRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        Transaction transaction = tradeService.sellStock(
                user,
                request.getStockSymbol(),
                request.getQuantity(),
                request.getPrice()
        );

        return ResponseEntity.ok(transaction);
    }
}
