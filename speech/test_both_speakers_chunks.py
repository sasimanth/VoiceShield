from speaker_verification.verifier import SpeakerVerificationEngine


engine = SpeakerVerificationEngine()


# --------------------------------------------------
# Enroll Speaker A
# --------------------------------------------------

speaker_a_recordings = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]

engine.enroll_speaker(
    "speaker_a",
    speaker_a_recordings
)

print("\n========================================")
print("   VOICESHIELD - CHUNK VERIFICATION")
print("========================================")


# --------------------------------------------------
# Test Speaker A chunks
# --------------------------------------------------

print("\n[1] Speaker A chunks")
print("----------------------------------------")

for i in range(1, 8):

    chunk_path = (
        f"speech/test_audio/chunks/chunk_{i}.wav"
    )

    result = engine.verify_speaker(
        "speaker_a",
        chunk_path,
        threshold=0.30
    )

    print(
    f"Chunk {i}: "
    f"Score = {result['similarity_score']} | "
    f"Risk = {result['risk_level']} | "
    f"Status = {result['status']}"
    )


# --------------------------------------------------
# Test Speaker B full recordings
# --------------------------------------------------

speaker_b_recordings = [
    "speech/test_audio/speaker2_1.wav",
    "speech/test_audio/speaker2_2.wav",
    "speech/test_audio/speaker2_3.wav",
]

print("\n[2] Speaker B recordings")
print("----------------------------------------")

for audio_path in speaker_b_recordings:

    result = engine.verify_speaker(
        "speaker_a",
        audio_path,
        threshold=0.30
    )

    print(
    f"{audio_path.split('/')[-1]}: "
    f"Score = {result['similarity_score']} | "
    f"Risk = {result['risk_level']} | "
    f"Status = {result['status']}"
    )


print("\n========================================")
print("             TEST COMPLETE")
print("========================================")