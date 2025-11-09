package com.example.bankcards.config;

import com.example.bankcards.service.CryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Value("${card.crypto.key}")
    private String key;

    @Value("${card.crypto.transformation:AES/ECB/PKCS5Padding}")
    private String transformation;

    @Bean
    public CryptoService cryptoService() {
        return new CryptoService(key, transformation);
    }

}
