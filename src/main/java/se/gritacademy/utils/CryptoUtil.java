package se.gritacademy.utils;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class CryptoUtil {

    private static final int GCM_TAG_LENGTH = 128;

    public static String hashKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(key.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes).substring(0, 32);
    }

    public static String encryptMessage(String message, String secretKey) throws Exception {
        SecretKey key = createSecretKey(secretKey);
        Cipher cipher = initializeCipher(key);
        byte[] encryptedMessage = cipher.doFinal(message.getBytes());
        byte[] nonce = cipher.getIV();
        byte[] authTag = extractAuthTag(encryptedMessage);
        return combineEncryptedData(encryptedMessage, nonce, authTag);
    }

    public static String decryptMessage(String encryptedData, String secretKey) throws Exception {
        String[] parts = parseEncryptedData(encryptedData);
        byte[] encryptedBytes = Base64.getDecoder().decode(parts[0]);
        byte[] nonce = Base64.getDecoder().decode(parts[1]);
        byte[] authTagFromDatabase = decodeAuthTag(parts[2]);
        SecretKey key = createSecretKey(secretKey);
        Cipher cipher = initializeCipher(key, nonce);
        byte[] authTagFromDecryption = extractAuthTag(encryptedBytes);
        compareAuthTags(authTagFromDatabase, authTagFromDecryption);
        try {
            return new String(cipher.doFinal(encryptedBytes));
        } catch (AEADBadTagException e) {
            throw new SecurityException("Decryption failed, authentication tag mismatch or data tampering detected", e);
        }
    }

    /**
     * Creates a SecretKey from the provided string using the AES algorithm
     */
    private static SecretKey createSecretKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Initializes a Cipher object with the AES/GCM/NoPadding encryption algorithm
     */
    private static Cipher initializeCipher(SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    /**
     *Extracts the authentication tag from the end of the encrypted message
     */
    private static byte[] extractAuthTag(byte[] encryptedMessage) {
        byte[] authTag = new byte[GCM_TAG_LENGTH / 8];
        System.arraycopy(encryptedMessage, encryptedMessage.length - authTag.length, authTag, 0, authTag.length);
        return authTag;
    }

    /**
     * Combines the encrypted message, nonce, and authentication tag into a single string
     */
    private static String combineEncryptedData(byte[] encryptedMessage, byte[] nonce, byte[] authTag) {
        return Base64.getEncoder().encodeToString(encryptedMessage) + ":" +
                Base64.getEncoder().encodeToString(nonce) + ":" +
                Base64.getEncoder().encodeToString(authTag);
    }

    /**
     * Parses the encrypted data into its components: encrypted message, nonce, and auth tag
     */
    private static String[] parseEncryptedData(String encryptedData) {
        String[] parts = encryptedData.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Incorrect encrypted data format");
        }
        return parts;
    }

    /**
     * Decodes the authentication tag from the database (Base64)
     */
    private static byte[] decodeAuthTag(String authTagBase64) throws SecurityException {
        try {
            return Base64.getDecoder().decode(authTagBase64);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Decryption failed, invalid authentication tag format", e);
        }
    }

    /**
     * Initializes a Cipher object for decryption using the provided secret key and nonce
     */
    private static Cipher initializeCipher(SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher;
    }

    /**
     * Compares the authentication tag from the database and the one from decryption
     */
    private static void compareAuthTags(byte[] authTagFromDatabase, byte[] authTagFromDecryption) throws SecurityException {
        if (!Arrays.equals(authTagFromDatabase, authTagFromDecryption)) {
            throw new SecurityException("Authentication tag mismatch or data tampering detected");
        }
    }
}