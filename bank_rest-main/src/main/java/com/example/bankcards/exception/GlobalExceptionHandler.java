package com.example.bankcards.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> exceptionHandler(Exception e){
        log.error("Exception has been caught: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Internal server error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    @ExceptionHandler(UserNameNotFreeException.class)
    public ResponseEntity<ErrorResponseDto> UserNameNotFreeExceptionHandler(Exception e){
        log.error("Обнаружено User Name Not Free Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Имя пользователя занято",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    @ExceptionHandler(UserNotFoundCustomException.class)
    public ResponseEntity<ErrorResponseDto> UserNotFoundCustomExceptionHandler(Exception e){
        log.error("Обнаружено User Not Found Custom Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Пользователь не найден",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(PasswordIsShortException.class)
    public ResponseEntity<ErrorResponseDto> PasswordIsShortExceptionHandler(Exception e){
        log.error("Обнаружено Password Is Short Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Пароль слишком короткий",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }
}
