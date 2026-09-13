from pathlib import Path

from speech.speaker_verification.ecapa_service import (
    ECAPASpeakerEmbedder,
)


AUDIO_FILE = Path(
    "test_audio/real_world/1188-133604-0000_16k.wav"
)


def main():
    print("Loading ECAPA model...")

    embedder = ECAPASpeakerEmbedder()

    print("Generating embedding...")

    embedding = embedder.generate_embedding(
        AUDIO_FILE.read_bytes()
    )

    print()
    print("========== ECAPA TEST ==========")
    print("Audio file:", AUDIO_FILE)
    print("Embedding shape:", embedding.shape)
    print("Embedding dimension:", embedding.shape[0])
    print("Embedding dtype:", embedding.dtype)
    print("Embedding norm:", float((embedding ** 2).sum() ** 0.5))
    print("All values finite:", bool(__import__("numpy").isfinite(embedding).all()))
    print("First 10 values:")
    print(embedding[:10])
    print("================================")


if __name__ == "__main__":
    main()