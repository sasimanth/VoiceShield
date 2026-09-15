package com.voiceshield.backend.service;

import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakerVerificationServiceTest {

    @Mock
    private SpeakerProfileRepository speakerProfileRepository;

    @Mock
    private MlInferenceClient mlInferenceClient;

    private SpeakerVerificationService service;

    @BeforeEach
    void setUp() {
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

        double secondComponent =
                Math.sqrt(1.0 - targetSimilarity * targetSimilarity);

        double[] probe = new double[192];
        probe[0] = targetSimilarity;
        probe[1] = secondComponent;

        SpeakerProfile profile = new SpeakerProfile();
        profile.setSpeakerId(speakerId);
        profile.setAudioData(toBytes(reference));
        profile.setEmbeddingDimension(192);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(probe);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                expectedStatus,
                response.getStatus()
        );

        assertEquals(
                expectedMatch,
                response.isMatch()
        );

        assertEquals(
                0.4000,
                response.getThreshold(),
                1e-9
        );

        assertEquals(
                targetSimilarity,
                response.getSimilarityScore(),
                1e-5
        );
    }

    private static byte[] toBytes(double[] embedding) {

        ByteBuffer buffer =
                ByteBuffer
                        .allocate(192 * 4)
                        .order(ByteOrder.LITTLE_ENDIAN);

        for (double value : embedding) {
            buffer.putFloat((float) value);
        }

        return buffer.array();
    }
        @Test
    void nullEmbeddingShouldReturnUnavailable() {

        String speakerId = "TEST-INVALID";

        SpeakerProfile profile = createValidProfile(speakerId);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(null);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                SpeakerVerificationStatus.UNAVAILABLE,
                response.getStatus()
        );

        assertEquals(
                false,
                response.isMatch()
        );
    }

    @Test
    void wrongDimensionEmbeddingShouldReturnUnavailable() {

        String speakerId = "TEST-WRONG-DIMENSION";

        SpeakerProfile profile = createValidProfile(speakerId);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        double[] wrongDimension = new double[191];

        for (int i = 0; i < wrongDimension.length; i++) {
            wrongDimension[i] = 0.1;
        }

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(wrongDimension);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                SpeakerVerificationStatus.UNAVAILABLE,
                response.getStatus()
        );

        assertEquals(
                false,
                response.isMatch()
        );
    }

    @Test
    void nanEmbeddingShouldReturnUnavailable() {

        String speakerId = "TEST-NAN";

        SpeakerProfile profile = createValidProfile(speakerId);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        double[] invalidEmbedding = new double[192];
        invalidEmbedding[0] = Double.NaN;

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(invalidEmbedding);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                SpeakerVerificationStatus.UNAVAILABLE,
                response.getStatus()
        );

        assertEquals(
                false,
                response.isMatch()
        );
    }

    @Test
    void zeroNormEmbeddingShouldReturnUnavailable() {

        String speakerId = "TEST-ZERO-NORM";

        SpeakerProfile profile = createValidProfile(speakerId);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        double[] zeroEmbedding = new double[192];

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(zeroEmbedding);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                SpeakerVerificationStatus.UNAVAILABLE,
                response.getStatus()
        );

        assertEquals(
                false,
                response.isMatch()
        );
    }

    private SpeakerProfile createValidProfile(
            String speakerId
    ) {

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

        // Invalid stored embedding: wrong size.
        profile.setAudioData(new byte[100]);

        profile.setEmbeddingDimension(192);

        when(
                speakerProfileRepository.findBySpeakerId(speakerId)
        ).thenReturn(Optional.of(profile));

        double[] validProbe = new double[192];
        validProbe[0] = 1.0;

        when(
                mlInferenceClient.generateSpeakerEmbedding(
                        any(byte[].class),
                        anyString()
                )
        ).thenReturn(validProbe);

        SpeakerVerifyResponse response =
                service.verifySpeaker(
                        speakerId,
                        new byte[]{1, 2, 3}
                );

        assertEquals(
                SpeakerVerificationStatus.UNAVAILABLE,
                response.getStatus()
        );

        assertEquals(
                false,
                response.isMatch()
        );
    }
}