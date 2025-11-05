package com.example.bankcards.exception;

public class UserNameNotFreeException extends RuntimeException {
    public UserNameNotFreeException(String message) {
        super(message);
    }
}
