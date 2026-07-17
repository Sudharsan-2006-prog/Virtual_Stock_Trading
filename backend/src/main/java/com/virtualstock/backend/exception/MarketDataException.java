package com.virtualstock.backend.exception;

public class MarketDataException extends RuntimeException {
    public MarketDataException(String message) {
        super(message);
    }
}
