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

    // ------------------------------------------------------------
    // MULTIPART ENROLLMENT
    // ------------------------------------------------------------

    @PostMapping(
            value = "/enroll",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Enroll Speaker",
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

            boolean enrolled =
                    speakerService.enrollSpeaker(
                            speakerId,
                            file.getBytes()
                    );

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
                            "message",
                            "Speaker voice profile enrolled successfully"
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
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Enroll Speaker using Base64",
            description = "Enrolls a speaker using Base64 encoded audio"
    )
    public ResponseEntity<Map<String, Object>> enrollSpeakerJson(
            @RequestBody @Valid SpeakerEnrollRequest request
    ) {
        try {
            String speakerId = request.getSpeakerId();

            if (speakerId == null || speakerId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Speaker ID is required"
                        )
                );
            }

            if (request.getAudioBase64() == null
                    || request.getAudioBase64().isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Base64 audio is required"
                        )
                );
            }

            byte[] audioBytes =
                    Base64.getDecoder().decode(
                            request.getAudioBase64()
                    );

            boolean enrolled =
                    speakerService.enrollSpeaker(
                            speakerId,
                            audioBytes
                    );

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
                            "message",
                            "Speaker voice profile enrolled successfully"
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Invalid Base64 audio"
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
    // MULTIPART VERIFICATION
    // ------------------------------------------------------------

    @PostMapping(
            value = "/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Verify Speaker",
            description = "Verifies speaker identity using uploaded audio"
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

            SpeakerVerifyResponse response =
                    speakerService.verifySpeaker(
                            speakerId,
                            file.getBytes()
                    );

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
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Verify Speaker using Base64",
            description = "Verifies speaker identity using Base64 encoded audio"
    )
    public ResponseEntity<SpeakerVerifyResponse> verifySpeakerJson(
            @RequestBody @Valid SpeakerVerifyRequest request
    ) {
        try {
            String speakerId =
                    request.getClaimedSpeakerId();

            if (speakerId == null || speakerId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getAudioBase64() == null
                    || request.getAudioBase64().isBlank()) {

                return ResponseEntity.badRequest().build();
            }

            byte[] audioBytes =
                    Base64.getDecoder().decode(
                            request.getAudioBase64()
                    );

            SpeakerVerifyResponse response =
                    speakerService.verifySpeaker(
                            speakerId,
                            audioBytes
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}