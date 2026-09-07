package com.voiceshield.backend.client;

import com.voiceshield.backend.dto.MlInferenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MlInferenceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private MlInferenceClient mlInferenceClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("http://localhost:8001");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        mlInferenceClient = new MlInferenceClient(restClient);
    }

    @Test
    void testSuccessfulPythonMlResponseMapping() {
        String mockPythonJsonResponse = "{\"deepfake_probability\":0.87,\"acoustic_anomaly\":0.82,\"prosodic_anomaly\":0.79,\"behavioral_anomaly\":0.65,\"status\":\"SUCCESS\"}";

        mockServer.expect(requestTo("http://localhost:8001/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockPythonJsonResponse, MediaType.APPLICATION_JSON));

        byte[] sampleAudio = new byte[]{82, 73, 70, 70, 0, 0, 0, 0};
        MlInferenceResponse response = mlInferenceClient.analyzeAudio(sampleAudio, "test.wav");

        mockServer.verify();
        assertNotNull(response);
        assertEquals(0.87, response.getDeepfakeScore(), 0.001);
        assertEquals(0.82, response.getAcousticAnomaly(), 0.001);
        assertEquals(0.79, response.getProsodicAnomaly(), 0.001);
        assertEquals(0.65, response.getBehavioralAnomaly(), 0.001);
        assertEquals("OPTIMAL", response.getAnalysisStatus());
        assertEquals("HEALTHY", response.getMlServiceStatus());
    }

    @Test
    void testPythonMlServiceUnavailableFallback() {
        mockServer.expect(requestTo("http://localhost:8001/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        byte[] sampleAudio = new byte[]{82, 73, 70, 70, 0, 0, 0, 0};
        MlInferenceResponse response = mlInferenceClient.analyzeAudio(sampleAudio, "test.wav");

        assertNotNull(response);
        assertEquals("DEGRADED", response.getAnalysisStatus());
        assertEquals("UNAVAILABLE", response.getMlServiceStatus());
        assertEquals(0.0, response.getDeepfakeScore());
    }

    @Test
    void testEmptyAudioHandling() {
        MlInferenceResponse response = mlInferenceClient.analyzeAudio(new byte[0], "test.wav");

        assertNotNull(response);
        assertEquals("DEGRADED", response.getAnalysisStatus());
        assertEquals("UNAVAILABLE", response.getMlServiceStatus());
    }
}

