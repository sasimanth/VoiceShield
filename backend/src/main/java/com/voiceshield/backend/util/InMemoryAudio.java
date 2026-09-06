package com.voiceshield.backend.util;

import java.util.Arrays;

public class InMemoryAudio implements AutoCloseable {

    private byte[] audioData;

    public InMemoryAudio(byte[] audioData) {
        if (audioData == null) {
            this.audioData = new byte[0];
        } else {
            this.audioData = audioData;
        }
    }

    public byte[] getAudioData() {
        return audioData;
    }

    /**
     * Wipes audio bytes from memory to prevent memory dump analysis.
     */
    public void wipe() {
        if (audioData != null) {
            Arrays.fill(audioData, (byte) 0);
        }
    }

    @Override
    public void close() {
        wipe();
    }
}