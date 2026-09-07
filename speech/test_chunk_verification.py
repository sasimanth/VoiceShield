from speaker_verification.verifier import SpeakerVerificationEngine


engine = SpeakerVerificationEngine()

# Enroll Speaker A
speaker_a_recordings = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]

print("\n--- Enrolling Speaker A ---")

engine.enroll_speaker(
    "speaker_a",
    speaker_a_recordings
)

print("Speaker A enrolled successfully!")


# Test every audio chunk
print("\n--- Real-Time Chunk Verification ---")
print("----------------------------------------")

for i in range(1, 8):

    chunk_path = f"speech/test_audio/chunks/chunk_{i}.wav"

    result = engine.verify_speaker(
        "speaker_a",
        chunk_path,
        threshold=0.30
    )

    print(
        f"Chunk {i}: "
        f"Score = {result['similarity_score']} | "
        f"Status = {result['status']}"
    )


print("\n--- Chunk verification completed ---")