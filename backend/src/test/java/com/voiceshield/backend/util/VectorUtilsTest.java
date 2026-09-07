package com.voiceshield.backend.util;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @Test
    void testCosineSimilarityIdenticalVectors() {
        double[] u = new double[192];
        for (int i = 0; i < 192; i++) {
            u[i] = 1.0;
        }

        double similarity = VectorUtils.cosineSimilarity(u, u);
        assertEquals(1.0, similarity, 1e-6);
    }

    @Test
    void testCosineSimilarityOppositeVectors() {
        double[] u = new double[192];
        double[] v = new double[192];
        for (int i = 0; i < 192; i++) {
            u[i] = 1.0;
            v[i] = -1.0;
        }

        double similarity = VectorUtils.cosineSimilarity(u, v);
        assertEquals(-1.0, similarity, 1e-6);
    }

    @Test
    void testCosineSimilarityOrthogonalVectors() {
        double[] u = new double[192];
        double[] v = new double[192];
        for (int i = 0; i < 96; i++) {
            u[i] = 1.0;
        }
        for (int i = 96; i < 192; i++) {
            v[i] = 1.0;
        }

        double similarity = VectorUtils.cosineSimilarity(u, v);
        assertEquals(0.0, similarity, 1e-6);
    }

    @Test
    void testCosineSimilarityDimensionMismatch() {
        double[] u = new double[192];
        double[] v = new double[128];
        assertThrows(IllegalArgumentException.class, () -> VectorUtils.cosineSimilarity(u, v));
    }

    @Test
    void testCosineSimilarityZeroNormVector() {
        double[] u = new double[192];
        double[] v = new double[192];
        for (int i = 0; i < 192; i++) {
            v[i] = 1.0;
        }

        assertThrows(IllegalArgumentException.class, () -> VectorUtils.cosineSimilarity(u, v));
    }

    @Test
    void testFloatArrayToBytesAndBack() {
        float[] original = new float[192];
        for (int i = 0; i < 192; i++) {
            original[i] = i * 0.123f;
        }

        byte[] bytes = VectorUtils.floatArrayToBytes(original);
        assertNotNull(bytes);
        assertEquals(768, bytes.length);

        float[] reconstructed = VectorUtils.bytesToFloatArray(bytes);
        assertNotNull(reconstructed);
        assertEquals(192, reconstructed.length);

        for (int i = 0; i < 192; i++) {
            assertEquals(original[i], reconstructed[i], 1e-5f);
        }
    }

    @Test
    void testDoubleListToFloatArray() {
        List<Double> doubleList = new ArrayList<>();
        for (int i = 0; i < 192; i++) {
            doubleList.add(i * 0.5);
        }

        float[] floats = VectorUtils.doubleListToFloatArray(doubleList);
        assertNotNull(floats);
        assertEquals(192, floats.length);
        assertEquals(0.0f, floats[0], 1e-5f);
        assertEquals(0.5f, floats[1], 1e-5f);
    }

    @Test
    void testDoubleListToFloatArrayRejectsInvalidValues() {
        // Null element check
        List<Double> nullList = new ArrayList<>();
        for (int i = 0; i < 191; i++) nullList.add(0.1);
        nullList.add(null);
        assertNull(VectorUtils.doubleListToFloatArray(nullList));

        // NaN element check
        List<Double> nanList = new ArrayList<>();
        for (int i = 0; i < 191; i++) nanList.add(0.1);
        nanList.add(Double.NaN);
        assertNull(VectorUtils.doubleListToFloatArray(nanList));

        // Positive Infinity element check
        List<Double> posInfList = new ArrayList<>();
        for (int i = 0; i < 191; i++) posInfList.add(0.1);
        posInfList.add(Double.POSITIVE_INFINITY);
        assertNull(VectorUtils.doubleListToFloatArray(posInfList));

        // Negative Infinity element check
        List<Double> negInfList = new ArrayList<>();
        for (int i = 0; i < 191; i++) negInfList.add(0.1);
        negInfList.add(Double.NEGATIVE_INFINITY);
        assertNull(VectorUtils.doubleListToFloatArray(negInfList));

        // Incorrect dimension check (e.g. 191 elements)
        List<Double> shortList = new ArrayList<>();
        for (int i = 0; i < 191; i++) shortList.add(0.1);
        assertNull(VectorUtils.doubleListToFloatArray(shortList));
    }
}
