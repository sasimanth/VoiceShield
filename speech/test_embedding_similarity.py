import torch
import torch.nn.functional as F

from speaker_verification.verifier import SpeakerVerificationEngine


engine = SpeakerVerificationEngine()

speaker1_a = "speech/test_audio/speaker1.wav"
speaker1_b = "speech/test_audio/speaker1_b.wav"
speaker2_a = "speech/test_audio/speaker2.wav"


print("\n--- Extracting Embeddings ---")

embedding_1a = engine.extract_embedding(speaker1_a)
embedding_1b = engine.extract_embedding(speaker1_b)
embedding_2a = engine.extract_embedding(speaker2_a)

print("Speaker 1 - Recording A:", embedding_1a.shape)
print("Speaker 1 - Recording B:", embedding_1b.shape)
print("Speaker 2 - Recording A:", embedding_2a.shape)


print("\n--- Embedding Similarity ---")

same_speaker_score = F.cosine_similarity(
    embedding_1a.unsqueeze(0),
    embedding_1b.unsqueeze(0)
).item()

different_speaker_score = F.cosine_similarity(
    embedding_1a.unsqueeze(0),
    embedding_2a.unsqueeze(0)
).item()


print(
    f"Speaker 1 A vs Speaker 1 B: "
    f"{same_speaker_score:.4f}"
)

print(
    f"Speaker 1 A vs Speaker 2 A: "
    f"{different_speaker_score:.4f}"
)