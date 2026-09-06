from pathlib import Path
import random

import soundfile as sf
import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader

from ml.deepfake_detector.model import DeepfakeAASISTModel


# ============================================================
# SETTINGS
# ============================================================

PROJECT_ROOT = Path.cwd()

TRAIN_DIR = PROJECT_ROOT / "datasets" / "asvspoof" / "train"
DEV_DIR = PROJECT_ROOT / "datasets" / "asvspoof" / "dev"

CHECKPOINT_DIR = PROJECT_ROOT / "checkpoints"
CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)

SAMPLE_RATE = 16000

# Approximately 4.04 seconds
NUM_SAMPLES = 64600

# Training settings
BATCH_SIZE = 4
EPOCHS = 10
LEARNING_RATE = 0.0001

# CPU-only system
DEVICE = torch.device("cpu")

# Dataset size
TRAIN_PER_CLASS = 800
DEV_PER_CLASS = 200


# ============================================================
# REPRODUCIBILITY
# ============================================================

random.seed(42)
np.random.seed(42)
torch.manual_seed(42)


# ============================================================
# DATASET
# ============================================================

class AudioDataset(Dataset):

    def __init__(
        self,
        root_dir,
        max_per_class=None,
        training=False
    ):

        self.samples = []
        self.training = training

        bonafide_dir = root_dir / "bonafide"
        spoof_dir = root_dir / "spoof"

        bonafide_files = sorted(
            bonafide_dir.glob("*.flac")
        )

        spoof_files = sorted(
            spoof_dir.glob("*.flac")
        )

        # Limit files if requested
        if max_per_class is not None:

            bonafide_files = bonafide_files[
                :max_per_class
            ]

            spoof_files = spoof_files[
                :max_per_class
            ]

        # 0 = Bonafide / Human
        # 1 = Spoof / AI-generated

        for file in bonafide_files:
            self.samples.append(
                (file, 0)
            )

        for file in spoof_files:
            self.samples.append(
                (file, 1)
            )

        random.shuffle(self.samples)

        print(f"\nDataset: {root_dir}")
        print(f"Bonafide: {len(bonafide_files)}")
        print(f"Spoof:    {len(spoof_files)}")
        print(f"Total:    {len(self.samples)}")

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, index):

        file_path, label = self.samples[index]

        # ----------------------------------------------------
        # Load FLAC
        # ----------------------------------------------------

        audio, sample_rate = sf.read(
            str(file_path),
            dtype="float32"
        )

        # ----------------------------------------------------
        # Stereo → Mono
        # ----------------------------------------------------

        if audio.ndim > 1:
            audio = audio.mean(axis=1)

        # ----------------------------------------------------
        # NumPy → PyTorch
        # ----------------------------------------------------

        waveform = torch.from_numpy(
            np.asarray(
                audio,
                dtype=np.float32
            )
        )

        # ----------------------------------------------------
        # Resample to 16 kHz
        # ----------------------------------------------------

        if sample_rate != SAMPLE_RATE:

            new_length = int(
                waveform.shape[0]
                * SAMPLE_RATE
                / sample_rate
            )

            waveform = torch.nn.functional.interpolate(
                waveform.unsqueeze(0).unsqueeze(0),
                size=new_length,
                mode="linear",
                align_corners=False
            ).squeeze()

        # ----------------------------------------------------
        # Normalize
        # ----------------------------------------------------

        max_value = waveform.abs().max()

        if max_value > 0:
            waveform = waveform / max_value

        # ----------------------------------------------------
        # FIXED LENGTH
        # ----------------------------------------------------

        if waveform.shape[0] > NUM_SAMPLES:

            max_start = (
                waveform.shape[0]
                - NUM_SAMPLES
            )

            if self.training:

                # Random crop during training
                start = random.randint(
                    0,
                    max_start
                )

            else:

                # Center crop during validation
                start = max_start // 2

            waveform = waveform[
                start:start + NUM_SAMPLES
            ]

        elif waveform.shape[0] < NUM_SAMPLES:

            padding = (
                NUM_SAMPLES
                - waveform.shape[0]
            )

            waveform = torch.nn.functional.pad(
                waveform,
                (0, padding)
            )

        # ----------------------------------------------------
        # Safety check
        # ----------------------------------------------------

        if waveform.shape[0] != NUM_SAMPLES:

            raise RuntimeError(
                f"Incorrect audio length: "
                f"{waveform.shape[0]}. "
                f"Expected {NUM_SAMPLES}."
            )

        return (
            waveform.float(),
            torch.tensor(
                label,
                dtype=torch.long
            )
        )


