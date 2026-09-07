package com.voiceshield.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping(
        value = "/enroll",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Enroll Speaker", description = "Enrolls reference vocal profile for claimed identity")
    public ResponseEntity<SpeakerEnrollResponse> enrollSpeaker(
            @Valid @RequestBody SpeakerEnrollRequest request) {

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

    @PostMapping(
        value = "/verify",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Verify Speaker", description = "Verifies voice biometric similarity against enrolled profile")
    public ResponseEntity<SpeakerVerifyResponse> verifySpeaker(
            @Valid @RequestBody SpeakerVerifyRequest request) {

        byte[] rawBytes = AudioValidationUtils.validateAndDecodeBase64Audio(request.getAudioBase64());

        try (InMemoryAudio secureAudio = new InMemoryAudio(rawBytes)) {
            SpeakerVerifyResponse response = speakerService.verifySpeaker(request.getClaimedSpeakerId(), secureAudio.getAudioData());
            return ResponseEntity.ok(response);
        }
    }
}