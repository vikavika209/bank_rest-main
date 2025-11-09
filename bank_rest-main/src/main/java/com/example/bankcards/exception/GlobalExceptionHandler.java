package com.example.bankcards.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;

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

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> CardNotFoundExceptionHandler(Exception e){
        log.error("Обнаружено Card Not Found Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Карта не найдена",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(CardNumberIsNotFree.class)
    public ResponseEntity<ErrorResponseDto> CardNumberIsNotFreeHandler(Exception e){
        log.error("Обнаружено Card Number Is Not Free: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Номер карты не свободен",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    @ExceptionHandler(NotVerifyException.class)
    public ResponseEntity<ErrorResponseDto> NotVerifyExceptionHandler(Exception e){
        log.error("Обнаружено Not Verify Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Карта не принадлежит пользователю",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    @ExceptionHandler(TransferException.class)
    public ResponseEntity<ErrorResponseDto> TransferExceptionHandler(Exception e){
        log.error("Обнаружено Transfer Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Ошибка во время перевода денежных средств",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> UsernameNotFoundExceptionHandler(Exception e){
        log.error("Обнаружено Username Not Found Exception: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(
                "Пользователь не найден",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuth(AuthenticationException e){
        log.warn("Auth failure: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDto(
                        "Пользователь не аутентифицирован",
                        e.getMessage(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException e){
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDto(
                        "Недостаточно прав",
                        e.getMessage(),
                        LocalDateTime.now()
                ));
    }

}
