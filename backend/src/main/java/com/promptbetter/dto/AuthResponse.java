package com.promptbetter.dto;

public record AuthResponse(String token, Long id, String name, String email) {
}
