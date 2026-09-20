package com.voiceshield.backend.dto;

import java.time.Instant;

public class ApiErrorResponse {

    private final String error;
    private final String message;
    private final String timestamp;

    public ApiErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}