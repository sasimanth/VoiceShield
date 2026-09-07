package com.voiceshield.backend.util;

import com.voiceshield.backend.exception.InvalidAudioException;
import java.util.Base64;

public class AudioValidationUtils {

    private static final long MAX_AUDIO_SIZE_BYTES = 10 * 1024 * 1024;

    public static byte[] validateAndDecodeBase64Audio(String base64Audio) {
        if (base64Audio == null || base64Audio.trim().isEmpty()) {
            throw new InvalidAudioException("Audio payload cannot be empty.");
        }

        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(base64Audio);
        } catch (IllegalArgumentException e) {
            throw new InvalidAudioException("Invalid Base64 audio encoding format.");
        }

        if (decodedBytes.length == 0) {
            throw new InvalidAudioException("Decoded audio payload contains 0 bytes.");
        }

        if (decodedBytes.length > MAX_AUDIO_SIZE_BYTES) {
            throw new InvalidAudioException("Audio payload exceeds maximum size limit of 10 MB.");
        }

        return decodedBytes;
    }
}
