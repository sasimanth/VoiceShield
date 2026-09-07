package com.voiceshield.backend.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class VectorUtils {

    public static final int EXPECTED_DIMENSION = 192;
    public static final int EXPECTED_BYTE_LENGTH = EXPECTED_DIMENSION * 4; // 768 bytes

    private VectorUtils() {}

    public static double cosineSimilarity(double[] u, double[] v) {
        if (u == null || v == null) {
            throw new IllegalArgumentException("Input vectors cannot be null.");
        }

        if (u.length != EXPECTED_DIMENSION || v.length != EXPECTED_DIMENSION) {
            throw new IllegalArgumentException("Vector dimension mismatch: expected " 
                    + EXPECTED_DIMENSION + ", got u=" + u.length + ", v=" + v.length);
        }

        double dotProduct = 0.0;
        double normU = 0.0;
        double normV = 0.0;

        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            double ui = u[i];
            double vi = v[i];

            if (Double.isNaN(ui) || Double.isInfinite(ui) || Double.isNaN(vi) || Double.isInfinite(vi)) {
                throw new IllegalArgumentException("Vector contains non-finite values (NaN/Inf).");
            }

            dotProduct += ui * vi;
            normU += ui * ui;
            normV += vi * vi;
        }

        normU = Math.sqrt(normU);
        normV = Math.sqrt(normV);

        // Zero-norm defense: zero-norm vectors are invalid for cosine similarity (no manufactured score)
        if (normU < 1e-12 || normV < 1e-12) {
            throw new IllegalArgumentException("Cannot compute cosine similarity for zero-norm vector.");
        }

        // Return raw mathematical cosine similarity in [-1.0, +1.0]
        return dotProduct / (normU * normV);
    }

    public static byte[] floatArrayToBytes(float[] floats) {
        if (floats == null || floats.length != EXPECTED_DIMENSION) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(EXPECTED_BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    public static float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length != EXPECTED_BYTE_LENGTH) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[EXPECTED_DIMENSION];
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            float f = buffer.getFloat();
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                return null;
            }
            floats[i] = f;
        }
        return floats;
    }

    public static float[] doubleListToFloatArray(List<Double> list) {
        if (list == null || list.size() != EXPECTED_DIMENSION) {
            return null;
        }
        float[] floats = new float[EXPECTED_DIMENSION];
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            Double val = list.get(i);
            if (val == null || val.isNaN() || val.isInfinite()) {
                return null; // Reject entire vector if any element is null, NaN, or Infinite
            }
            floats[i] = val.floatValue();
        }
        return floats;
    }

    public static double[] floatArrayToDoubleArray(float[] floats) {
        if (floats == null || floats.length != EXPECTED_DIMENSION) {
            return null;
        }
        double[] doubles = new double[EXPECTED_DIMENSION];
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }
}
