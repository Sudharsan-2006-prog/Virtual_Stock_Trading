package com.virtualstock.backend.controller;

import com.virtualstock.backend.dto.DashboardAnalyticsResponseDto;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.service.AnalyticsService;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<DashboardAnalyticsResponseDto> getAnalytics(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        
        DashboardAnalyticsResponseDto response = analyticsService.getDashboardAnalytics(user);
        return ResponseEntity.ok(response);
    }
}
