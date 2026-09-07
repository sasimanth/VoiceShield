from speaker_verification.verifier import SpeakerVerificationEngine


engine = SpeakerVerificationEngine()


# -----------------------------
# Speaker 1 enrollment recordings
# -----------------------------

speaker1_recordings = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]


# -----------------------------
# Speaker 2 test recordings
# -----------------------------

speaker2_recordings = [
    "speech/test_audio/speaker2_1.wav",
    "speech/test_audio/speaker2_2.wav",
    "speech/test_audio/speaker2_3.wav",
]


print("\n--- Enrolling Speaker 1 ---")

enrollment_result = engine.enroll_speaker(
    "person_1",
    speaker1_recordings
)

print(enrollment_result)


print("\n--- Testing Speaker 1 Recordings ---")

for audio_path in speaker1_recordings:

    result = engine.verify_speaker(
        "person_1",
        audio_path
    )

    print(
        f"{audio_path} -> "
        f"Score: {result['similarity_score']} | "
        f"Status: {result['status']}"
    )


print("\n--- Testing Speaker 2 Recordings ---")

for audio_path in speaker2_recordings:

    result = engine.verify_speaker(
        "person_1",
        audio_path
    )

    print(
        f"{audio_path} -> "
        f"Score: {result['similarity_score']} | "
        f"Status: {result['status']}"
    )