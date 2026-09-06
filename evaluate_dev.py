from pathlib import Path
import numpy as np
import soundfile as sf
import torch
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report
)

from ml.deepfake_detector.model import DeepfakeAASISTModel


# ============================================================
# SETTINGS
# ============================================================

PROJECT_ROOT = Path.cwd()

DEV_DIR = PROJECT_ROOT / "datasets" / "asvspoof" / "dev"
CHECKPOINT = PROJECT_ROOT / "checkpoints" / "best_model.pth"

SAMPLE_RATE = 16000
NUM_SAMPLES = 64600

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


# ============================================================
# LOAD MODEL
# ============================================================

print("=" * 70)
print("VoiceShield - Complete Development Set Evaluation")
print("=" * 70)

print(f"Device     : {DEVICE}")
print(f"Checkpoint : {CHECKPOINT}")
print(f"Dev set    : {DEV_DIR}")
print()


model = DeepfakeAASISTModel(num_classes=2)

checkpoint = torch.load(
    CHECKPOINT,
    map_location=DEVICE
)

if isinstance(checkpoint, dict) and "model_state_dict" in checkpoint:
    model.load_state_dict(checkpoint["model_state_dict"])
else:
    model.load_state_dict(checkpoint)

model.to(DEVICE)
model.eval()

print("Model loaded successfully.")
print()


# ============================================================
# AUDIO PREPROCESSING
# ============================================================

def load_audio(path):

    audio, sr = sf.read(path, dtype="float32")

    # Convert stereo to mono
    if audio.ndim > 1:
        audio = np.mean(audio, axis=1)

    # Resample if necessary
    if sr != SAMPLE_RATE:

        old_length = len(audio)

        new_length = int(
            old_length * SAMPLE_RATE / sr
        )

        old_positions = np.linspace(
            0,
            1,
            old_length
        )

        new_positions = np.linspace(
            0,
            1,
            new_length
        )

        audio = np.interp(
            new_positions,
            old_positions,
            audio
        )

    # Normalize
    max_value = np.max(np.abs(audio))

    if max_value > 0:
        audio = audio / max_value

    # Center crop / pad
    if len(audio) > NUM_SAMPLES:

        start = (len(audio) - NUM_SAMPLES) // 2

        audio = audio[
            start:start + NUM_SAMPLES
        ]

    elif len(audio) < NUM_SAMPLES:

        padding = NUM_SAMPLES - len(audio)

        audio = np.pad(
            audio,
            (0, padding)
        )

    return torch.tensor(
        audio,
        dtype=torch.float32
    )


# ============================================================
# EVALUATION
# ============================================================

all_labels = []
all_predictions = []
all_spoof_probabilities = []

results = []


class_info = [
    ("bonafide", 0),
    ("spoof", 1)
]


for folder_name, true_label in class_info:

    folder = DEV_DIR / folder_name

    files = sorted(folder.glob("*.flac"))

    print(
        f"Evaluating {folder_name}: "
        f"{len(files)} files"
    )

    for audio_file in files:

        try:

            audio = load_audio(audio_file)

            audio = audio.unsqueeze(0)

            audio = audio.to(DEVICE)

            with torch.no_grad():

                logits, embedding = model(audio)

                probabilities = torch.softmax(
                    logits,
                    dim=1
                )

            human_probability = probabilities[0, 0].item()

            spoof_probability = probabilities[0, 1].item()

            prediction = int(
                torch.argmax(probabilities, dim=1).item()
            )

            all_labels.append(true_label)
            all_predictions.append(prediction)
            all_spoof_probabilities.append(
                spoof_probability
            )

            results.append({
                "file": audio_file.name,
                "actual": true_label,
                "prediction": prediction,
                "human_probability": human_probability,
                "spoof_probability": spoof_probability
            })

        except Exception as e:

            print(
                f"ERROR processing "
                f"{audio_file.name}: {e}"
            )


# ============================================================
# METRICS
# ============================================================

accuracy = accuracy_score(
    all_labels,
    all_predictions
)

precision = precision_score(
    all_labels,
    all_predictions,
    zero_division=0
)

recall = recall_score(
    all_labels,
    all_predictions,
    zero_division=0
)

f1 = f1_score(
    all_labels,
    all_predictions,
    zero_division=0
)

cm = confusion_matrix(
    all_labels,
    all_predictions
)


# ============================================================
# RESULTS
# ============================================================

print()
print("=" * 70)
print("FINAL RESULTS")
print("=" * 70)

print(
    f"Total samples : {len(all_labels)}"
)

print(
    f"Accuracy      : {accuracy:.4f} "
    f"({accuracy * 100:.2f}%)"
)

print(
    f"Precision     : {precision:.4f}"
)

print(
    f"Recall        : {recall:.4f}"
)

print(
    f"F1 Score      : {f1:.4f}"
)

print()
print("Confusion Matrix")
print()
print("                 Predicted")
print("              Human    Spoof")
print(
    f"Actual Human   {cm[0,0]:4d}     {cm[0,1]:4d}"
)
print(
    f"Actual Spoof   {cm[1,0]:4d}     {cm[1,1]:4d}"
)


# ============================================================
# SECURITY-IMPORTANT ERRORS
# ============================================================

false_positives = [
    r for r in results
    if r["actual"] == 0
    and r["prediction"] == 1
]

false_negatives = [
    r for r in results
    if r["actual"] == 1
    and r["prediction"] == 0
]


print()
print("=" * 70)
print("ERROR ANALYSIS")
print("=" * 70)

print(
    f"Human classified as Spoof : "
    f"{len(false_positives)}"
)

print(
    f"Spoof classified as Human : "
    f"{len(false_negatives)}"
)


# ============================================================
# MOST DANGEROUS FALSE NEGATIVES
# ============================================================

print()
print("Most dangerous false negatives:")
print("(AI/Spoof samples classified as Human)")
print()

false_negatives.sort(
    key=lambda x: x["spoof_probability"]
)

for r in false_negatives[:10]:

    print(
        f"{r['file']:25s} "
        f"Spoof={r['spoof_probability']:.4f}"
    )


# ============================================================
# CLASSIFICATION REPORT
# ============================================================

print()
print("=" * 70)
print("CLASSIFICATION REPORT")
print("=" * 70)

print(
    classification_report(
        all_labels,
        all_predictions,
        target_names=[
            "Human / Bonafide",
            "AI / Spoof"
        ],
        zero_division=0
    )
)

print("=" * 70)
print("Evaluation complete.")
print("=" * 70)