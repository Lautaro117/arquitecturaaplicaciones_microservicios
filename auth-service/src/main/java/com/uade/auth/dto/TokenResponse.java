package com.uade.auth.dto;

public record TokenResponse(String token, String type, long expiresIn) {}
