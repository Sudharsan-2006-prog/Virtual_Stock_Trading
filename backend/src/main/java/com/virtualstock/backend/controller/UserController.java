package com.virtualstock.backend.controller;

import com.virtualstock.backend.dto.RegisterRequest;
import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {

        return userService.registerUser(request);

    }
}