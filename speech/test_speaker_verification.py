from speaker_verification.verifier import SpeakerVerificationEngine

engine = SpeakerVerificationEngine()

speaker1 = "speech/test_audio/speaker1.wav"
speaker2 = "speech/test_audio/speaker2.wav"

print("\nEnrollment:")
print(engine.enroll_speaker("person_1", speaker1))

print("\nSame speaker:")
print(engine.verify_speaker("person_1", speaker1))

print("\nDifferent speaker:")
print(engine.verify_speaker("person_1", speaker2))