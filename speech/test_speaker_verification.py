from speaker_verification.verifier import SpeakerVerificationEngine

engine = SpeakerVerificationEngine()

speaker1 = "speech/test_audio/speaker1.wav"

print("\n--- Testing ECAPA Embedding Extraction ---")

embedding = engine.extract_embedding(speaker1)

print("Embedding extracted successfully!")
print("Embedding shape:", embedding.shape)
print("Embedding type:", embedding.dtype)