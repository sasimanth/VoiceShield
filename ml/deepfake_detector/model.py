"""
Deepfake / Synthetic Voice Detection Neural Network Architecture
Owner: Person 1 (AI/ML Engineer)
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
from typing import Dict, Any, Tuple, Optional


class SincConv(nn.Module):
    """SincNet parameterized bandpass filter layer for raw waveform analysis."""
    def __init__(self, out_channels: int = 70, kernel_size: int = 129, sample_rate: int = 16000):
        super().__init__()
        self.out_channels = out_channels
        self.kernel_size = kernel_size
        self.sample_rate = sample_rate

        # Initialize bandpass filter frequencies linearly spaced on Mel scale
        min_freq = 0.0
        max_freq = sample_rate / 2.0
        min_mel = 2595.0 * np.log10(1.0 + min_freq / 700.0)
        max_mel = 2595.0 * np.log10(1.0 + max_freq / 700.0)
        mel_points = np.linspace(min_mel, max_mel, out_channels + 1)
        freq_points = 700.0 * (10.0 ** (mel_points / 2595.0) - 1.0)

        self.f_low = nn.Parameter(torch.tensor(freq_points[:-1], dtype=torch.float32).view(-1, 1))
        self.f_band = nn.Parameter(torch.tensor(np.diff(freq_points), dtype=torch.float32).view(-1, 1))

        t = torch.linspace(-(kernel_size // 2), kernel_size // 2, steps=kernel_size)
        self.register_buffer("window", 0.54 - 0.46 * torch.cos(2 * np.pi * (t + kernel_size // 2) / kernel_size))
        self.register_buffer("t", 2 * np.pi * t.view(1, -1) / sample_rate)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        x shape: (batch_size, 1, num_samples)
        returns: (batch_size, out_channels, time_steps)
        """
        f_low = torch.clamp(self.f_low, min=10.0, max=self.sample_rate / 2.0 - 50.0)
        f_high = torch.clamp(f_low + torch.abs(self.f_band), min=20.0, max=self.sample_rate / 2.0)

        # Sinc bandpass formula: 2*f_high*sinc(2*f_high*t) - 2*f_low*sinc(2*f_low*t)
        low_pass_high = 2 * f_high * torch.special.sinc(f_high * self.t / np.pi)
        low_pass_low = 2 * f_low * torch.special.sinc(f_low * self.t / np.pi)
        filters = (low_pass_high - low_pass_low) * self.window
        filters = filters.view(self.out_channels, 1, self.kernel_size)

        return F.conv1d(x, filters, stride=10, padding=self.kernel_size // 2)


class DeepfakeAASISTModel(nn.Module):
    """
    Spectro-Temporal Graph Attention Network (AASIST-style) for Deepfake Speech Detection.
    Detects spectral cutoff, vocoder artifacts, and phase inconsistencies.
    """
    def __init__(self, in_channels: int = 1, num_classes: int = 2):
        super().__init__()
        self.sinc_conv = SincConv(out_channels=64, kernel_size=129)
        self.bn1 = nn.BatchNorm1d(64)
        self.pool1 = nn.MaxPool1d(kernel_size=3, stride=3)

        # Temporal & Spectral Feature Extractor Blocks
        self.conv2 = nn.Conv1d(64, 128, kernel_size=5, padding=2)
        self.bn2 = nn.BatchNorm1d(128)
        self.pool2 = nn.MaxPool1d(kernel_size=3, stride=3)

        self.conv3 = nn.Conv1d(128, 256, kernel_size=3, padding=1)
        self.bn3 = nn.BatchNorm1d(256)
        self.global_pool = nn.AdaptiveAvgPool1d(1)

        # Classifier: [Bonafide (Human), Spoof (AI Clone)]
        self.classifier = nn.Sequential(
            nn.Linear(256, 128),
            nn.LeakyReLU(0.2),
            nn.Dropout(0.3),
            nn.Linear(128, num_classes)
        )

    def forward(self, x: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        x: Raw audio tensor of shape (batch, num_samples) or (batch, 1, num_samples)
        Returns: (logits, embedding)
        """
        if x.dim() == 2:
            x = x.unsqueeze(1)

        h = F.leaky_relu(self.bn1(self.sinc_conv(x)), negative_slope=0.2)
        h = self.pool1(h)

        h = F.leaky_relu(self.bn2(self.conv2(h)), negative_slope=0.2)
        h = self.pool2(h)

        h = F.leaky_relu(self.bn3(self.conv3(h)), negative_slope=0.2)
        embedding = self.global_pool(h).squeeze(-1)

        logits = self.classifier(embedding)
        return logits, embedding
