from speaker_verification.verifier import SpeakerVerificationEngine

engine = SpeakerVerificationEngine()

speaker1_a = "speech/test_audio/speaker1.wav"
speaker1_b = "speech/test_audio/speaker1_b.wav"
speaker2_a = "speech/test_audio/speaker2.wav"

engine.enroll_speaker("person_1", speaker1_a)

print("\n--- Threshold Evaluation ---")

pairs = [
    ("Same speaker", speaker1_b),
    ("Different speaker", speaker2_a)
]

scores = []

for label, audio_path in pairs:
    result = engine.verify_speaker(
        "person_1",
        audio_path,
        threshold=0.0
    )

    score = result["similarity_score"]
    scores.append((label, score))

    print(f"{label}: {score}")

print("\n--- Testing Thresholds ---")

thresholds = [0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50]

for threshold in thresholds:

    genuine_score = scores[0][1]
    impostor_score = scores[1][1]

    genuine_accepted = genuine_score >= threshold
    impostor_accepted = impostor_score >= threshold

    print(
        f"Threshold {threshold:.2f} | "
        f"Genuine: {'ACCEPT' if genuine_accepted else 'REJECT'} | "
        f"Impostor: {'ACCEPT' if impostor_accepted else 'REJECT'}"
    )