package com.voiceshield.backend.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.voiceshield.backend.client.MlInferenceClient;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

@ExtendWith(MockitoExtension.class)
class SpeakerVerificationServiceTest {

    @Mock
    private SpeakerProfileRepository speakerProfileRepository;

    private TestableMlInferenceClient mlInferenceClient;
    private SpeakerVerificationService service;

    static class TestableMlInferenceClient extends MlInferenceClient {
        private double[] nextEmbedding;

        public void setNextEmbedding(double[] embedding) {
            this.nextEmbedding = embedding;
        }

        @Override
        public double[] generateSpeakerEmbedding(byte[] audioBytes, String filename) {
            return nextEmbedding;
        }
    }

    @BeforeEach
    void setUp() {
        mlInferenceClient = new TestableMlInferenceClient();
        service = new SpeakerVerificationService(
                speakerProfileRepository,
                mlInferenceClient
        );

        ReflectionTestUtils.setField(
                service,
                "similarityThreshold",
                0.4000
        );
    }

    @Test
    void similarityBelowThresholdShouldBeMismatch() {
        verifyThreshold(
                0.3999,
                SpeakerVerificationStatus.MISMATCH,
                false
        );
    }

    @Test
    void similarityAtThresholdShouldBeMatch() {
        verifyThreshold(
                0.4000,
                SpeakerVerificationStatus.MATCH,
                true
        );
    }

    @Test
    void similarityAboveThresholdShouldBeMatch() {
        verifyThreshold(
                0.4001,
                SpeakerVerificationStatus.MATCH,
                true
        );
    }

    private void verifyThreshold(
            double targetSimilarity,
            SpeakerVerificationStatus expectedStatus,
            boolean expectedMatch
    ) {
        String speakerId = "TEST-THRESHOLD";

        double[] reference = new double[192];
        reference[0] = 1.0;

        double secondComponent = Math.sqrt(1.0 - targetSimilarity * targetSimilarity);

        double[] probe = new double[192];
        probe[0] = targetSimilarity;
        probe[1] = secondComponent;

        SpeakerProfile profile = new SpeakerProfile();
        profile.setSpeakerId(speakerId);
        profile.setAudioData(toBytes(reference));
        profile.setEmbeddingDimension(192);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));
        mlInferenceClient.setNextEmbedding(probe);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMatch, response.isMatch());
        assertEquals(0.4000, response.getThreshold(), 1e-9);
        assertEquals(targetSimilarity, response.getSimilarityScore(), 1e-5);
    }

    private static byte[] toBytes(double[] embedding) {
        ByteBuffer buffer = ByteBuffer.allocate(192 * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : embedding) {
            buffer.putFloat((float) value);
        }
        return buffer.array();
    }

    @Test
    void nullEmbeddingShouldReturnUnavailable() {
        String speakerId = "TEST-INVALID";
        SpeakerProfile profile = createValidProfile(speakerId);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));
        mlInferenceClient.setNextEmbedding(null);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertEquals(false, response.isMatch());
    }

    @Test
    void wrongDimensionEmbeddingShouldReturnUnavailable() {
        String speakerId = "TEST-WRONG-DIMENSION";
        SpeakerProfile profile = createValidProfile(speakerId);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));

        double[] wrongDimension = new double[191];
        for (int i = 0; i < wrongDimension.length; i++) {
            wrongDimension[i] = 0.1;
        }
        mlInferenceClient.setNextEmbedding(wrongDimension);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertEquals(false, response.isMatch());
    }

    @Test
    void nanEmbeddingShouldReturnUnavailable() {
        String speakerId = "TEST-NAN";
        SpeakerProfile profile = createValidProfile(speakerId);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));

        double[] invalidEmbedding = new double[192];
        invalidEmbedding[0] = Double.NaN;
        mlInferenceClient.setNextEmbedding(invalidEmbedding);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertEquals(false, response.isMatch());
    }

    @Test
    void zeroNormEmbeddingShouldReturnUnavailable() {
        String speakerId = "TEST-ZERO-NORM";
        SpeakerProfile profile = createValidProfile(speakerId);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));

        double[] zeroEmbedding = new double[192];
        mlInferenceClient.setNextEmbedding(zeroEmbedding);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertEquals(false, response.isMatch());
    }

    private SpeakerProfile createValidProfile(String speakerId) {
        double[] reference = new double[192];
        reference[0] = 1.0;

        SpeakerProfile profile = new SpeakerProfile();
        profile.setSpeakerId(speakerId);
        profile.setAudioData(toBytes(reference));
        profile.setEmbeddingDimension(192);

        return profile;
    }

    @Test
    void invalidStoredEmbeddingShouldReturnUnavailable() {
        String speakerId = "TEST-STORED-INVALID";
        SpeakerProfile profile = new SpeakerProfile();
        profile.setSpeakerId(speakerId);
        profile.setAudioData(new byte[100]);
        profile.setEmbeddingDimension(192);

        when(speakerProfileRepository.findBySpeakerId(speakerId)).thenReturn(Optional.of(profile));

        double[] validProbe = new double[192];
        validProbe[0] = 1.0;
        mlInferenceClient.setNextEmbedding(validProbe);

        SpeakerVerifyResponse response = service.verifySpeaker(
                speakerId,
                new byte[]{1, 2, 3}
        );

        assertEquals(SpeakerVerificationStatus.UNAVAILABLE, response.getStatus());
        assertEquals(false, response.isMatch());
    }
}
