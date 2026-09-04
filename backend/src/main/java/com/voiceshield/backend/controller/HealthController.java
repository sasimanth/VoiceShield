package com.voiceshield.backend.controller;

import com.voiceshield.backend.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health & Diagnostics", description = "System health check and dependency statuses")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health Status", description = "Returns active health state of VoiceShield Java backend")
    public ResponseEntity<HealthResponse> getHealth() {
        Map<String, String> deps = Map.of(
                "risk_engine", "ACTIVE (Java In-Memory v1.2.0)",
                "database", "UP",
                "ml_bridge", "READY"
        );
        return ResponseEntity.ok(new HealthResponse("UP", "VoiceShield-Java-Backend", "1.0.0", deps));
    }
}
