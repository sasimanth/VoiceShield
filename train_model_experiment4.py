import os
import random
import soundfile as sf
from scipy.signal import resample_poly
import numpy as np
import torch
import torch.nn as nn
import torchaudio
from torch.utils.data import Dataset, DataLoader
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix

from ml.deepfake_detector.model import DeepfakeAASISTModel


# ============================================================
# EXPERIMENT 4 CONFIGURATION
# ============================================================

TRAIN_DIR = "datasets/experiment4/train"
DEV_DIR = "datasets/experiment4/dev"

CHECKPOINT_DIR = "checkpoints"
CHECKPOINT_PATH = os.path.join(
    CHECKPOINT_DIR,
    "experiment4_best_model.pth"
)

SAMPLE_RATE = 16000
NUM_SAMPLES = 64600

BATCH_SIZE = 4
EPOCHS = 10
LEARNING_RATE = 0.0001

SEED = 42

DEVICE = torch.device(
    "cuda" if torch.cuda.is_available() else "cpu"
)


# ============================================================
# REPRODUCIBILITY
# ============================================================

random.seed(SEED)
np.random.seed(SEED)
torch.manual_seed(SEED)

if torch.cuda.is_available():
    torch.cuda.manual_seed_all(SEED)


# ============================================================
# AUDIO AUGMENTATION
# ============================================================

def augment_audio(audio):
    """
    Moderate training-time augmentation.

    Augmentations:
    1. Random gain
    2. Small Gaussian noise
    3. Small time shift
    """

    # --------------------------------------------------------
    # Random gain
    # --------------------------------------------------------
    if random.random() < 0.5:
        gain = random.uniform(0.85, 1.15)
        audio = audio * gain

    # --------------------------------------------------------
    # Add small Gaussian noise
    # --------------------------------------------------------
    if random.random() < 0.3:
        noise_level = random.uniform(0.001, 0.005)
        noise = torch.randn_like(audio) * noise_level
        audio = audio + noise

    # --------------------------------------------------------
    # Small time shift
    # --------------------------------------------------------
    if random.random() < 0.3:
        max_shift = int(0.05 * SAMPLE_RATE)
        shift = random.randint(-max_shift, max_shift)

        if shift > 0:
            audio = torch.cat(
                [
                    torch.zeros(shift),
                    audio[:-shift]
                ]
            )

        elif shift < 0:
            shift = abs(shift)

            audio = torch.cat(
                [
                    audio[shift:],
                    torch.zeros(shift)
                ]
            )

    # --------------------------------------------------------
    # Prevent clipping
    # --------------------------------------------------------
    audio = torch.clamp(audio, -1.0, 1.0)

    return audio


# ============================================================
# DATASET
# ============================================================

