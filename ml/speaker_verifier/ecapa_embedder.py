import os
import logging
import numpy as np
import torch
from typing import Optional, Dict, Any

logger = logging.getLogger("voiceshield.ecapa_embedder")


class ECAPASpeakerEmbedder:
    """
    Stateless neural speaker embedding extractor powered by SpeechBrain ECAPA-TDNN.
    Produces 192-dimensional continuous speaker embeddings (Float32).
    """

    def __init__(self, model_source: str = "speechbrain/spkrec-ecapa-voxceleb", savedir: Optional[str] = None):
        self.model_source = model_source
        self.savedir = savedir or os.path.join("ml", "checkpoints", "spkrec-ecapa-voxceleb")
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        
        self.classifier = None
        self.model_loaded = False
        self.model_status_str = "UNINITIALIZED"

        self._load_model()

    def _load_model(self):
        """Loads the pre-trained SpeechBrain ECAPA-TDNN model."""
        try:
            from speechbrain.inference.speaker import EncoderClassifier
        except ImportError:
            try:
                from speechbrain.pretrained import EncoderClassifier
            except ImportError:
                self.model_status_str = "SPEECHBRAIN_NOT_INSTALLED"
                logger.error("SpeechBrain package is not installed.")
                return

        local_strategy = None
        try:
            from speechbrain.utils.fetching import LocalStrategy
            local_strategy = LocalStrategy.COPY
        except ImportError:
            pass

        try:
            logger.info("Initializing ECAPA-TDNN model from source '%s' on device '%s'...", self.model_source, self.device)
            os.makedirs(self.savedir, exist_ok=True)
            
            kwargs = {
                "source": self.model_source,
                "savedir": self.savedir,
                "run_opts": {"device": self.device}
            }
            if local_strategy is not None:
                kwargs["local_strategy"] = local_strategy

            self.classifier = EncoderClassifier.from_hparams(**kwargs)
            self.model_loaded = True
            self.model_status_str = "READY"
            logger.info("ECAPA-TDNN model loaded successfully.")
        except Exception as e:
            self.model_loaded = False
            self.model_status_str = f"LOAD_FAILED: {str(e)}"
            logger.error("Failed to load ECAPA-TDNN model checkpoint: %s", str(e))

    def extract_embedding(self, audio_data: np.ndarray, sample_rate: int = 16000) -> np.ndarray:
        """
        Extracts a 192-dimensional Float32 speaker embedding vector from preprocessed 16kHz mono audio.
        """
        if not self.model_loaded or self.classifier is None:
            raise RuntimeError(f"ECAPA-TDNN model is not loaded (status: {self.model_status_str}).")

        if audio_data is None or len(audio_data) == 0:
            raise ValueError("Audio data array is empty or None.")

        # Ensure audio is float32 1D numpy array
        if audio_data.ndim > 1:
            audio_data = np.mean(audio_data, axis=1)
        audio_data = audio_data.astype(np.float32)

        # Convert to PyTorch tensor [batch_size=1, time_samples]
        signal_tensor = torch.from_numpy(audio_data).unsqueeze(0).to(self.device)

        try:
            with torch.no_grad():
                embeddings = self.classifier.encode_batch(signal_tensor)
            
            # Squeeze batch/channel dimensions to obtain 1D array of shape (192,)
            embedding_np = embeddings.squeeze().cpu().numpy().astype(np.float32)

            # Strict dimension & finite checks
            if embedding_np.shape != (192,):
                embedding_np = embedding_np.reshape(-1)
                if embedding_np.shape != (192,):
                    raise ValueError(f"Extracted embedding dimension mismatch: expected (192,), got {embedding_np.shape}")

            if np.isnan(embedding_np).any() or np.isinf(embedding_np).any():
                raise ValueError("Extracted embedding vector contains invalid numerical values (NaN/Inf).")

            logger.info("ECAPA-TDNN speaker embedding extracted successfully: dimension=%d, status=SUCCESS", len(embedding_np))
            return embedding_np

        except Exception as e:
            logger.error("ECAPA-TDNN inference forward pass failed: %s", str(e))
            raise RuntimeError(f"ECAPA-TDNN inference failed: {str(e)}")
