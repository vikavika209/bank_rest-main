package com.example.bankcards.exception;

public class CardNumberIsNotFree extends RuntimeException {
    public CardNumberIsNotFree(String message) {
        super(message);
    }
}
