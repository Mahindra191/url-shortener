package com.dev.project.url_shortener.exception;

public class AliasConflictException extends RuntimeException {
    private final String existingShortUrl;

    public AliasConflictException(String message, String existingShortUrl) {
        super(message);
        this.existingShortUrl = existingShortUrl;
    }

    public String getExistingShortUrl() {
        return existingShortUrl;
    }
}