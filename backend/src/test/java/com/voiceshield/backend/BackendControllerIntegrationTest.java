package com.voiceshield.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class BackendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        context.setTransactionAmount(500000.00);
        context.setCurrency("INR");
        context.setNewBeneficiary(true);
        context.setInternationalCall(true);
        context.setUrgency(true);
        context.setCallerTrustScore(0.80);

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
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "sample_voice.wav",
                "audio/wav",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00}
        );

        mockMvc.perform(multipart("/api/v1/audio/analyze")
                        .file(audioFile)
                        .param("caller_id", "+91-9876543210")
                        .param("transaction_amount", "25000.00")
                        .param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.risk_score").isNumber())
                .andExpect(jsonPath("$.decision").isNotEmpty());
    }

    @Test
    void testSpeakerEnrollAndVerify() throws Exception {
        mockMvc.perform(post("/api/v1/speaker/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"speaker_id\":\"USER-999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/speaker/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claimed_speaker_id\":\"USER-999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.is_match").value(true));
    }
}
