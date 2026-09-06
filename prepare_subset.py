from pathlib import Path
import shutil
import random

# ============================================================
# ASVspoof 2019 LA paths
# ============================================================

SOURCE_ROOT = Path(r"C:\Users\India\Downloads\LA\LA")

TRAIN_AUDIO = SOURCE_ROOT / "ASVspoof2019_LA_train" / "flac"
DEV_AUDIO = SOURCE_ROOT / "ASVspoof2019_LA_dev" / "flac"

PROTOCOL_DIR = SOURCE_ROOT / "ASVspoof2019_LA_cm_protocols"

TRAIN_PROTOCOL = (
    PROTOCOL_DIR / "ASVspoof2019.LA.cm.train.trn.txt"
)

DEV_PROTOCOL = (
    PROTOCOL_DIR / "ASVspoof2019.LA.cm.dev.trl.txt"
)

# ============================================================
# VoiceShield project folders
# ============================================================

PROJECT_ROOT = Path.cwd()

DATASET_ROOT = (
    PROJECT_ROOT / "datasets" / "asvspoof"
)

BASELINE_ROOT = (
    PROJECT_ROOT / "datasets" / "asvspoof_baseline_400_100"
)

# ============================================================
# Experiment 2 sizes
# ============================================================

TRAIN_PER_CLASS = 800
DEV_PER_CLASS = 200

RANDOM_SEED = 42


# ============================================================
# Read protocol
# ============================================================

def read_protocol(protocol_file):
    """Return bonafide and spoof audio IDs."""

    bonafide = []
    spoof = []

    with open(protocol_file, "r") as f:

        for line in f:

            parts = line.strip().split()

            if len(parts) < 5:
                continue

            audio_id = parts[1]
            label = parts[-1].lower()

            if label == "bonafide":
                bonafide.append(audio_id)

            elif label == "spoof":
                spoof.append(audio_id)

    return bonafide, spoof


# ============================================================
# Copy files
# ============================================================

def copy_files(
    audio_folder,
    audio_ids,
    destination
):

    destination.mkdir(
        parents=True,
        exist_ok=True
    )

    copied = 0

    for audio_id in audio_ids:

        source = (
            audio_folder /
            f"{audio_id}.flac"
        )

        target = (
            destination /
            f"{audio_id}.flac"
        )

        if not source.exists():

            print(
                f"WARNING: Missing file: {source}"
            )

            continue

        shutil.copy2(
            source,
            target
        )

        copied += 1

    return copied


# ============================================================
# Select reproducibly
# ============================================================

def select_files(
    protocol_file,
    per_class,
    seed
):

    bonafide, spoof = read_protocol(
        protocol_file
    )

    print()
    print(
        f"Protocol: {protocol_file.name}"
    )

    print(
        f"Bonafide available: {len(bonafide)}"
    )

    print(
        f"Spoof available:    {len(spoof)}"
    )

    rng = random.Random(seed)

    rng.shuffle(bonafide)
    rng.shuffle(spoof)

    return (
        bonafide[:per_class],
        spoof[:per_class]
    )


# ============================================================
# Prepare TRAIN
# ============================================================

print("=" * 70)
print("VoiceShield - Experiment 2 Dataset Preparation")
print("=" * 70)

print()
print("Target:")
print("Train : 400 bonafide + 400 spoof")
print("Dev   : 100 bonafide + 100 spoof")
print("Seed  : 42")


# ------------------------------------------------------------
# TRAIN
# ------------------------------------------------------------

train_bonafide, train_spoof = select_files(
    TRAIN_PROTOCOL,
    TRAIN_PER_CLASS,
    RANDOM_SEED
)

train_bonafide_dest = (
    DATASET_ROOT / "train" / "bonafide"
)

train_spoof_dest = (
    DATASET_ROOT / "train" / "spoof"
)

print()
print("Copying training bonafide files...")

copied_train_bonafide = copy_files(
    TRAIN_AUDIO,
    train_bonafide,
    train_bonafide_dest
)

