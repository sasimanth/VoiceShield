package com.voiceshield.backend;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceshield.backend.client.MlInferenceClient;
import com.voiceshield.backend.dto.MlSpeakerEmbeddingResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.springframework.web.client.RestClient;

import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
   private RestClient mlRestClient;

   @BeforeEach
   void setUp() {
      float[] floatVec = new float[192];
      for (int i = 0; i < 192; i++) floatVec[i] = 0.1f;
      byte[] dummyVectorBytes = com.voiceshield.backend.util.VectorUtils.floatArrayToBytes(floatVec);

      SpeakerProfile profile = new SpeakerProfile("USER-999", dummyVectorBytes);

      Mockito.when(speakerProfileRepository.save(any(SpeakerProfile.class))).thenReturn(profile);
      when(speakerProfileRepository.findBySpeakerId(anyString()))
          .thenReturn(Optional.of(profile));

      List<Double> mockEmbedding = Collections.nCopies(192, 0.1);
      MlSpeakerEmbeddingResponse mockEmbeddingResponse = new MlSpeakerEmbeddingResponse(mockEmbedding, 192, "SUCCESS");

      RestClient.RequestBodyUriSpec uriSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
      RestClient.RequestBodySpec bodySpec = Mockito.mock(RestClient.RequestBodySpec.class);
      RestClient.ResponseSpec responseSpec = Mockito.mock(RestClient.ResponseSpec.class);

      when(mlRestClient.post()).thenReturn(uriSpec);
      when(uriSpec.uri(anyString())).thenReturn(bodySpec);
      when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
      when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
      when(bodySpec.retrieve()).thenReturn(responseSpec);
      when(responseSpec.body(MlSpeakerEmbeddingResponse.class)).thenReturn(mockEmbeddingResponse);
   }

   @Test
   void testHealthEndpoint() throws Exception {
      this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/health"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.service").value("VoiceShield-Java-Backend"));
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

      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/risk/evaluate")
              .contentType(MediaType.APPLICATION_JSON)
              .content(this.objectMapper.writeValueAsString(input)))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_score").isNumber())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_level").value("CRITICAL"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.decision").value("BLOCK"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.model_version").value("v1.2.0-composite-risk"));
   }

   @Test
   void testAudioAnalyzeMultipartEndpoint() throws Exception {
      MockMultipartFile audioFile = new MockMultipartFile("file", "sample_voice.wav", "audio/wav", new byte[]{82, 73, 70, 70, 0, 0, 0, 0});
      this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/audio/analyze")
              .file(audioFile)
              .param("caller_id", "+91-9876543210")
              .param("transaction_amount", "25000.00")
              .param("currency", "INR"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.risk_score").isNumber())
              .andExpect(MockMvcResultMatchers.jsonPath("$.decision").isNotEmpty());
   }

   @Test
   void testSpeakerEnrollAndVerify() throws Exception {
      String audioBase64 = Base64.getEncoder().encodeToString(new byte[]{82, 73, 70, 70, 0, 0, 0, 0});

      // ENROLL
      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/speaker/enroll")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
              .andExpect(MockMvcResultMatchers.jsonPath("$.speaker_id").value("USER-999"));

      // VERIFY (Phase 6C-4 active threshold 0.4000: matching vectors return status MATCH, similarity_score=1.0, is_match=true, threshold=0.4)
      this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/speaker/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"claimed_speaker_id\":\"USER-999\",\"audio_base64\":\"" + audioBase64 + "\"}"))
              .andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("MATCH"))
              .andExpect(MockMvcResultMatchers.jsonPath("$.similarity_score").value(1.0))
              .andExpect(MockMvcResultMatchers.jsonPath("$.threshold").value(0.4))
              .andExpect(MockMvcResultMatchers.jsonPath("$.is_match").value(true));
   }
}