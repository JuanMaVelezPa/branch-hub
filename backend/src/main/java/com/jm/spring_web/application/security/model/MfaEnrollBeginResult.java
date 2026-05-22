package com.jm.spring_web.application.security.model;

public record MfaEnrollBeginResult(String otpauthUri, String secretBase32) {
}
