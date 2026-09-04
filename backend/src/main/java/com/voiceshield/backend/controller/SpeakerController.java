package com.voiceshield.backend.controller;

import com.voiceshield.backend.dto.SpeakerEnrollRequest;
import com.voiceshield.backend.dto.SpeakerVerifyRequest;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.service.SpeakerVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/speaker")
@Tag(name = "Speaker Biometrics", description = "Voice enrollment and identity verification")
public class SpeakerController {

    private final SpeakerVerificationService speakerService;

    public SpeakerController(SpeakerVerificationService speakerService) {
        this.speakerService = speakerService;
    }

    @PostMapping(value = "/enroll", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Enroll Speaker", description = "Enrolls reference vocal profile for claimed identity")
    public ResponseEntity<Map<String, Object>> enrollSpeaker(
            @RequestParam(value = "speaker_id", required = false) String speakerIdParam,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) @Valid SpeakerEnrollRequest jsonRequest
    ) {
        String speakerId = jsonRequest != null ? jsonRequest.getSpeakerId() : speakerIdParam;
        byte[] audioBytes = null;

        try {
            if (file != null && !file.isEmpty()) {
                audioBytes = file.getBytes();
            } else if (jsonRequest != null && jsonRequest.getAudioBase64() != null) {
                audioBytes = Base64.getDecoder().decode(jsonRequest.getAudioBase64());
            }

            boolean enrolled = speakerService.enrollSpeaker(speakerId, audioBytes);
            if (!enrolled) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid speaker ID"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "speaker_id", speakerId,
                    "message", "Speaker voice profile enrolled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping(value = "/verify", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Verify Speaker", description = "Verifies voice biometric similarity against enrolled profile")
    public ResponseEntity<SpeakerVerifyResponse> verifySpeaker(
            @RequestParam(value = "claimed_speaker_id", required = false) String speakerIdParam,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) @Valid SpeakerVerifyRequest jsonRequest
    ) {
        String speakerId = jsonRequest != null ? jsonRequest.getClaimedSpeakerId() : speakerIdParam;
        byte[] audioBytes = null;

        try {
            if (file != null && !file.isEmpty()) {
                audioBytes = file.getBytes();
            } else if (jsonRequest != null && jsonRequest.getAudioBase64() != null) {
                audioBytes = Base64.getDecoder().decode(jsonRequest.getAudioBase64());
            }

            SpeakerVerifyResponse response = speakerService.verifySpeaker(speakerId, audioBytes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
