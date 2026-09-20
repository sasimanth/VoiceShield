package com.voiceshield.backend;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.backend.service.MlInferenceClient;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BackendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SpeakerProfileRepository speakerProfileRepository;

    @MockBean
    private MlInferenceClient mlInferenceClient;

    @BeforeEach
    void setUp() {
        double[] mockEmbedding = new double[192];
        for (int i = 0; i < 192; i++) mockEmbedding[i] = 0.1;

        ByteBuffer buffer = ByteBuffer.allocate(192 * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (double val : mockEmbedding) buffer.putFloat((float) val);
        byte[] dummyBytes = buffer.array();

        SpeakerProfile profile = new SpeakerProfile();
        profile.setSpeakerId("USER-999");
        profile.setAudioData(dummyBytes);
        profile.setEmbeddingDimension(192);

        Mockito.lenient().when(speakerProfileRepository.save(any(SpeakerProfile.class))).thenReturn(profile);
        Mockito.lenient().when(speakerProfileRepository.findBySpeakerId(anyString())).thenReturn(Optional.of(profile));
        Mockito.lenient().when(mlInferenceClient.generateSpeakerEmbedding(any(byte[].class), anyString())).thenReturn(mockEmbedding);
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("VoiceShield-Java-Backend"));
    }

    @Test
    void testDirectRiskEvaluationContract() throws Exception {
        ContextMetadata context = new ContextMetadata();
        context.setTransactionAmount(500000.0);
        context.setCurrency("INR");
        context.setNewBeneficiary(true);
        context.setInternationalCall(true);
        context.setUrgency(true);
        context.setCallerTrustScore(0.8);

        RiskSignalInput input = new RiskSignalInput();
        input.setSessionId("test-sess-001");
        input.setCallerId("+91-9876543210");
        input.setTimestamp(Instant.now().toString());
        input.setNonce(UUID.randomUUID().toString());
        input.setDeepfakeProbability(0.87);
        input.setSpeakerVerificationStatus(SpeakerVerificationStatus.MISMATCH);
        input.setSpeakerSimilarity(0.25);
        input.setAcousticAnomaly(0.82);
        input.setProsodicAnomaly(0.79);
        input.setBehavioralAnomaly(0.65);
        input.setContext(context);

        mockMvc.perform(post("/api/v1/risk/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.risk_score").isNumber())
                .andExpect(jsonPath("$.risk_level").value("CRITICAL"))
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.model_version").value("v1.2.0-composite-risk"));
    }

    @Test
    void testAudioAnalyzeMultipartEndpoint() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile("file", "sample_voice.wav", "audio/wav", new byte[]{82, 73, 70, 70, 0, 0, 0, 0});
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/audio/analyze")
                .file(audioFile)
                .param("caller_id", "+91-9876543210")
                .param("transaction_amount", "25000.00")
                .param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.risk_score").isNumber())
                .andExpect(jsonPath("$.decision").isNotEmpty());
    }

    @Test
    void testSpeakerEnrollAndVerifyJson() throws Exception {
        String audioBase64 = Base64.getEncoder().encodeToString(new byte[]{82, 73, 70, 70, 0, 0, 0, 0});

        mockMvc.perform(post("/api/v1/speaker/enroll")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.speaker_id").value("USER-999"));

        mockMvc.perform(post("/api/v1/speaker/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimed_speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCH"))
                .andExpect(jsonPath("$.similarity_score").value(1.0))
                .andExpect(jsonPath("$.threshold").value(0.4))
                .andExpect(jsonPath("$.is_match").value(true));
    }
}