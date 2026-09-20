package com.voiceshield.backend.controller;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.voiceshield.backend.dto.SpeakerEnrollRequest;
import com.voiceshield.backend.dto.SpeakerEnrollResponse;
import com.voiceshield.backend.dto.SpeakerVerifyRequest;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.service.SpeakerVerificationService;
import com.voiceshield.backend.util.AudioValidationUtils;
import com.voiceshield.backend.util.InMemoryAudio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/speaker")
@Tag(name = "Speaker Biometrics", description = "Voice enrollment and identity verification")
public class SpeakerController {

    private final SpeakerVerificationService speakerService;

    public SpeakerController(SpeakerVerificationService speakerService) {
        this.speakerService = speakerService;
    }

    // ------------------------------------------------------------
    // MULTIPART ENROLLMENT
    // ------------------------------------------------------------

    @PostMapping(
            value = "/enroll",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Enroll Speaker (Multipart)",
            description = "Enrolls a speaker using an uploaded audio file"
    )
    public ResponseEntity<Map<String, Object>> enrollSpeakerMultipart(
            @RequestParam("speaker_id") String speakerId,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (speakerId == null || speakerId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Speaker ID is required"
                        )
                );
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Audio file is required"
                        )
                );
            }

            boolean enrolled = speakerService.enrollSpeaker(speakerId.trim(), file.getBytes());

            if (!enrolled) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Speaker enrollment failed"
                        )
                );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "speaker_id", speakerId.trim(),
                            "message", "Speaker voice profile enrolled successfully"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }

    // ------------------------------------------------------------
    // JSON / BASE64 ENROLLMENT
    // ------------------------------------------------------------

    @PostMapping(
            value = "/enroll",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Enroll Speaker (JSON/Base64)",
            description = "Enrolls reference vocal profile for claimed identity using Base64 encoded audio"
    )
    public ResponseEntity<SpeakerEnrollResponse> enrollSpeakerJson(
            @Valid @RequestBody SpeakerEnrollRequest request
    ) {
        byte[] rawBytes = AudioValidationUtils.validateAndDecodeBase64Audio(request.getAudioBase64());

        try (InMemoryAudio secureAudio = new InMemoryAudio(rawBytes)) {
            boolean enrolled = speakerService.enrollSpeaker(request.getSpeakerId(), secureAudio.getAudioData());

            if (!enrolled) {
                return ResponseEntity.badRequest().body(
                        new SpeakerEnrollResponse(false, request.getSpeakerId(), "Invalid speaker ID or audio data")
                );
            }

            return ResponseEntity.ok(
                    new SpeakerEnrollResponse(true, request.getSpeakerId(), "Speaker voice profile enrolled successfully")
            );
        }
    }

    // ------------------------------------------------------------
    // MULTIPART VERIFICATION
    // ------------------------------------------------------------

    @PostMapping(
            value = "/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Verify Speaker (Multipart)",
            description = "Verifies speaker identity using uploaded audio file"
    )
    public ResponseEntity<SpeakerVerifyResponse> verifySpeakerMultipart(
            @RequestParam("claimed_speaker_id") String speakerId,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (speakerId == null || speakerId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            SpeakerVerifyResponse response = speakerService.verifySpeaker(speakerId.trim(), file.getBytes());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ------------------------------------------------------------
    // JSON / BASE64 VERIFICATION
    // ------------------------------------------------------------

    @PostMapping(
            value = "/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Verify Speaker (JSON/Base64)",
            description = "Verifies voice biometric similarity against enrolled profile using Base64 encoded audio"
    )
    public ResponseEntity<SpeakerVerifyResponse> verifySpeakerJson(
            @Valid @RequestBody SpeakerVerifyRequest request
    ) {
        byte[] rawBytes = AudioValidationUtils.validateAndDecodeBase64Audio(request.getAudioBase64());

        try (InMemoryAudio secureAudio = new InMemoryAudio(rawBytes)) {
            SpeakerVerifyResponse response = speakerService.verifySpeaker(request.getClaimedSpeakerId(), secureAudio.getAudioData());
            return ResponseEntity.ok(response);
        }
    }
}