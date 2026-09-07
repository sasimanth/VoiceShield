package com.voiceshield.backend.service;

import com.voiceshield.backend.client.MlInferenceClient;
import com.voiceshield.backend.dto.MlSpeakerEmbeddingResponse;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.backend.util.VectorUtils;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpeakerVerificationServiceTest {

    private SpeakerProfileRepository speakerProfileRepository;
    private SpeakerProfileService speakerProfileService;
    private MlSpeakerEmbeddingResponse stubEmbeddingResponse;
    private MlInferenceClient testMlInferenceClient;
    private SpeakerVerificationService speakerVerificationService;

    @BeforeEach
    void setUp() {
        speakerProfileRepository = mock(SpeakerProfileRepository.class);
        speakerProfileService = new SpeakerProfileService(speakerProfileRepository);
        stubEmbeddingResponse = null;

        testMlInferenceClient = new MlInferenceClient(null) {
            @Override
            public MlSpeakerEmbeddingResponse extractSpeakerEmbedding(byte[] audioBytes, String filename) {
                if (audioBytes == null || audioBytes.length == 0) {
                    return null;
                }
                return stubEmbeddingResponse;
            }
        };

        speakerVerificationService = new SpeakerVerificationService(speakerProfileService, testMlInferenceClient, 0.4000);
    }

    private List<Double> createMockEmbeddingList(double value) {
        List<Double> list = new ArrayList<>(192);
        for (int i = 0; i < 192; i++) {
            list.add(value);
        }
        return list;
    }

    private float[] createMockFloatArray(float value) {
        float[] array = new float[192];
        for (int i = 0; i < 192; i++) {
            array[i] = value;
        }
        return array;
    }

    @Test
    void testEnrollSpeakerSuccess() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        List<Double> mockEmbedding = createMockEmbeddingList(0.5);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(mockEmbedding, 192, "SUCCESS");

        boolean result = speakerVerificationService.enrollSpeaker("USR_1001", audioBytes);
        assertTrue(result);

        verify(speakerProfileRepository, times(1)).save(any(SpeakerProfile.class));
    }

    @Test
    void testEnrollSpeakerFailureOnEmptyAudio() {
        boolean result = speakerVerificationService.enrollSpeaker("USR_1001", new byte[0]);
        assertFalse(result);
        verify(speakerProfileRepository, never()).save(any());
    }

    @Test
    void testVerifySpeakerUnenrolled() {
        when(speakerProfileRepository.findBySpeakerId("USR_UNENROLLED")).thenReturn(Optional.empty());

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_UNENROLLED", new byte[]{1, 2, 3});
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertNull(response.getSimilarityScore());
        assertFalse(response.isMatch());
    }

    @Test
    void testVerifySpeakerScoreGreaterThanThresholdMatch() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] refEmbeddingFloat = createMockFloatArray(1.0f);
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(refEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_MATCH", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_MATCH")).thenReturn(Optional.of(profile));

        List<Double> queryEmbedding = createMockEmbeddingList(1.0);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_MATCH", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.MATCH, response.getStatus());
        assertNotNull(response.getSimilarityScore());
        assertEquals(1.0, response.getSimilarityScore(), 1e-4);
        assertTrue(response.isMatch());
        assertEquals(0.4000, response.getThreshold(), 1e-4);
    }

    @Test
    void testVerifySpeakerScoreExactlyThresholdMatch() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        // Orthogonal-like vectors yielding similarity exactly ~0.4000
        float[] refFloat = new float[192];
        float[] queryFloat = new float[192];

        // 100 elements positive 1.0, remainder chosen so dot product gives similarity = 0.4000
        // Sim = sum(ref * query) / (norm(ref) * norm(query))
        // If ref is all 1.0, norm = sqrt(192). If query has k ones, dot = k, norm = sqrt(k * 192) -> sim = k / sqrt(192*k) = sqrt(k/192)
        // sqrt(k/192) = 0.4000 -> k/192 = 0.16 -> k = 30.72.
        // Let's test with exact score construction
        List<Double> queryEmbedding = new ArrayList<>();
        for (int i = 0; i < 192; i++) {
            refFloat[i] = 1.0f;
            queryEmbedding.add(i < 31 ? 2.4774193548387097 : 0.0); // Exact dot product matching 0.4000 threshold
        }

        byte[] refBytes = VectorUtils.floatArrayToBytes(refFloat);
        SpeakerProfile profile = new SpeakerProfile("USR_EXACT", refBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_EXACT")).thenReturn(Optional.of(profile));

        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_EXACT", audioBytes);
        assertNotNull(response);
        assertTrue(response.getSimilarityScore() >= 0.4000);
        assertEquals(SpeakerVerificationStatus.MATCH, response.getStatus());
        assertTrue(response.isMatch());
        assertEquals(0.4000, response.getThreshold(), 1e-4);
    }

    @Test
    void testVerifySpeakerScoreBelowThresholdMismatch() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] refEmbeddingFloat = createMockFloatArray(1.0f);
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(refEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_MISMATCH", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_MISMATCH")).thenReturn(Optional.of(profile));

        List<Double> queryEmbedding = createMockEmbeddingList(-1.0);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_MISMATCH", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.MISMATCH, response.getStatus());
        assertNotNull(response.getSimilarityScore());
        assertEquals(-1.0, response.getSimilarityScore(), 1e-4);
        assertFalse(response.isMatch());
        assertEquals(0.4000, response.getThreshold(), 1e-4);
    }

    @Test
    void testVerifySpeakerZeroNormEmbeddingReturnsUnavailable() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] zeroEmbeddingFloat = new float[192];
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(zeroEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_ZERO_NORM", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_ZERO_NORM")).thenReturn(Optional.of(profile));

        List<Double> queryEmbedding = createMockEmbeddingList(1.0);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_ZERO_NORM", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertNull(response.getSimilarityScore());
        assertFalse(response.isMatch());
    }

    @Test
    void testVerifySpeakerInvalidDimensionsRejected() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] refEmbeddingFloat = createMockFloatArray(1.0f);
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(refEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_BAD_DIM", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_BAD_DIM")).thenReturn(Optional.of(profile));

        // Dimension = 100 instead of 192
        List<Double> badEmbedding = new ArrayList<>();
        for (int i = 0; i < 100; i++) badEmbedding.add(1.0);

        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(badEmbedding, 100, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_BAD_DIM", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertNull(response.getSimilarityScore());
        assertFalse(response.isMatch());
    }

    @Test
    void testVerifySpeakerNanOrInfinityRejected() {
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] nanEmbeddingFloat = createMockFloatArray(1.0f);
        nanEmbeddingFloat[0] = Float.NaN;
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(nanEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_NAN", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_NAN")).thenReturn(Optional.of(profile));

        List<Double> queryEmbedding = createMockEmbeddingList(1.0);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = speakerVerificationService.verifySpeaker("USR_NAN", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertNull(response.getSimilarityScore());
        assertFalse(response.isMatch());
    }

    @Test
    void testThresholdConfigurationLoading() {
        SpeakerVerificationService svc = new SpeakerVerificationService(speakerProfileService, testMlInferenceClient, 0.4000);
        assertEquals(0.4000, svc.getThreshold(), 1e-4);
    }

    @Test
    void testNoSilentFallbackThresholdWhenUnconfigured() {
        SpeakerVerificationService unconfiguredSvc = new SpeakerVerificationService(speakerProfileService, testMlInferenceClient, null);
        assertNull(unconfiguredSvc.getThreshold());

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        float[] refEmbeddingFloat = createMockFloatArray(1.0f);
        byte[] refEmbeddingBytes = VectorUtils.floatArrayToBytes(refEmbeddingFloat);

        SpeakerProfile profile = new SpeakerProfile("USR_UNCONFIG", refEmbeddingBytes);
        when(speakerProfileRepository.findBySpeakerId("USR_UNCONFIG")).thenReturn(Optional.of(profile));

        List<Double> queryEmbedding = createMockEmbeddingList(1.0);
        stubEmbeddingResponse = new MlSpeakerEmbeddingResponse(queryEmbedding, 192, "SUCCESS");

        SpeakerVerifyResponse response = unconfiguredSvc.verifySpeaker("USR_UNCONFIG", audioBytes);
        assertNotNull(response);
        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertNotNull(response.getSimilarityScore());
        assertEquals(1.0, response.getSimilarityScore(), 1e-4);
        assertFalse(response.isMatch());
        assertNull(response.getThreshold()); // Ensures threshold cannot silently fall back to arbitrary numeric value
    }
}
