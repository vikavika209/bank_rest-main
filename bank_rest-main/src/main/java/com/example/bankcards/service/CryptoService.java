package com.example.bankcards.service;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoService {

    private final String transformation;
    private final SecretKeySpec keySpec;

    public CryptoService(String key, String transformation) {
        this.transformation = transformation;
        this.keySpec = new SecretKeySpec(key.getBytes(), "AES");
    }

    public String encrypt(String plain) {
        try {
            Cipher c = Cipher.getInstance(transformation);
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String enc) {
        try {
            Cipher c = Cipher.getInstance(transformation);
            c.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(c.doFinal(Base64.getDecoder().decode(enc)));
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    public String getMaskedNumber(String enc) {
        String decryptedNumber = decrypt(enc);

        if (decryptedNumber == null || decryptedNumber.length() < 4) {
            return "****";
        }
        String last4 = decryptedNumber.substring(decryptedNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}