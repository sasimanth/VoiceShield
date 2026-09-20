from speaker_verification.verifier import SpeakerVerificationEngine

engine = SpeakerVerificationEngine()

speaker1_recordings = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]

speaker2_recordings = [
    "speech/test_audio/speaker2_1.wav",
    "speech/test_audio/speaker2_2.wav",
    "speech/test_audio/speaker2_3.wav",
]

print("\n--- Enrolling Speaker 1 ---")
result = engine.enroll_speaker("person_1", speaker1_recordings)
print(result)

print("\n--- Genuine Scores ---")
genuine_scores = []

for audio_path in speaker1_recordings:
    result = engine.verify_speaker(
        "person_1",
        audio_path,
        threshold=0.0
    )

    score = result["similarity_score"]
    genuine_scores.append(score)

    print(f"{audio_path}: {score}")

print("\n--- Impostor Scores ---")
impostor_scores = []

for audio_path in speaker2_recordings:
    result = engine.verify_speaker(
        "person_1",
        audio_path,
        threshold=0.0
    )

    score = result["similarity_score"]
    impostor_scores.append(score)

    print(f"{audio_path}: {score}")

print("\n--- Score Ranges ---")

print(
    f"Genuine minimum: "
    f"{min(genuine_scores):.4f}"
)

print(
    f"Genuine maximum: "
    f"{max(genuine_scores):.4f}"
)

print(
    f"Impostor minimum: "
    f"{min(impostor_scores):.4f}"
)

print(
    f"Impostor maximum: "
    f"{max(impostor_scores):.4f}"
)

print("\n--- Threshold Evaluation ---")

thresholds = [
    0.20,
    0.25,
    0.30,
    0.35,
    0.40,
    0.45,
    0.50,
    0.55,
    0.60,
    0.65,
    0.70,
    0.75,
    0.80,
    0.85,
    0.90,
]

for threshold in thresholds:

    genuine_accepts = sum(
        score >= threshold
        for score in genuine_scores
    )

    impostor_accepts = sum(
        score >= threshold
        for score in impostor_scores
    )

    genuine_total = len(genuine_scores)
    impostor_total = len(impostor_scores)

    genuine_rate = (
        genuine_accepts / genuine_total
    )

    impostor_rate = (
        impostor_accepts / impostor_total
    )

    print(
        f"Threshold {threshold:.2f} | "
        f"Genuine accepted: "
        f"{genuine_accepts}/{genuine_total} "
        f"({genuine_rate:.0%}) | "
        f"Impostor accepted: "
        f"{impostor_accepts}/{impostor_total} "
        f"({impostor_rate:.0%})"
    )