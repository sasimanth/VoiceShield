from speaker_verification.verifier import SpeakerVerificationEngine


# --------------------------------------------------
# VoiceShield - Automatic Speaker Verification Test
# --------------------------------------------------

engine = SpeakerVerificationEngine()


# Speaker A recordings
speaker_a = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]


# Speaker B recordings
speaker_b = [
    "speech/test_audio/speaker2_1.wav",
    "speech/test_audio/speaker2_2.wav",
    "speech/test_audio/speaker2_3.wav",
]


print("\n========================================")
print("   VOICESHIELD - SPEAKER VERIFICATION")
print("========================================")


# --------------------------------------------------
# Step 1: Enroll Speaker A
# --------------------------------------------------

print("\n[1] Enrolling Speaker A...")

result = engine.enroll_speaker(
    "speaker_a",
    speaker_a
)

print("Status:", result["status"])
print("Recordings used:", result["recordings_used"])


# --------------------------------------------------
# Step 2: Test Speaker A
# --------------------------------------------------

print("\n[2] Testing Speaker A recordings...")
print("----------------------------------------")

for audio_path in speaker_a:

    result = engine.verify_speaker(
        "speaker_a",
        audio_path,
        threshold=0.30
    )

    print(
        f"{audio_path.split('/')[-1]} -> "
        f"Score: {result['similarity_score']} | "
        f"{result['status']}"
    )


# --------------------------------------------------
# Step 3: Test Speaker B
# --------------------------------------------------

print("\n[3] Testing Speaker B recordings...")
print("----------------------------------------")

for audio_path in speaker_b:

    result = engine.verify_speaker(
        "speaker_a",
        audio_path,
        threshold=0.30
    )

    print(
        f"{audio_path.split('/')[-1]} -> "
        f"Score: {result['similarity_score']} | "
        f"{result['status']}"
    )


print("\n========================================")
print("             TEST COMPLETE")
print("========================================")