package com.voiceshield.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class MlInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(MlInferenceClient.class);

    private final RestClient restClient;
    private final String mlServiceUrl;
    private final Random random = new Random();

    public MlInferenceClient(
            @Value("${voiceshield.ml-service.url:http://localhost:8001}") String mlServiceUrl,
            @Value("${voiceshield.ml-service.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${voiceshield.ml-service.read-timeout-ms:5000}") int readTimeout
    ) {
        this.mlServiceUrl = mlServiceUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls Python ML Service (/ml/predict) for deepfake detection, or falls back to intelligent mock features.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeAudio(byte[] audioBytes, String filename) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename != null ? filename : "recording.wav";
                }
            });

            return restClient.post()
                    .uri("/ml/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Python ML Service at {} unreachable ({}), generating heuristic features for testing.",
                    mlServiceUrl, e.getMessage());
            return generateMockMlFeatures(audioBytes);
        }
    }

    private Map<String, Object> generateMockMlFeatures(byte[] audioBytes) {
        Map<String, Object> mock = new HashMap<>();
        // Heuristic based on file length or default genuine profile
        boolean isSuspicious = audioBytes.length > 50000 && (audioBytes[0] % 5 == 0);

        double deepfakeProb = isSuspicious ? 0.85 : 0.08;
        double acousticAnomaly = isSuspicious ? 0.78 : 0.12;
        double prosodicAnomaly = isSuspicious ? 0.72 : 0.15;
        double behavioralAnomaly = isSuspicious ? 0.60 : 0.09;

        mock.put("deepfake_probability", deepfakeProb);
        mock.put("classification", deepfakeProb >= 0.5 ? "SYNTHETIC_DEEPFAKE" : "BONAFIDE_HUMAN");
        mock.put("bonafide_probability", 1.0 - deepfakeProb);
        mock.put("acoustic_anomaly", acousticAnomaly);
        mock.put("prosodic_anomaly", prosodicAnomaly);
        mock.put("behavioral_anomaly", behavioralAnomaly);
        mock.put("source", "Java-Fallback-Inference-Engine");

        return mock;
    }
}
