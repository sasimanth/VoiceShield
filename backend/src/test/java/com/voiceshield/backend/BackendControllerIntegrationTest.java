// Source code updated for VoiceShield Backend Integration Tests
package com.voiceshield.backend;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

@SpringBootTest
@AutoConfigureMockMvc
public class BackendControllerIntegrationTest {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   public BackendControllerIntegrationTest() {
   }

   @Test
   void testHealthEndpoint() throws Exception {
      this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/health", new Object[0]))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.status", new Object[0]).value("UP"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.service", new Object[0]).value("VoiceShield-Java-Backend"));
   }

   @Test
   void testDirectRiskEvaluationContract() throws Exception {
      ContextMetadata context = new ContextMetadata();
      context.setTransactionAmount((double)500000.0F);
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
      input.setSpeakerSimilarity((double)0.25F);
      input.setAcousticAnomaly(0.82);
      input.setProsodicAnomaly(0.79);
      input.setBehavioralAnomaly(0.65);
      input.setContext(context);

      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/risk/evaluate", new Object[0])
              .contentType(MediaType.APPLICATION_JSON)
              .content(this.objectMapper.writeValueAsString(input)))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_score", new Object[0]).isNumber())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_level", new Object[0]).value("CRITICAL"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.decision", new Object[0]).value("BLOCK"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.model_version", new Object[0]).value("v1.2.0-composite-risk"));
   }

   @Test
   void testAudioAnalyzeMultipartEndpoint() throws Exception {
      MockMultipartFile audioFile = new MockMultipartFile("file", "sample_voice.wav", "audio/wav", new byte[]{82, 73, 70, 70, 0, 0, 0, 0});
      this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/audio/analyze", new Object[0])
              .file(audioFile)
              .param("caller_id", new String[]{"+91-9876543210"})
              .param("transaction_amount", new String[]{"25000.00"})
              .param("currency", new String[]{"INR"}))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_score", new Object[0]).isNumber())
              .andExpect(MockMvcResultMatchers.jsonPath("$.decision", new Object[0]).isNotEmpty());
   }

   @Test
   void testSpeakerEnrollAndVerify() throws Exception {
      String audioBase64 = Base64.getEncoder().encodeToString(new byte[]{82, 73, 70, 70, 0, 0, 0, 0});

      // ENROLL
      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/speaker/enroll", new Object[0])
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.success", new Object[0]).value(true))
              .andExpect(MockMvcResultMatchers.jsonPath("$.speaker_id", new Object[0]).value("USER-999"));

      // VERIFY
      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/speaker/verify", new Object[0])
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"claimed_speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.status", new Object[0]).value("MATCH"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.is_match", new Object[0]).value(true))
              .andExpect(MockMvcResultMatchers.jsonPath("$.similarity_score", new Object[0]).value(0.85))
              .andExpect(MockMvcResultMatchers.jsonPath("$.threshold", new Object[0]).value(0.75));
   }
}