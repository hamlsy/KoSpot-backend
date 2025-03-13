package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.exception.object.domain.GameHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class AESService {

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    private static final String ALGORITHM = "AES";
    private static final String ALGORITHM_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    // Encryption (server → client)
    public String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(aesSecretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new GameHandler(ErrorStatus.GAME_COORDINATES_ENCRYPT_ERROR);
        }
    }

    public <T> String toEncryptString(T object) {
        return encrypt(String.valueOf(object));
    }
}