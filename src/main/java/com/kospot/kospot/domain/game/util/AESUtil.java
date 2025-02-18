package com.kospot.kospot.domain.game.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {

    //todo refactoring
    private static String AES_SECRET_KEY = "0123456789abcdef";
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    // Encryption (server → client)
    public static String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(AES_SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}