package com.jm.spring_web.application.security.model;

public record VerifyMfaCommand(String mfaChallengeToken, String code) {
}