class AudioDataset(Dataset):

    def __init__(
        self,
        root_dir,
        sample_rate=16000,
        num_samples=64600,
        training=False
    ):

        self.root_dir = root_dir
        self.sample_rate = sample_rate
        self.num_samples = num_samples
        self.training = training

        self.files = []

        # ----------------------------------------------------
        # Class 0 = bonafide / human
        # Class 1 = spoof / AI
        # ----------------------------------------------------

        class_dirs = {
            "bonafide": 0,
            "spoof": 1
        }

        for class_name, label in class_dirs.items():

            class_path = os.path.join(
                root_dir,
                class_name
            )

            if not os.path.exists(class_path):
                raise FileNotFoundError(
                    f"Missing directory: {class_path}"
                )

            for filename in os.listdir(class_path):

                if filename.lower().endswith(
                    (".wav", ".flac", ".mp3", ".m4a")
                ):

                    filepath = os.path.join(
                        class_path,
                        filename
                    )

                    self.files.append(
                        (filepath, label)
                    )

        random.shuffle(self.files)

        print(
            f"Loaded {len(self.files)} files from {root_dir}"
        )

    def __len__(self):
        return len(self.files)

    def load_audio(self, filepath):
        # ----------------------------------------------------
        # Load WAV / FLAC using SoundFile
        # This avoids the TorchCodec problem in torchaudio
        # ----------------------------------------------------

        audio_data, sr = sf.read(filepath, dtype="float32")

        # ----------------------------------------------------
        # Convert stereo -> mono
        # ----------------------------------------------------

        if audio_data.ndim > 1:
            audio_data = np.mean(audio_data, axis=1)

        waveform = torch.from_numpy(audio_data)
        # ----------------------------------------------------
        # Resample if required
        # ----------------------------------------------------

        if sr != self.sample_rate:
            waveform = torch.from_numpy(
                resample_poly(
                    waveform.numpy(),
                    self.sample_rate,
                    sr
                ).astype(np.float32)
            )

        waveform = waveform.float()

        # ----------------------------------------------------
        # Normalize
        # ----------------------------------------------------

        max_value = waveform.abs().max()

        if max_value > 0:
            waveform = waveform / max_value

        return waveform
    def fixed_length(self, waveform):

        length = waveform.shape[0]

        # ----------------------------------------------------
        # If audio is longer than required
        # ----------------------------------------------------

        if length > self.num_samples:

            if self.training:

                start = random.randint(
                    0,
                    length - self.num_samples
                )

            else:

                start = (
                    length - self.num_samples
                ) // 2

            waveform = waveform[
                start:start + self.num_samples
            ]

        # ----------------------------------------------------
        # If audio is shorter than required
        # ----------------------------------------------------

        elif length < self.num_samples:

            padding = self.num_samples - length

            waveform = torch.nn.functional.pad(
                waveform,
                (0, padding)
            )

        return waveform

    def __getitem__(self, index):

        filepath, label = self.files[index]

        try:

            waveform = self.load_audio(filepath)

            waveform = self.fixed_length(waveform)

            # ------------------------------------------------
            # Apply augmentation ONLY during training
            # ------------------------------------------------

            if self.training:

                waveform = augment_audio(waveform)

            return waveform, torch.tensor(
                label,
                dtype=torch.long
            )

        except Exception as e:

            print(
                f"\nError loading: {filepath}"
            )

            print(
                f"Error: {e}"
            )

            # Return silence instead of crashing training
            waveform = torch.zeros(
                self.num_samples,
                dtype=torch.float32
            )

            return waveform, torch.tensor(
                label,
                dtype=torch.long
            )


# ============================================================
# CREATE DATASETS
# ============================================================

print("\n===============================================")
print("VOICE SHIELD - EXPERIMENT 4")
print("===============================================")

print(f"Device: {DEVICE}")
print(f"Sample Rate: {SAMPLE_RATE}")
print(f"Input Samples: {NUM_SAMPLES}")
print(f"Batch Size: {BATCH_SIZE}")
print(f"Epochs: {EPOCHS}")
print(f"Learning Rate: {LEARNING_RATE}")

print("\nLoading datasets...")

train_dataset = AudioDataset(
    TRAIN_DIR,
    SAMPLE_RATE,
    NUM_SAMPLES,
    training=True
)

dev_dataset = AudioDataset(
    DEV_DIR,
    SAMPLE_RATE,
    NUM_SAMPLES,
    training=False
)


# ============================================================
# DATALOADERS
# ============================================================

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


# ============================================================
# MODEL
# ============================================================

print("\nCreating model...")

model = DeepfakeAASISTModel()

model = model.to(DEVICE)


# ============================================================
# CLASS WEIGHTS
# ============================================================

# Train:
# Bonafide = 1200
# Spoof    = 800
#
# Slightly increase spoof importance because it is the
# minority class.

class_weights = torch.tensor(
    [1.0, 1.5],
    dtype=torch.float32
).to(DEVICE)


criterion = nn.CrossEntropyLoss(
    weight=class_weights
)

optimizer = torch.optim.Adam(
    model.parameters(),
    lr=LEARNING_RATE
)


# ============================================================
# TRAINING
# ============================================================

best_f1 = 0.0
best_accuracy = 0.0

os.makedirs(
    CHECKPOINT_DIR,
    exist_ok=True
)


