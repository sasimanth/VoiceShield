# VoiceShield — Risk & Context Evaluation Engine
**Module**: `risk_engine/`  
**Owner**: Person 4 (Risk & Cybersecurity Engineer)  
**Branch**: `member-sasimanth`  
**Target Consumer**: Person 3 (Backend Engineer — FastAPI / PostgreSQL / Service Orchestration)  

---

## Overview
The **Risk & Context Evaluation Engine** computes composite, explainable impersonation risk scores (0–100) and actionable policy decisions (`ALLOW`, `WARN`, `SECONDARY_VERIFICATION`, `BLOCK`).

### Key Capabilities
1. **Decoupled Context Engine**: Standalone evaluation of transaction amounts (e.g. ₹5,00,000 threshold), social engineering urgency, foreign VoIP origins, and beneficiary novelty.
2. **Multi-Signal Risk Fusion**: Weights AASIST deepfake probabilities, acoustic anomalies, prosodic cadence, speaker biometric consistency, and contextual risk.
3. **Graceful Fallback Reweighting**: Dynamically adjusts weights when speaker verification is `UNAVAILABLE` without inventing artificial numbers.
4. **Targeted Clone Detection**: Flags contradictory telemetry when synthetic audio matches an enrolled VIP voice profile.
5. **Security & DPDP Privacy Compliance**: Zero-raw-audio retention, SHA-256 session tokenization, anti-replay nonce validation, and defensive input sanitization.

---

## Directory Structure
```
risk_engine/
├── pom.xml                                           # Maven build configuration
├── src/
│   ├── main/
│   │   └── java/com/voiceshield/risk/
│   │       ├── context/                              # Context Analysis Engine
│   │       │   ├── ContextAnalyzer.java
│   │       │   └── ContextRulesConfig.java
│   │       ├── risk/                                 # Multi-Signal Risk Scoring & Decision Engine
│   │       │   ├── RiskScoringEngine.java
│   │       │   ├── RiskWeightsConfig.java
│   │       │   ├── DecisionEngine.java
│   │       │   └── ReasonCodeGenerator.java
│   │       ├── security/                             # Security, Privacy & Replay Defense
│   │       │   ├── PrivacyGuard.java
│   │       │   ├── InputValidator.java
│   │       │   └── ReplayProtection.java
│   │       ├── model/                                # POJOs matching frozen contract
│   │       │   ├── RiskSignalInput.java
│   │       │   ├── RiskEvaluationResult.java
│   │       │   ├── ContextMetadata.java
│   │       │   ├── ContextAnalysisResult.java
│   │       │   ├── RiskLevel.java
│   │       │   ├── Decision.java
│   │       │   └── SpeakerVerificationStatus.java
│   │       ├── mock/                                 # Mock signals for parallel development
│   │       │   └── MockSignalFactory.java
│   │       └── cli/                                  # CLI / Interoperability runner
│   │           └── RiskEngineCli.java
│   └── test/
│       └── java/com/voiceshield/risk/                # 34 automated unit & contract tests
│           ├── ContextAnalyzerTest.java
│           ├── DecisionEngineTest.java
│           ├── RiskScoringEngineTest.java
│           ├── BoundaryAndEdgeCaseTest.java
│           ├── SecurityAndPrivacyTest.java
│           └── FrozenContractComplianceTest.java
```

---

## Quickstart

### 1. Build and Run Test Suite
```bash
cd risk_engine
mvn clean test
```

### 2. Package Executable JAR
```bash
mvn package
```
Generates `target/risk-engine-1.2.0.jar` (shaded standalone fat-jar).

### 3. Run Pre-Canned Mock Scenarios via CLI
```bash
# Prompt example attack scenario (Deepfake 0.87, Mismatch, ₹5,00,000, Urgency)
java -jar target/risk-engine-1.2.0.jar --mock attack

# Genuine human voice scenario
java -jar target/risk-engine-1.2.0.jar --mock low

# Contradictory signal scenario (Deepfake 0.89 + Speaker MATCH)
java -jar target/risk-engine-1.2.0.jar --mock contradictory

# Missing speaker reference scenario
java -jar target/risk-engine-1.2.0.jar --mock missing
```

### 4. Evaluate Custom JSON Payload
```bash
java -jar target/risk-engine-1.2.0.jar --file /path/to/input.json
# OR stream via STDIN:
cat input.json | java -jar target/risk-engine-1.2.0.jar --stdin
```

---

## Frozen Integration Contract
- Schema: [`integration/contracts/risk_contract.schema.json`](../integration/contracts/risk_contract.schema.json)
- Sample JSON: [`integration/contracts/risk_contract.json`](../integration/contracts/risk_contract.json)
- Interface Guide: [`integration/contracts/risk_contract.md`](../integration/contracts/risk_contract.md)
- Complete 10-Point Engineering Handoff: [`docs/PERSON_4_HANDOFF.md`](../docs/PERSON_4_HANDOFF.md)