# ============================================================
# EVALUATION
# ============================================================

def evaluate(model, loader):

    model.eval()

    correct = 0
    total = 0

    all_predictions = []
    all_labels = []

    with torch.no_grad():

        for audio, labels in loader:

            audio = audio.to(DEVICE)
            labels = labels.to(DEVICE)

            logits, _ = model(audio)

            predictions = torch.argmax(
                logits,
                dim=1
            )

            correct += (
                predictions == labels
            ).sum().item()

            total += labels.size(0)

            all_predictions.extend(
                predictions.cpu().numpy()
            )

            all_labels.extend(
                labels.cpu().numpy()
            )

    if total == 0:
        return 0.0, all_labels, all_predictions

    accuracy = (
        100.0
        * correct
        / total
    )

    return accuracy, all_labels, all_predictions


# ============================================================
# METRICS
# ============================================================

def calculate_metrics(labels, predictions):

    labels = np.array(labels)
    predictions = np.array(predictions)

    # Confusion matrix
    tn = np.sum(
        (labels == 0) & (predictions == 0)
    )

    fp = np.sum(
        (labels == 0) & (predictions == 1)
    )

    fn = np.sum(
        (labels == 1) & (predictions == 0)
    )

    tp = np.sum(
        (labels == 1) & (predictions == 1)
    )

    # Precision
    precision = (
        tp / (tp + fp)
        if (tp + fp) > 0
        else 0.0
    )

    # Recall
    recall = (
        tp / (tp + fn)
        if (tp + fn) > 0
        else 0.0
    )

    # F1
    if precision + recall > 0:

        f1 = (
            2 * precision * recall
            / (precision + recall)
        )

    else:
        f1 = 0.0

    print("\n" + "=" * 60)
    print("FINAL DEVELOPMENT METRICS")
    print("=" * 60)

    print(f"Accuracy : {100 * (tp + tn) / len(labels):.2f}%")
    print(f"Precision: {precision:.4f}")
    print(f"Recall   : {recall:.4f}")
    print(f"F1 Score : {f1:.4f}")

    print("\nConfusion Matrix")
    print("----------------")
    print("                 Predicted")
    print("              Human  Spoof")
    print(f"Actual Human  {tn:5d}  {fp:5d}")
    print(f"Actual Spoof  {fn:5d}  {tp:5d}")

    return {
        "accuracy": 100 * (tp + tn) / len(labels),
        "precision": precision,
        "recall": recall,
        "f1": f1
    }


# ============================================================
# TRAINING
# ============================================================

