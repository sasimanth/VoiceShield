import wave
import os


# --------------------------------------------------
# VoiceShield - Audio Chunking Test
# --------------------------------------------------

input_file = "speech/test_audio/speaker1_1.wav"
output_folder = "speech/test_audio/chunks"

# Create output folder
os.makedirs(output_folder, exist_ok=True)


# Length of each chunk in seconds
chunk_duration = 2


print("\n========================================")
print("      VOICESHIELD - AUDIO CHUNKING")
print("========================================")


with wave.open(input_file, "rb") as audio:

    sample_rate = audio.getframerate()
    channels = audio.getnchannels()
    sample_width = audio.getsampwidth()

    frames_per_chunk = sample_rate * chunk_duration

    chunk_number = 1

    while True:

        frames = audio.readframes(frames_per_chunk)

        if not frames:
            break

        output_file = os.path.join(
            output_folder,
            f"chunk_{chunk_number}.wav"
        )

        with wave.open(output_file, "wb") as chunk:

            chunk.setnchannels(channels)
            chunk.setsampwidth(sample_width)
            chunk.setframerate(sample_rate)
            chunk.writeframes(frames)

        print(f"Created: {output_file}")

        chunk_number += 1


print("\nAudio chunking completed.")
print("========================================")