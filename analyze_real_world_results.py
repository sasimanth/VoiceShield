import os
import numpy as np
import soundfile as sf

from ml.deepfake_detector.spectral_features import SpectralFeatureExtractor


TEST_DIR = "test_audio/real_world"

extractor = SpectralFeatureExtractor(sample_rate=16000)


# Files classified as SPOOF even though they are genuine
FALSE_POSITIVES = {
    "1089-134686-0000_16k.wav",
    "1284-1180-0000_16k.wav",
    "2300-131720-0000_16k.wav",
    "4446-2271-0000_16k.wav"
}


all_results = []
false_positive_results = []
correct_human_results = []


files = sorted([
    f for f in os.listdir(TEST_DIR)
    if f.endswith("_16k.wav")
])


for filename in files:

    path = os.path.join(TEST_DIR, filename)

    audio, sr = sf.read(path)

    if audio.ndim > 1:
        audio = np.mean(audio, axis=1)

    features = extractor.extract(audio)

    result = {
        "file": filename,
        **features
    }

    all_results.append(result)

    if filename in FALSE_POSITIVES:
        false_positive_results.append(result)
    else:
        correct_human_results.append(result)


FEATURES = [
    "spectral_centroid_hz",
    "spectral_flatness",
    "spectral_rolloff_hz",
    "zero_crossing_rate",
    "high_freq_energy_ratio",
    "acoustic_anomaly_score"
]


def print_group(title, results):

    print()
    print("=" * 75)
    print(title)
    print("=" * 75)

    print(f"Number of files: {len(results)}")

    for feature in FEATURES:

        values = [
            r[feature]
            for r in results
        ]

        print(
            f"{feature:30s} "
            f"mean={np.mean(values):.4f} "
            f"std={np.std(values):.4f} "
            f"min={np.min(values):.4f} "
            f"max={np.max(values):.4f}"
        )


print_group(
    "FALSE POSITIVES - Genuine speech predicted as SPOOF",
    false_positive_results
)

print_group(
    "CORRECT HUMAN - Genuine speech predicted as HUMAN",
    correct_human_results
)


print()
print("=" * 75)
print("INDIVIDUAL FALSE POSITIVE FILES")
print("=" * 75)

for result in false_positive_results:

    print()
    print(result["file"])

    for feature in FEATURES:
        print(
            f"  {feature:30s}: "
            f"{result[feature]}"
        )