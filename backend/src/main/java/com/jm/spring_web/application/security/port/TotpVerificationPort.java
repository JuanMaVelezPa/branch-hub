package com.jm.spring_web.application.security.port;

public interface TotpVerificationPort {

    boolean verify(byte[] secret, String code);
}