for epoch in range(EPOCHS):

    print("\n-----------------------------------------------")
    print(
        f"Epoch {epoch + 1}/{EPOCHS}"
    )
    print("-----------------------------------------------")

    # ========================================================
    # TRAIN
    # ========================================================

    model.train()

    running_loss = 0.0
    train_predictions = []
    train_labels = []

    for batch_idx, (audio, labels) in enumerate(
        train_loader
    ):

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

        running_loss += loss.item()

        predictions = torch.argmax(
            logits,
            dim=1
        )

        train_predictions.extend(
            predictions.detach().cpu().numpy()
        )

        train_labels.extend(
            labels.detach().cpu().numpy()
        )

        if (
            batch_idx + 1
        ) % 50 == 0:

            print(
                f"Batch {batch_idx + 1}/"
                f"{len(train_loader)}"
            )

    train_loss = (
        running_loss /
        len(train_loader)
    )

    train_accuracy = accuracy_score(
        train_labels,
        train_predictions
    )


    # ========================================================
    # VALIDATION
    # ========================================================

    model.eval()

    dev_predictions = []
    dev_labels = []

    dev_loss = 0.0

    with torch.no_grad():

        for audio, labels in dev_loader:

            audio = audio.to(DEVICE)
            labels = labels.to(DEVICE)

            logits, embeddings = model(audio)

            loss = criterion(
                logits,
                labels
            )

            dev_loss += loss.item()

            predictions = torch.argmax(
                logits,
                dim=1
            )

            dev_predictions.extend(
                predictions.cpu().numpy()
            )

            dev_labels.extend(
                labels.cpu().numpy()
            )

    dev_loss = (
        dev_loss /
        len(dev_loader)
    )

    dev_accuracy = accuracy_score(
        dev_labels,
        dev_predictions
    )

    dev_precision = precision_score(
        dev_labels,
        dev_predictions,
        zero_division=0
    )

    dev_recall = recall_score(
        dev_labels,
        dev_predictions,
        zero_division=0
    )

    dev_f1 = f1_score(
        dev_labels,
        dev_predictions,
        zero_division=0
    )


    # ========================================================
    # PRINT RESULTS
    # ========================================================

    print("\nResults:")

    print(
        f"Train Loss     : {train_loss:.4f}"
    )

    print(
        f"Train Accuracy : {train_accuracy:.4f}"
    )

    print(
        f"Dev Loss       : {dev_loss:.4f}"
    )

    print(
        f"Dev Accuracy   : {dev_accuracy:.4f}"
    )

    print(
        f"Dev Precision  : {dev_precision:.4f}"
    )

    print(
        f"Dev Recall     : {dev_recall:.4f}"
    )

    print(
        f"Dev F1         : {dev_f1:.4f}"
    )


    # ========================================================
    # CONFUSION MATRIX
    # ========================================================

    cm = confusion_matrix(
        dev_labels,
        dev_predictions
    )

    print("\nConfusion Matrix:")

    print(
        "              Human   Spoof"
    )

    print(
        f"Human         {cm[0][0]:5d}   {cm[0][1]:5d}"
    )

    print(
        f"Spoof         {cm[1][0]:5d}   {cm[1][1]:5d}"
    )


    # ========================================================
    # SAVE BEST MODEL
    # ========================================================

    if dev_f1 > best_f1:

        best_f1 = dev_f1
        best_accuracy = dev_accuracy

        torch.save(
            model.state_dict(),
            CHECKPOINT_PATH
        )

        print(
            "\n*** NEW BEST MODEL SAVED ***"
        )

        print(
            f"Checkpoint: {CHECKPOINT_PATH}"
        )


# ============================================================
# FINAL RESULT
# ============================================================

print("\n===============================================")
print("EXPERIMENT 4 TRAINING COMPLETE")
print("===============================================")

print(
    f"Best Dev Accuracy: {best_accuracy:.4f}"
)

print(
    f"Best Dev F1      : {best_f1:.4f}"
)

print(
    f"Model saved to   : {CHECKPOINT_PATH}"
)

print("===============================================")