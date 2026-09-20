from speaker_verification.verifier import SpeakerVerificationEngine
import os


# --------------------------------------------------
# VoiceShield - Speaker Verification Demo
# --------------------------------------------------

engine = SpeakerVerificationEngine()


# Recordings used to create Person 1's voice profile
speaker1_recordings = [
    "speech/test_audio/speaker1_1.wav",
    "speech/test_audio/speaker1_2.wav",
    "speech/test_audio/speaker1_3.wav",
    "speech/test_audio/speaker1_4.wav",
    "speech/test_audio/speaker1_5.wav",
]


print("\n========================================")
print("       VOICESHIELD SPEAKER VERIFICATION")
print("========================================")


# --------------------------------------------------
# Step 1: Enroll speaker
# --------------------------------------------------

print("\n[1] Creating speaker profile...")

enrollment = engine.enroll_speaker(
    "person_1",
    speaker1_recordings
)

print("Status:", enrollment["status"])
print("Speaker:", enrollment["speaker_id"])
print("Recordings used:", enrollment["recordings_used"])


# --------------------------------------------------
# Step 2: Select audio to test
# --------------------------------------------------

print("\n[2] Enter the audio file you want to verify.")

audio_path = input(
    "Audio path: "
).strip()


# --------------------------------------------------
# Step 3: Check file
# --------------------------------------------------

if not os.path.exists(audio_path):

    print("\nERROR: Audio file not found.")
    print("Please check the file path and try again.")

else:

    # --------------------------------------------------
    # Step 4: Verify speaker
    # --------------------------------------------------

    print("\n[3] Analyzing voice...")

    try:

        result = engine.verify_speaker(
            "person_1",
            audio_path,
            threshold=0.30
        )

        # --------------------------------------------------
        # Step 5: Display result
        # --------------------------------------------------

        print("\n========================================")
        print("              VERIFICATION RESULT")
        print("========================================")

        if not result["enrolled"]:

            print("Error:", result["message"])

        else:

            print(
                f"Similarity Score: "
                f"{result['similarity_score']}"
            )

            print(
                f"Identity Anomaly Score: "
                f"{result['identity_anomaly_score']}"
            )

            print(
                f"Threshold: "
                f"{result['similarity_threshold']}"
            )

            print(
                f"Status: "
                f"{result['status']}"
            )

            print(
                f"Verified: "
                f"{result['verified']}"
            )

            print("----------------------------------------")

            if result["verified"]:

                print("RESULT: GENUINE SPEAKER")
                print(
                    "Voice matches the enrolled speaker."
                )

            else:

                print("RESULT: POSSIBLE IMPERSONATION")
                print(
                    "Voice does not match the enrolled speaker."
                )

        print("========================================\n")

    except Exception as error:

        print("\nERROR: Unable to analyze the audio.")
        print("Reason:", error)