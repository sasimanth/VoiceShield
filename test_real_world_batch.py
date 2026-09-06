import os
import sys
import numpy as np
import torch
import soundfile as sf
import librosa

from ml.deepfake_detector.model import DeepfakeAASISTModel


# ============================================================
# CONFIGURATION
# ============================================================

TEST_DIR = "test_audio/real_world"
CHECKPOINT = "checkpoints/experiment3_1600_200_best_model.pth"

SAMPLE_RATE = 16000
NUM_SAMPLES = 64600


# ============================================================
# LOAD MODEL
# ============================================================

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print("=" * 70)
print("VOICE SHIELD - REAL-WORLD GENERALIZATION TEST")
print("=" * 70)

print(f"Device     : {device}")
print(f"Checkpoint : {CHECKPOINT}")
print(f"Test folder: {TEST_DIR}")
print()


model = DeepfakeAASISTModel(num_classes=2)

checkpoint = torch.load(
    CHECKPOINT,
    map_location=device
)

# Handle either a direct state_dict or a checkpoint dictionary
if isinstance(checkpoint, dict) and "model_state_dict" in checkpoint:
    model.load_state_dict(checkpoint["model_state_dict"])
else:
    model.load_state_dict(checkpoint)

model.to(device)
model.eval()

print("Model loaded successfully.")
print()


# ============================================================
# AUDIO PREPROCESSING
# ============================================================

def load_audio(file_path):

    audio, sr = sf.read(file_path)

    # Convert stereo -> mono
    if audio.ndim > 1:
        audio = np.mean(audio, axis=1)

    audio = audio.astype(np.float32)

    # Resample if necessary
    if sr != SAMPLE_RATE:
        audio = librosa.resample(
            audio,
            orig_sr=sr,
            target_sr=SAMPLE_RATE
        )

    # Normalize
    max_value = np.max(np.abs(audio))

    if max_value > 0:
        audio = audio / max_value

    # Fixed length: 64600 samples
    if len(audio) > NUM_SAMPLES:

        # Center crop
        start = (len(audio) - NUM_SAMPLES) // 2
        audio = audio[start:start + NUM_SAMPLES]

    elif len(audio) < NUM_SAMPLES:

        padding = NUM_SAMPLES - len(audio)

        audio = np.pad(
            audio,
            (0, padding),
            mode="constant"
        )

    return audio


# ============================================================
# FIND TEST FILES
# ============================================================

if not os.path.exists(TEST_DIR):
    print(f"ERROR: Test directory does not exist:")
    print(TEST_DIR)
    sys.exit(1)


files = sorted([
    os.path.join(TEST_DIR, f)
    for f in os.listdir(TEST_DIR)
    if f.lower().endswith(".wav")
])


if len(files) == 0:

    print("ERROR: No WAV files found in:")
    print(TEST_DIR)

    print()
    print("Expected files such as:")
    print("6930-75918-0000_16k.wav")

    sys.exit(1)


print(f"Found {len(files)} WAV files.")
print()


# ============================================================
# RUN PREDICTIONS
# ============================================================

results = []

human_count = 0
spoof_count = 0


print("-" * 70)
print(
    f"{'FILE':40s} {'HUMAN %':>10s} "
    f"{'SPOOF %':>10s} {'PREDICTION':>12s}"
)
print("-" * 70)


with torch.no_grad():

    for file_path in files:

        try:

            audio = load_audio(file_path)

            waveform = torch.tensor(
                audio,
                dtype=torch.float32
            ).unsqueeze(0).to(device)

            logits, embedding = model(waveform)

            probabilities = torch.softmax(
                logits,
                dim=1
            )[0]

            human_probability = float(probabilities[0])
            spoof_probability = float(probabilities[1])

            if spoof_probability >= 0.5:
                prediction = "SPOOF"
                spoof_count += 1
            else:
                prediction = "HUMAN"
                human_count += 1

            filename = os.path.basename(file_path)

            print(
                f"{filename[:40]:40s} "
                f"{human_probability * 100:9.2f}% "
                f"{spoof_probability * 100:9.2f}% "
                f"{prediction:>12s}"
            )

            results.append({
                "file": filename,
                "human_probability": human_probability,
                "spoof_probability": spoof_probability,
                "prediction": prediction
            })

        except Exception as e:

            print()
            print(f"ERROR processing {file_path}")
            print(e)
            print()


# ============================================================
# SUMMARY
# ============================================================

total = len(results)

if total > 0:

    false_positive_rate = spoof_count / total
    human_acceptance_rate = human_count / total

    spoof_probabilities = [
        r["spoof_probability"]
        for r in results
    ]

    mean_spoof_probability = np.mean(
        spoof_probabilities
    )

    median_spoof_probability = np.median(
        spoof_probabilities
    )

    print()
    print("=" * 70)
    print("REAL-WORLD GENERALIZATION RESULTS")
    print("=" * 70)

    print(f"Total genuine files       : {total}")
    print(f"Predicted HUMAN           : {human_count}")
    print(f"Predicted SPOOF           : {spoof_count}")

    print()
    print(
        f"Human acceptance rate     : "
        f"{human_acceptance_rate * 100:.2f}%"
    )

    print(
        f"False positive rate       : "
        f"{false_positive_rate * 100:.2f}%"
    )

    print()
    print(
        f"Mean spoof probability    : "
        f"{mean_spoof_probability * 100:.2f}%"
    )

    print(
        f"Median spoof probability  : "
        f"{median_spoof_probability * 100:.2f}%"
    )

    print("=" * 70)

    print()
    print("IMPORTANT:")
    print(
        "All LibriSpeech test-clean files used here are genuine "
        "human speech."
    )

    print(
        "Therefore, any file predicted as SPOOF is a "
        "false positive."
    )