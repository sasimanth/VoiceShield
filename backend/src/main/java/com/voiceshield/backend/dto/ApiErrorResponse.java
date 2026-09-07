package com.voiceshield.backend.dto;

import java.time.Instant;

public class ApiErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private String timestamp;

    public ApiErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ApiErrorResponse(boolean success, String error, String message) {
        this.success = success;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}