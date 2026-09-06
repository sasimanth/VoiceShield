from pathlib import Path
import sys

import torch
import soundfile as sf
import numpy as np

from ml.deepfake_detector.model import DeepfakeAASISTModel


# ============================================================
# SETTINGS
# ============================================================

SAMPLE_RATE = 16000
NUM_SAMPLES = 64600

MODEL_PATH = Path("checkpoints/best_model.pth")


# ============================================================
# LOAD AUDIO
# ============================================================

def load_audio(audio_path):

    waveform, sample_rate = sf.read(
        str(audio_path),
        dtype="float32"
    )

    # Stereo -> Mono
    if waveform.ndim > 1:
        waveform = np.mean(waveform, axis=1)

    # Resample
    if sample_rate != SAMPLE_RATE:

        old_length = len(waveform)

        new_length = int(
            old_length * SAMPLE_RATE / sample_rate
        )

        old_indices = np.linspace(
            0,
            1,
            old_length
        )

        new_indices = np.linspace(
            0,
            1,
            new_length
        )

        waveform = np.interp(
            new_indices,
            old_indices,
            waveform
        ).astype(np.float32)

    # Normalize
    max_value = np.max(np.abs(waveform))

    if max_value > 0:
        waveform = waveform / max_value

    # Fixed length
    if len(waveform) > NUM_SAMPLES:

        start = (len(waveform) - NUM_SAMPLES) // 2

        waveform = waveform[
            start:start + NUM_SAMPLES
        ]

    elif len(waveform) < NUM_SAMPLES:

        padding = NUM_SAMPLES - len(waveform)

        waveform = np.pad(
            waveform,
            (0, padding)
        )

    return torch.tensor(
        waveform,
        dtype=torch.float32
    )


# ============================================================
# MAIN
# ============================================================

def main():

    # --------------------------------------------------------
    # Get audio path from command line
    # --------------------------------------------------------

    if len(sys.argv) < 2:

        print()
        print("Usage:")
        print(
            "python test_trained_model.py "
            "path_to_audio.wav"
        )
        print()
        print("Example:")
        print(
            "python test_trained_model.py "
            "test_audio/human_test_16k_mono.wav"
        )

        return

    audio_path = Path(sys.argv[1])

    if not audio_path.exists():

        print()
        print("ERROR: Audio file does not exist:")
        print(audio_path)
        return

    if not MODEL_PATH.exists():

        print()
        print("ERROR: Model checkpoint does not exist:")
        print(MODEL_PATH)
        return

    # --------------------------------------------------------
    # Header
    # --------------------------------------------------------

    print("=" * 60)
    print("VoiceShield - Trained Model Test")
    print("=" * 60)

    device = torch.device("cpu")

    print("Device:", device)
    print("Model :", MODEL_PATH)
    print("Audio :", audio_path)

    # --------------------------------------------------------
    # Load model
    # --------------------------------------------------------

    model = DeepfakeAASISTModel()

    checkpoint = torch.load(
        MODEL_PATH,
        map_location=device
    )

    if (
        isinstance(checkpoint, dict)
        and "model_state_dict" in checkpoint
    ):

        model.load_state_dict(
            checkpoint["model_state_dict"]
        )

    else:

        model.load_state_dict(checkpoint)

    model.to(device)
    model.eval()

    # --------------------------------------------------------
    # Load audio
    # --------------------------------------------------------

    waveform = load_audio(audio_path)

    waveform = waveform.unsqueeze(0)

    waveform = waveform.to(device)

    # --------------------------------------------------------
    # Prediction
    # --------------------------------------------------------

    with torch.no_grad():

        logits, embedding = model(waveform)

        probabilities = torch.softmax(
            logits,
            dim=1
        )

        human_probability = (
            probabilities[0][0].item()
        )

        spoof_probability = (
            probabilities[0][1].item()
        )

        predicted_class = torch.argmax(
            probabilities,
            dim=1
        ).item()

    # --------------------------------------------------------
    # Result
    # --------------------------------------------------------

    print()
    print("=" * 60)
    print("RESULT")
    print("=" * 60)

    print(
        f"Human probability : "
        f"{human_probability:.4f}"
    )

    print(
        f"Spoof probability : "
        f"{spoof_probability:.4f}"
    )

    if predicted_class == 0:

        print(
            "Classification     : "
            "HUMAN / BONAFIDE"
        )

    else:

        print(
            "Classification     : "
            "AI / SPOOF"
        )

    print()
    print(
        "Embedding size     :",
        embedding.shape[1]
    )

    print("=" * 60)


if __name__ == "__main__":
    main()