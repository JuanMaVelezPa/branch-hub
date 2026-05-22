package com.jm.spring_web.infrastructure.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class MfaSecretCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaSecretCipher(MfaProperties properties) {
        Objects.requireNonNull(properties);
        String b64 = properties.encryptionKeyBase64();
        if (b64 == null || b64.isBlank()) {
            this.aesKey = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(b64.trim());
        if (raw.length != 32) {
            throw new IllegalArgumentException("security.mfa.encryption-key-base64 must decode to 32 bytes (AES-256)");
        }
        this.aesKey = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(byte[] plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ciphertext.length);
            buf.put(iv);
            buf.put(ciphertext);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt MFA secret", e);
        }
    }

    public byte[] decrypt(String encoded) {
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            ByteBuffer buf = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt MFA secret", e);
        }
    }

    private void requireKey() {
        if (aesKey == null) {
            throw new IllegalStateException(
                    "MFA is not configured: set security.mfa.encryption-key-base64 (32-byte key, Base64)");
        }
    }
}