print(
    f"Copied: {copied_train_bonafide}"
)

print()
print("Copying training spoof files...")

copied_train_spoof = copy_files(
    TRAIN_AUDIO,
    train_spoof,
    train_spoof_dest
)

print(
    f"Copied: {copied_train_spoof}"
)


# ============================================================
# DEV
# ============================================================

print()
print("=" * 70)
print("Preparing development set")
print("=" * 70)


dev_bonafide, dev_spoof = select_files(
    DEV_PROTOCOL,
    DEV_PER_CLASS,
    RANDOM_SEED
)


# ============================================================
# Preserve original 100 dev files
# ============================================================

print()
print("Preserving original baseline dev files...")

baseline_dev_bonafide = (
    BASELINE_ROOT /
    "dev" /
    "bonafide"
)

baseline_dev_spoof = (
    BASELINE_ROOT /
    "dev" /
    "spoof"
)

new_dev_bonafide_dest = (
    DATASET_ROOT /
    "dev" /
    "bonafide"
)

new_dev_spoof_dest = (
    DATASET_ROOT /
    "dev" /
    "spoof"
)


# Get original files

original_bonafide = sorted(
    baseline_dev_bonafide.glob("*.flac")
)

original_spoof = sorted(
    baseline_dev_spoof.glob("*.flac")
)


print(
    f"Original bonafide files found: "
    f"{len(original_bonafide)}"
)

print(
    f"Original spoof files found: "
    f"{len(original_spoof)}"
)


# Copy original 50 + add 50 new

existing_bonafide_ids = {
    file.stem
    for file in original_bonafide
}

existing_spoof_ids = {
    file.stem
    for file in original_spoof
}


additional_bonafide = [
    audio_id
    for audio_id in dev_bonafide
    if audio_id not in existing_bonafide_ids
]

additional_spoof = [
    audio_id
    for audio_id in dev_spoof
    if audio_id not in existing_spoof_ids
]


# Need 50 additional files per class

additional_bonafide = additional_bonafide[:50]
additional_spoof = additional_spoof[:50]


print()
print(
    f"Additional bonafide files: "
    f"{len(additional_bonafide)}"
)

print(
    f"Additional spoof files: "
    f"{len(additional_spoof)}"
)


# Copy original files

copied_original_bonafide = copy_files(
    DEV_AUDIO,
    [
        file.stem
        for file in original_bonafide
    ],
    new_dev_bonafide_dest
)

copied_original_spoof = copy_files(
    DEV_AUDIO,
    [
        file.stem
        for file in original_spoof
    ],
    new_dev_spoof_dest
)


# Copy additional files

copied_additional_bonafide = copy_files(
    DEV_AUDIO,
    additional_bonafide,
    new_dev_bonafide_dest
)

copied_additional_spoof = copy_files(
    DEV_AUDIO,
    additional_spoof,
    new_dev_spoof_dest
)


# ============================================================
# Final verification
# ============================================================

final_train_bonafide = list(
    (DATASET_ROOT / "train" / "bonafide")
    .glob("*.flac")
)

final_train_spoof = list(
    (DATASET_ROOT / "train" / "spoof")
    .glob("*.flac")
)

final_dev_bonafide = list(
    (DATASET_ROOT / "dev" / "bonafide")
    .glob("*.flac")
)

final_dev_spoof = list(
    (DATASET_ROOT / "dev" / "spoof")
    .glob("*.flac")
)


print()
print("=" * 70)
print("FINAL DATASET COUNTS")
print("=" * 70)

print(
    f"Train bonafide : "
    f"{len(final_train_bonafide)}"
)

print(
    f"Train spoof    : "
    f"{len(final_train_spoof)}"
)

print(
    f"Dev bonafide   : "
    f"{len(final_dev_bonafide)}"
)

print(
    f"Dev spoof      : "
    f"{len(final_dev_spoof)}"
)

print()
print("=" * 70)
print("DATASET PREPARATION COMPLETE")
print("=" * 70)