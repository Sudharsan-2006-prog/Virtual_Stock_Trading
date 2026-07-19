package com.virtualstock.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    @Column(precision = 15, scale = 2, columnDefinition = "NUMERIC(15,2) DEFAULT 100000.00")
    private BigDecimal walletBalance;

    @PostLoad
    protected void onLoad() {
        // Auto-initialize null wallet balances loaded from legacy DB rows
        if (walletBalance == null) {
            walletBalance = new BigDecimal("100000.00");
        }
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        if (walletBalance != null && walletBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }
        this.walletBalance = walletBalance;
    }
}