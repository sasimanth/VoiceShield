package com.voiceshield.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.voiceshield.backend.dto.MlInferenceResponse;
import com.voiceshield.backend.dto.MlSpeakerEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class MlInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(MlInferenceClient.class);
    private final RestClient mlRestClient;

    public MlInferenceClient(RestClient mlRestClient) {
        this.mlRestClient = mlRestClient;
    }

    public MlInferenceResponse analyzeAudio(byte[] audioBytes, String filename) {
        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Empty audio byte array passed to MlInferenceClient");
            return MlInferenceResponse.degraded();
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return (filename != null && !filename.trim().isEmpty()) ? filename : "audio.wav";
                }
            }, headers);

            body.add("file", fileEntity);

            PythonFeatureResponse pyResponse = mlRestClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonFeatureResponse.class);

            if (pyResponse != null && "SUCCESS".equalsIgnoreCase(pyResponse.getStatus())) {
                log.info("Received real ML inference response from Python service: deepfake_probability={}", pyResponse.getDeepfakeProbability());
                return new MlInferenceResponse(
                        pyResponse.getDeepfakeProbability(),
                        pyResponse.getAcousticAnomaly(),
                        pyResponse.getProsodicAnomaly(),
                        pyResponse.getBehavioralAnomaly(),
                        "OPTIMAL",
                        "HEALTHY"
                );
            } else {
                log.warn("Python ML service returned non-success status");
                return MlInferenceResponse.degraded();
            }

        } catch (Exception e) {
            log.error("ML Inference HTTP communication failed: {}", e.getMessage());
            return MlInferenceResponse.degraded();
        }
    }

    public MlSpeakerEmbeddingResponse extractSpeakerEmbedding(byte[] audioBytes, String filename) {
        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Empty audio byte array passed to extractSpeakerEmbedding");
            return null;
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return (filename != null && !filename.trim().isEmpty()) ? filename : "audio.wav";
                }
            }, headers);

            body.add("file", fileEntity);

            MlSpeakerEmbeddingResponse response = mlRestClient.post()
                    .uri("/speaker/embed")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlSpeakerEmbeddingResponse.class);

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getStatus())
                    && response.getEmbedding() != null && response.getEmbedding().size() == 192) {
                log.info("Successfully received 192-D speaker embedding from Python ML service");
                return response;
            } else {
                log.warn("Python ML service returned invalid or non-success speaker embedding response");
                return null;
            }

        } catch (Exception e) {
            log.error("Speaker embedding HTTP communication failed: {}", e.getMessage());
            return null;
        }
    }

    private static class PythonFeatureResponse {
        @JsonProperty("deepfake_probability")
        private double deepfakeProbability;

        @JsonProperty("acoustic_anomaly")
        private double acousticAnomaly;

        @JsonProperty("prosodic_anomaly")
        private double prosodicAnomaly;

        @JsonProperty("behavioral_anomaly")
        private double behavioralAnomaly;

        @JsonProperty("status")
        private String status;

        public double getDeepfakeProbability() { return deepfakeProbability; }
        public void setDeepfakeProbability(double deepfakeProbability) { this.deepfakeProbability = deepfakeProbability; }

        public double getAcousticAnomaly() { return acousticAnomaly; }
        public void setAcousticAnomaly(double acousticAnomaly) { this.acousticAnomaly = acousticAnomaly; }

        public double getProsodicAnomaly() { return prosodicAnomaly; }
        public void setProsodicAnomaly(double prosodicAnomaly) { this.prosodicAnomaly = prosodicAnomaly; }

        public double getBehavioralAnomaly() { return behavioralAnomaly; }
        public void setBehavioralAnomaly(double behavioralAnomaly) { this.behavioralAnomaly = behavioralAnomaly; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}