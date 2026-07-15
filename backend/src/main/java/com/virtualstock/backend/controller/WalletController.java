package com.virtualstock.backend.controller;

import com.virtualstock.backend.entity.User;
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

    @GetMapping
    public ResponseEntity<Map<String, BigDecimal>> getWalletBalance(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("balance", user.getWalletBalance());
        return ResponseEntity.ok(response);
    }
}
