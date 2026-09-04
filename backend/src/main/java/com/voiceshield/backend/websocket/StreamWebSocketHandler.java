package com.voiceshield.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceshield.backend.service.RiskEvaluationService;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StreamWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StreamWebSocketHandler.class);
    private final RiskEvaluationService riskService;
    private final ObjectMapper objectMapper;
    private final Map<String, AtomicInteger> sessionChunkCounters = new ConcurrentHashMap<>();

    public StreamWebSocketHandler(RiskEvaluationService riskService, ObjectMapper objectMapper) {
        this.riskService = riskService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionChunkCounters.put(session.getId(), new AtomicInteger(0));
        log.info("WebSocket streaming session connected: {}", session.getId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "event", "CONNECTED",
                "session_id", session.getId(),
                "message", "VoiceShield real-time streaming channel open."
        ))));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer byteBuffer = message.getPayload();
        byte[] audioChunk = new byte[byteBuffer.remaining()];
        byteBuffer.get(audioChunk);

        int chunkIndex = sessionChunkCounters.getOrDefault(session.getId(), new AtomicInteger(0)).incrementAndGet();

        // Real-time incremental risk analysis
        double simulatedDeepfakeScore = (chunkIndex > 3 && audioChunk.length > 1024) ? 0.76 : 0.08;

        RiskEvaluationResult result = riskService.evaluateFromSignals(
                "STREAM-" + session.getId().substring(0, 6),
                simulatedDeepfakeScore,
                SpeakerVerificationStatus.MATCH,
                0.85,
                0.15,
                0.12,
                0.10,
                new ContextMetadata()
        );

        Map<String, Object> streamResponse = Map.of(
                "event", "CHUNK_EVALUATED",
                "chunk_index", chunkIndex,
                "chunk_bytes", audioChunk.length,
                "risk_score", result.getRiskScore(),
                "risk_level", result.getRiskLevel(),
                "decision", result.getDecision(),
                "reasons", result.getReasons()
        );

        sendSafeTextMessage(session, objectMapper.writeValueAsString(streamResponse));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received text payload in stream session {}: {}", session.getId(), payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "event", "ACK",
                "received_length", payload.length()
        ))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionChunkCounters.remove(session.getId());
        log.info("WebSocket streaming session closed: {}", session.getId());
    }

    private void sendSafeTextMessage(WebSocketSession session, String text) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(text));
            } catch (IOException e) {
                log.error("Failed to send WebSocket message: {}", e.getMessage());
            }
        }
    }
}
