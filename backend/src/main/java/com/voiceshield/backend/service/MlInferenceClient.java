package com.voiceshield.backend.service;

import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

@Service
public class MlInferenceClient {

    private static final Logger log =
            LoggerFactory.getLogger(MlInferenceClient.class);

    private final RestClient restClient;
    private final String mlServiceUrl;

    public MlInferenceClient(
            @Value("${voiceshield.ml-service.url:http://localhost:8001}")
            String mlServiceUrl,

            @Value("${voiceshield.ml-service.connect-timeout-ms:3000}")
            int connectTimeout,

            @Value("${voiceshield.ml-service.read-timeout-ms:5000}")
            int readTimeout
    ) {
        this.mlServiceUrl = mlServiceUrl;

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                Duration.ofMillis(connectTimeout)
        );

        requestFactory.setReadTimeout(
                Duration.ofMillis(readTimeout)
        );

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls Python ML Service (/ml/predict) for deepfake detection.
     *
     * NOTE:
     * The fallback is retained temporarily for development/testing.
     * It should not be treated as a production ML result.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeAudio(
            byte[] audioBytes,
            String filename
    ) {

        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Audio data is empty."
            );
        }

        try {
            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "file",
                    new ByteArrayResource(audioBytes) {
                        @Override
                        public String getFilename() {
                            return filename != null
                                    ? filename
                                    : "recording.wav";
                        }
                    }
            );

            byte[] responseBytes = restClient.post()
        .uri("/ml/predict")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(byte[].class);

if (responseBytes == null || responseBytes.length == 0) {
    throw new IllegalStateException(
            "ML service returned an empty response."
    );
}

Map<String, Object> response =
        new ObjectMapper().readValue(
                responseBytes,
                Map.class
        );

            if (response == null) {
                throw new IllegalStateException(
                        "ML service returned an empty response."
                );
            }

            return response;

        } catch (Exception e) {

            log.warn(
                    "Python ML Service at {} unreachable ({}). "
                            + "Using development fallback features.",
                    mlServiceUrl,
                    e.getMessage()
            );

            return generateMockMlFeatures(audioBytes);
        }
    }

    /**
     * Calls the Python FastAPI ECAPA endpoint:
     *
     * POST /speaker/embed
     *
     * Python generates the 192-D ECAPA embedding.
     * Java receives and validates the embedding.
     *
     * Java does NOT calculate cosine similarity here.
     * Java does NOT make the final verification decision here.
     */
    @SuppressWarnings("unchecked")
    public double[] generateSpeakerEmbedding(
            byte[] audioBytes,
            String filename
    ) {

        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Audio data is empty."
            );
        }

        try {
            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "file",
                    new ByteArrayResource(audioBytes) {
                        @Override
                        public String getFilename() {
                            return filename != null
                                    ? filename
                                    : "recording.wav";
                        }
                    }
            );
byte[] responseBytes = restClient.post()
        .uri("/speaker/embed")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(byte[].class);

if (responseBytes == null || responseBytes.length == 0) {
    throw new IllegalStateException(
            "ECAPA service returned an empty response."
    );
}

Map<String, Object> response =
        new ObjectMapper().readValue(
                responseBytes,
                Map.class
        );

            if (response == null) {
                throw new IllegalStateException(
                        "ECAPA service returned an empty response."
                );
            }

            /*
             * Verify that the Python service reports
             * the expected ECAPA embedding dimension.
             */
            Object dimensionObject =
                    response.get("embedding_dimension");

            if (!(dimensionObject instanceof Number)) {
                throw new IllegalStateException(
                        "ECAPA response is missing embedding_dimension."
                );
            }

            int dimension =
                    ((Number) dimensionObject).intValue();

            if (dimension != 192) {
                throw new IllegalStateException(
                        "Expected a 192-D ECAPA embedding, "
                                + "but received "
                                + dimension
                                + "-D."
                );
            }

            /*
             * Retrieve embedding array.
             */
            Object embeddingObject =
                    response.get("embedding");

            if (!(embeddingObject instanceof List<?> values)) {
                throw new IllegalStateException(
                        "ECAPA response does not contain "
                                + "a valid embedding array."
                );
            }

            /*
             * Verify exact dimension.
             */
            if (values.size() != 192) {
                throw new IllegalStateException(
                        "Expected 192 embedding values, "
                                + "but received "
                                + values.size()
                                + "."
                );
            }

            double[] embedding = new double[192];

            /*
             * Validate every embedding value.
             */
            for (int i = 0; i < values.size(); i++) {

                Object value = values.get(i);

                if (!(value instanceof Number)) {
                    throw new IllegalStateException(
                            "Embedding value at index "
                                    + i
                                    + " is not numeric."
                    );
                }

                double number =
                        ((Number) value).doubleValue();

                if (!Double.isFinite(number)) {
                    throw new IllegalStateException(
                            "Embedding value at index "
                                    + i
                                    + " is not finite."
                    );
                }

                embedding[i] = number;
            }

            return embedding;

        } catch (Exception e) {

            /*
             * Do NOT return a fake embedding.
             *
             * Invalid/unavailable ECAPA results must remain
             * invalid instead of being converted to a fabricated
             * speaker similarity score.
             */
            log.error(
                    "ECAPA speaker embedding generation failed: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Speaker embedding generation failed.",
                    e
            );
        }
    }

    /**
     * Temporary development fallback for deepfake testing.
     *
     * This is retained from the original implementation.
     * It must not be interpreted as production ML inference.
     */
    private Map<String, Object> generateMockMlFeatures(
            byte[] audioBytes
    ) {

        Map<String, Object> mock =
                new HashMap<>();

        if (audioBytes == null || audioBytes.length == 0) {
            mock.put("deepfake_probability", 0.0);
            mock.put("classification", "UNKNOWN");
            mock.put("bonafide_probability", 1.0);
            mock.put("acoustic_anomaly", 0.0);
            mock.put("prosodic_anomaly", 0.0);
            mock.put("behavioral_anomaly", 0.0);
            mock.put(
                    "source",
                    "Java-Fallback-Inference-Engine"
            );

            return mock;
        }

        /*
         * Development-only heuristic.
         */
        boolean isSuspicious =
                audioBytes.length > 50000
                        && (audioBytes[0] & 0xFF) % 5 == 0;

        double deepfakeProb =
                isSuspicious ? 0.85 : 0.08;

        double acousticAnomaly =
                isSuspicious ? 0.78 : 0.12;

        double prosodicAnomaly =
                isSuspicious ? 0.72 : 0.15;

        double behavioralAnomaly =
                isSuspicious ? 0.60 : 0.09;

        mock.put(
                "deepfake_probability",
                deepfakeProb
        );

        mock.put(
                "classification",
                deepfakeProb >= 0.5
                        ? "SYNTHETIC_DEEPFAKE"
                        : "BONAFIDE_HUMAN"
        );

        mock.put(
                "bonafide_probability",
                1.0 - deepfakeProb
        );

        mock.put(
                "acoustic_anomaly",
                acousticAnomaly
        );

        mock.put(
                "prosodic_anomaly",
                prosodicAnomaly
        );

        mock.put(
                "behavioral_anomaly",
                behavioralAnomaly
        );

        mock.put(
                "source",
                "Java-Fallback-Inference-Engine"
        );

        return mock;
    }
}