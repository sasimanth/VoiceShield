package com.voiceshield.backend.exception;

public class InvalidAudioException extends RuntimeException {
    public InvalidAudioException(String message) {
        super(message);
    }
}