def train():

    print("=" * 60)
    print("VoiceShield Deepfake Voice Detector")
    print("=" * 60)

    print(f"Device: {DEVICE}")
    print(f"Sample rate: {SAMPLE_RATE}")
    print(f"Samples per audio: {NUM_SAMPLES}")
    print(f"Batch size: {BATCH_SIZE}")
    print(f"Epochs: {EPOCHS}")
    print(f"Learning rate: {LEARNING_RATE}")

    # --------------------------------------------------------
    # DATASETS
    # --------------------------------------------------------

    train_dataset = AudioDataset(
        TRAIN_DIR,
        max_per_class=TRAIN_PER_CLASS,
        training=True
    )

    dev_dataset = AudioDataset(
        DEV_DIR,
        max_per_class=DEV_PER_CLASS,
        training=False
    )

    # --------------------------------------------------------
    # DATA LOADERS
    # --------------------------------------------------------

    train_loader = DataLoader(
        train_dataset,
        batch_size=BATCH_SIZE,
        shuffle=True,
        num_workers=0
    )

    dev_loader = DataLoader(
        dev_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
        num_workers=0
    )

    # --------------------------------------------------------
    # CREATE MODEL
    # --------------------------------------------------------

    model = DeepfakeAASISTModel(
        in_channels=1,
        num_classes=2
    )

    model.to(DEVICE)

    # --------------------------------------------------------
    # LOSS
    # --------------------------------------------------------

    criterion = nn.CrossEntropyLoss()

    # --------------------------------------------------------
    # OPTIMIZER
    # --------------------------------------------------------

    optimizer = torch.optim.Adam(
        model.parameters(),
        lr=LEARNING_RATE
    )

    # --------------------------------------------------------
    # BEST MODEL TRACKING
    # --------------------------------------------------------

    best_dev_accuracy = 0.0
    best_epoch = 0

    best_checkpoint_path = (
        CHECKPOINT_DIR
        / "best_model.pth"
    )

    # ========================================================
    # EPOCH LOOP
    # ========================================================

    for epoch in range(EPOCHS):

        model.train()

        total_loss = 0.0

        train_correct = 0
        train_total = 0

        # ----------------------------------------------------
        # TRAINING
        # ----------------------------------------------------

        for audio, labels in train_loader:

            audio = audio.to(DEVICE)
            labels = labels.to(DEVICE)

            optimizer.zero_grad()

            logits, embeddings = model(audio)

            loss = criterion(
                logits,
                labels
            )

            loss.backward()

            optimizer.step()

            total_loss += loss.item()

            predictions = torch.argmax(
                logits,
                dim=1
            )

            train_correct += (
                predictions == labels
            ).sum().item()

            train_total += labels.size(0)

        # ----------------------------------------------------
        # TRAINING METRICS
        # ----------------------------------------------------

        average_loss = (
            total_loss
            / len(train_loader)
        )

        train_accuracy = (
            100.0
            * train_correct
            / train_total
        )

        # ----------------------------------------------------
        # DEVELOPMENT
        # ----------------------------------------------------

        dev_accuracy, dev_labels, dev_predictions = evaluate(
            model,
            dev_loader
        )

        # ----------------------------------------------------
        # PRINT
        # ----------------------------------------------------

        print(
            f"\nEpoch {epoch + 1}/{EPOCHS}"
        )

        print(
            f"Average Loss       : "
            f"{average_loss:.4f}"
        )

        print(
            f"Training Accuracy  : "
            f"{train_accuracy:.2f}%"
        )

        print(
            f"Development Accuracy: "
            f"{dev_accuracy:.2f}%"
        )

        # ----------------------------------------------------
        # SAVE BEST MODEL
        # ----------------------------------------------------

        if dev_accuracy > best_dev_accuracy:

            best_dev_accuracy = dev_accuracy
            best_epoch = epoch + 1

            torch.save(
                {
                    "model_state_dict":
                        model.state_dict(),

                    "sample_rate":
                        SAMPLE_RATE,

                    "num_samples":
                        NUM_SAMPLES,

                    "classes": {
                        "0": "bonafide",
                        "1": "spoof"
                    },

                    "best_dev_accuracy":
                        best_dev_accuracy,

                    "epoch":
                        best_epoch
                },
                best_checkpoint_path
            )

            print(
                f"*** NEW BEST MODEL SAVED "
                f"({best_dev_accuracy:.2f}%) ***"
            )

    # ========================================================
    # LOAD BEST MODEL
    # ========================================================

    print("\n" + "=" * 60)
    print("LOADING BEST MODEL")
    print("=" * 60)

    checkpoint = torch.load(
        best_checkpoint_path,
        map_location=DEVICE
    )

    model.load_state_dict(
        checkpoint["model_state_dict"]
    )

    print(
        f"Best Epoch: {checkpoint['epoch']}"
    )

    print(
        f"Best Development Accuracy: "
        f"{checkpoint['best_dev_accuracy']:.2f}%"
    )

    # ========================================================
    # FINAL EVALUATION
    # ========================================================

    final_accuracy, final_labels, final_predictions = evaluate(
        model,
        dev_loader
    )

    calculate_metrics(
        final_labels,
        final_predictions
    )

    # ========================================================
    # COMPLETE
    # ========================================================

    print("\n" + "=" * 60)
    print("TRAINING COMPLETE")
    print("=" * 60)

    print(
        f"Best Development Accuracy: "
        f"{best_dev_accuracy:.2f}%"
    )

    print(
        f"Best Epoch: {best_epoch}"
    )

    print(
        f"Best model saved to:"
    )

    print(best_checkpoint_path)


# ============================================================
# MAIN
# ============================================================

if __name__ == "__main__":
    train()