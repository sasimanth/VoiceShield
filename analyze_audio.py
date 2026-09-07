from pathlib import Path
import numpy as np
import soundfile as sf

from ml.deepfake_detector.spectral_features import SpectralFeatureExtractor


# ============================================================
# VOICE SHIELD - LARGE AUDIO FEATURE ANALYSIS
# ============================================================

PROJECT_ROOT = Path.cwd()

HUMAN_AUDIO = PROJECT_ROOT / "test_audio" / "human_test_16k_mono.wav"

BONAFIDE_DIR = PROJECT_ROOT / "datasets" / "asvspoof" / "dev" / "bonafide"
SPOOF_DIR = PROJECT_ROOT / "datasets" / "asvspoof" / "dev" / "spoof"

NUM_SAMPLES = 50
TARGET_SR = 16000

extractor = SpectralFeatureExtractor(sample_rate=TARGET_SR)


def load_audio(path):
    audio, sr = sf.read(str(path))

    # Stereo -> mono
    if audio.ndim > 1:
        audio = np.mean(audio, axis=1)

    audio = audio.astype(np.float32)

    # Resample to 16 kHz
    if sr != TARGET_SR:
        old_times = np.linspace(0, 1, len(audio), endpoint=False)
        new_length = int(len(audio) * TARGET_SR / sr)
        new_times = np.linspace(0, 1, new_length, endpoint=False)

        audio = np.interp(new_times, old_times, audio)

    # Normalize
    max_value = np.max(np.abs(audio))

    if max_value > 0:
        audio = audio / max_value

    return audio


def extract_features(path):
    audio = load_audio(path)
    return extractor.extract(audio)


def collect_features(folder, limit):
    files = sorted(folder.glob("*.flac"))[:limit]

    results = []

    for i, path in enumerate(files, start=1):
        try:
            features = extract_features(path)
            results.append(features)

            print(
                f"{i:02d}/{limit}  "
                f"{path.name}  "
                f"centroid={features['spectral_centroid_hz']:.2f}  "
                f"flatness={features['spectral_flatness']:.4f}  "
                f"rolloff={features['spectral_rolloff_hz']:.2f}  "
                f"ZCR={features['zero_crossing_rate']:.4f}  "
                f"HF={features['high_freq_energy_ratio']:.4f}"
            )

        except Exception as e:
            print(f"ERROR: {path.name} -> {e}")

    return results


def calculate_statistics(results):
    feature_names = [
        "spectral_centroid_hz",
        "spectral_flatness",
        "spectral_rolloff_hz",
        "zero_crossing_rate",
        "high_freq_energy_ratio",
        "acoustic_anomaly_score"
    ]

    statistics = {}

    for feature in feature_names:
        values = np.array([r[feature] for r in results], dtype=np.float64)

        statistics[feature] = {
            "mean": np.mean(values),
            "std": np.std(values),
            "min": np.min(values),
            "max": np.max(values)
        }

    return statistics


def print_statistics(name, statistics):

    print()
    print("=" * 95)
    print(name)
    print("=" * 95)

    print(
        f"{'Feature':<30}"
        f"{'Mean':>12}"
        f"{'Std':>12}"
        f"{'Min':>12}"
        f"{'Max':>12}"
    )

    print("-" * 95)

    for feature, values in statistics.items():

        print(
            f"{feature:<30}"
            f"{values['mean']:>12.4f}"
            f"{values['std']:>12.4f}"
            f"{values['min']:>12.4f}"
            f"{values['max']:>12.4f}"
        )


# ============================================================
# MAIN
# ============================================================

print()
print("=" * 95)
print("VOICE SHIELD - LARGE AUDIO FEATURE ANALYSIS")
print("=" * 95)

# ------------------------------------------------------------
# REAL HUMAN RECORDING
# ------------------------------------------------------------

print()
print("1. REAL HUMAN RECORDING")
print("=" * 95)

human_features = extract_features(HUMAN_AUDIO)

for key, value in human_features.items():
    print(f"{key:<30}: {value}")


# ------------------------------------------------------------
# ASVSPOOF BONAFIDE
# ------------------------------------------------------------

print()
print("2. ASVSPOOF BONAFIDE - 50 SAMPLES")
print("=" * 95)

bonafide_results = collect_features(
    BONAFIDE_DIR,
    NUM_SAMPLES
)


# ------------------------------------------------------------
# ASVSPOOF SPOOF
# ------------------------------------------------------------

print()
print("3. ASVSPOOF SPOOF - 50 SAMPLES")
print("=" * 95)

spoof_results = collect_features(
    SPOOF_DIR,
    NUM_SAMPLES
)


# ------------------------------------------------------------
# STATISTICS
# ------------------------------------------------------------

bonafide_stats = calculate_statistics(bonafide_results)
spoof_stats = calculate_statistics(spoof_results)


print_statistics(
    "ASVSPOOF BONAFIDE STATISTICS",
    bonafide_stats
)

print_statistics(
    "ASVSPOOF SPOOF STATISTICS",
    spoof_stats
)


# ------------------------------------------------------------
# COMPARISON
# ------------------------------------------------------------

print()
print("=" * 95)
print("COMPARISON - REAL HUMAN vs ASVSPOOF")
print("=" * 95)

print(
    f"{'Feature':<30}"
    f"{'Real Human':>15}"
    f"{'ASV Bonafide':>15}"
    f"{'ASV Spoof':>15}"
)

print("-" * 95)

for feature in bonafide_stats:

    human_value = human_features[feature]
    bonafide_mean = bonafide_stats[feature]["mean"]
    spoof_mean = spoof_stats[feature]["mean"]

    print(
        f"{feature:<30}"
        f"{human_value:>15.4f}"
        f"{bonafide_mean:>15.4f}"
        f"{spoof_mean:>15.4f}"
    )


print()
print("=" * 95)
print("ANALYSIS COMPLETE")
print("=" * 95)