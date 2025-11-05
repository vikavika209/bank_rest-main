package com.example.bankcards.exception;

public class PasswordIsShortException extends RuntimeException {
    public PasswordIsShortException(String message) {
        super(message);
    }
}
