package com.example.bankcards.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class CryptoService {

    private final String transformation;
    private final SecretKeySpec keySpec;

    public CryptoService(String rawKey, String transformation) {
        this.transformation = transformation;
        if (rawKey == null) {
            throw new IllegalArgumentException("Crypto key is null");
        }

        String key = rawKey.trim();
        if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
            key = key.substring(1, key.length() - 1).trim();
        }

        byte[] keyBytes = deriveAesKeyBytes(key);

        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] deriveAesKeyBytes(String key) {
        byte[] keyBytes;

        if (key.regionMatches(true, 0, "base64:", 0, 7)) {
            keyBytes = Base64.getDecoder().decode(key.substring(7).trim());
        } else if (key.regionMatches(true, 0, "hex:", 0, 4)) {
            keyBytes = hexToBytes(key.substring(4).trim());
        } else if (key.regionMatches(true, 0, "plain:", 0, 6)) {
            keyBytes = key.substring(6).getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                keyBytes = Base64.getDecoder().decode(key);
            } catch (IllegalArgumentException e) {
                keyBytes = key.getBytes(StandardCharsets.UTF_8);
            }
        }

        int len = keyBytes.length;
        if (len == 16 || len == 24 || len == 32) return keyBytes;

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive AES key", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        String s = hex.replaceAll("\\s+", "");
        if (s.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex length");
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
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