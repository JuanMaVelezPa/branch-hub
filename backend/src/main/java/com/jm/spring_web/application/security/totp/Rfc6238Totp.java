package com.jm.spring_web.application.security.totp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Objects;

public final class Rfc6238Totp {
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    private Rfc6238Totp() {
    }

    public static boolean isValid(byte[] secret, String code, int driftSteps) {
        Objects.requireNonNull(secret);
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim().replace(" ", "");
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        long step = System.currentTimeMillis() / 1000 / PERIOD_SECONDS;
        for (long i = step - driftSteps; i <= step + driftSteps; i++) {
            if (constantTimeEquals(formatHotp(secret, i), normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String formatHotp(byte[] secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
