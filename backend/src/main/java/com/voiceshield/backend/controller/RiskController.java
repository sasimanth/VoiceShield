package com.voiceshield.backend.controller;

import com.voiceshield.backend.service.RiskEvaluationService;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskSignalInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
@Tag(name = "Risk & Decision Engine", description = "Multi-signal threat scoring and actionable decision classification")
public class RiskController {

    private final RiskEvaluationService riskService;

    public RiskController(RiskEvaluationService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Direct Risk Evaluation", description = "Evaluates composite risk from raw multi-signal input according to the frozen schema")
    public ResponseEntity<RiskEvaluationResult> evaluate(@RequestBody RiskSignalInput input) {
        RiskEvaluationResult result = riskService.evaluate(input);
        return ResponseEntity.ok(result);
    }
}
