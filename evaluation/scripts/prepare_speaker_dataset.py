import os
import sys
import json
import random
import logging
from typing import List, Dict, Any, Tuple

logger = logging.getLogger("voiceshield.evaluation.dataset_prep")

def discover_speaker_files(dataset_dir: str) -> Dict[str, List[str]]:
    """
    Discovers speaker audio files in a dataset directory.
    Expected structure: dataset_dir/speaker_id/*.wav (or flac/mp3)
    Returns dict mapping speaker_id to list of audio file paths.
    """
    valid_exts = {".wav", ".flac", ".mp3", ".ogg"}
    speaker_map = {}

    if not os.path.exists(dataset_dir):
        logger.error("Dataset directory does not exist: %s", dataset_dir)
        return speaker_map

    for entry in os.scandir(dataset_dir):
        if entry.is_dir():
            speaker_id = entry.name
            audio_files = []
            for root, _, files in os.walk(entry.path):
                for f in files:
                    ext = os.path.splitext(f)[1].lower()
                    if ext in valid_exts:
                        audio_files.append(os.path.join(root, f))
            if len(audio_files) >= 2:
                speaker_map[speaker_id] = sorted(audio_files)

    return speaker_map


def split_speakers(speaker_ids: List[str], dev_ratio: float = 0.60, seed: int = 42) -> Tuple[List[str], List[str]]:
    """
    Splits speakers into Calibration/Development and Held-Out sets.
    Enforces strict zero speaker leakage.
    """
    random.seed(seed)
    shuffled = sorted(speaker_ids)
    random.shuffle(shuffled)

    num_dev = int(round(len(shuffled) * dev_ratio))
    dev_speakers = shuffled[:num_dev]
    heldout_speakers = shuffled[num_dev:]

    return dev_speakers, heldout_speakers


def generate_trials(
    speaker_map: Dict[str, List[str]], 
    speakers: List[str], 
    num_genuine_per_speaker: int = 5, 
    num_impostor_per_speaker: int = 5,
    seed: int = 42
) -> List[Dict[str, Any]]:
    """
    Generates deterministic Genuine (A1<->A2) and Impostor (A1<->B1) trial pairs.
    """
    random.seed(seed)
    trials = []
    speaker_list = sorted(speakers)

    for spk in speaker_list:
        files = speaker_map[spk]
        num_files = len(files)

        # 1. Genuine Trials (A1 <-> A2)
        gen_count = 0
        for i in range(num_files):
            for j in range(i + 1, num_files):
                trials.append({
                    "trial_id": f"GEN_{spk}_{i}_{j}",
                    "speaker_id": spk,
                    "claimed_speaker_id": spk,
                    "ref_audio": files[i],
                    "query_audio": files[j],
                    "label": "GENUINE",
                    "is_target": True
                })
                gen_count += 1
                if gen_count >= num_genuine_per_speaker:
                    break
            if gen_count >= num_genuine_per_speaker:
                break

        # 2. Impostor Trials (A1 <-> B1)
        imp_count = 0
        other_speakers = [s for s in speaker_list if s != spk]
        if other_speakers:
            target_impostors = random.sample(other_speakers, min(num_impostor_per_speaker, len(other_speakers)))
            for imp_spk in target_impostors:
                ref_file = random.choice(files)
                query_file = random.choice(speaker_map[imp_spk])
                trials.append({
                    "trial_id": f"IMP_{spk}_vs_{imp_spk}",
                    "speaker_id": spk,
                    "claimed_speaker_id": imp_spk,
                    "ref_audio": ref_file,
                    "query_audio": query_file,
                    "label": "IMPOSTOR",
                    "is_target": False
                })
                imp_count += 1

    return trials


def prepare_dataset_manifests(
    dataset_name: str,
    dataset_dir: str,
    output_dir: str,
    dev_ratio: float = 0.60,
    seed: int = 42
) -> Dict[str, Any]:
    """
    Prepares dataset trial manifests for calibration and held-out evaluation.
    """
    speaker_map = discover_speaker_files(dataset_dir)
    speaker_ids = list(speaker_map.keys())

    if not speaker_ids:
        return {
            "status": "NO_DATA",
            "message": f"No valid multi-utterance speakers found in dataset directory: {dataset_dir}"
        }

    dev_speakers, heldout_speakers = split_speakers(speaker_ids, dev_ratio=dev_ratio, seed=seed)

    dev_trials = generate_trials(speaker_map, dev_speakers, seed=seed)
    heldout_trials = generate_trials(speaker_map, heldout_speakers, seed=seed + 1)

    os.makedirs(output_dir, exist_ok=True)

    dev_path = os.path.join(output_dir, "calibration_trials.json")
    heldout_path = os.path.join(output_dir, "heldout_trials.json")
    meta_path = os.path.join(output_dir, "dataset_metadata.json")

    with open(dev_path, "w") as f:
        json.dump(dev_trials, f, indent=2)

    with open(heldout_path, "w") as f:
        json.dump(heldout_trials, f, indent=2)

    metadata = {
        "dataset_name": dataset_name,
        "dataset_dir": dataset_dir,
        "total_speakers": len(speaker_ids),
        "calibration_speakers_count": len(dev_speakers),
        "heldout_speakers_count": len(heldout_speakers),
        "calibration_trials_count": len(dev_trials),
        "heldout_trials_count": len(heldout_trials),
        "random_seed": seed,
        "dev_ratio": dev_ratio,
        "zero_speaker_leakage_verified": True
    }

    with open(meta_path, "w") as f:
        json.dump(metadata, f, indent=2)

    logger.info("Dataset manifests generated at '%s': %d dev trials, %d heldout trials", output_dir, len(dev_trials), len(heldout_trials))
    return {
        "status": "SUCCESS",
        "metadata": metadata
    }


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print("Speaker Verification Dataset Preparation Framework ready.")
