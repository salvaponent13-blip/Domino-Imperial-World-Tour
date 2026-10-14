#!/usr/bin/env python3
"""Adelgaza las pistas de audio del juego sin tocar sus nombres ni su licencia.

Contexto: las pistas musicales de terceros llegan como grabaciones completas (3-7 minutos,
estereo a 44,1 kHz), lo que dejaba el APK en 46 MB. Un dominó offline no puede pesar eso.

Que hace, por cada pista musical:
  1. Decodifica y mezcla a MONO (los altavoces de movil no justifican estereo).
  2. Busca el punto MAS SILENCIOSO cerca de la duracion objetivo y corta ahi, no a ciegas:
     cortar en mitad de una frase se oye como un tropiezo al repetir.
  3. Cierra el bucle con un fundido cruzado de potencia constante entre el final y el
     arranque, para que la repeticion no tenga costura.
  4. Recodifica a OGG Vorbis a 32 kHz.

Los nombres de archivo NO cambian nunca: `AudioLicenseManifestTest` y los recursos de
`DominoMusicManager` dependen de ellos.

Los stingers cortos (victoria, derrota, tension) se dejan intactos: son de un solo uso
(`loop = false`), ya pesan poco y no deben llevar bucle.

Uso:
    python tools/slim_audio.py --dry-run      # mide sin escribir
    python tools/slim_audio.py                # aplica
    python tools/slim_audio.py --backup       # guarda copia antes de tocar
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

FFMPEG = (r"C:\Users\horts\AppData\Local\Microsoft\WinGet\Packages"
          r"\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-9.0.1-full_build\bin\ffmpeg.exe")
FFPROBE = FFMPEG.replace("ffmpeg.exe", "ffprobe.exe")

REPO = Path(__file__).resolve().parent.parent
RAW = REPO / "app" / "src" / "main" / "res" / "raw"

SAMPLE_RATE = 24000          # el contenido util de estas pistas vive por debajo de 10 kHz
TARGET_SECONDS = 65.0        # duracion objetivo del bucle
SEARCH_SECONDS = 12.0        # margen alrededor del objetivo donde buscar el punto mas silencioso
CROSSFADE_SECONDS = 2.5      # fundido cruzado de la costura del bucle
QUALITY = "3"                # calidad Vorbis

# Pistas de un solo uso: no se tocan.
ONE_SHOT = {"mus_win.ogg", "mus_lose.ogg", "mus_tension.ogg"}


def probe(path: Path) -> dict:
    out = subprocess.run(
        [FFPROBE, "-v", "error", "-show_entries", "format=duration,bit_rate",
         "-show_entries", "stream=sample_rate,channels", "-of", "json", str(path)],
        capture_output=True, text=True, check=True,
    )
    return json.loads(out.stdout)


def decode_mono(path: Path) -> np.ndarray:
    """Decodifica a float mono al sample rate de trabajo, con ffmpeg."""
    out = subprocess.run(
        [FFMPEG, "-v", "error", "-i", str(path),
         "-f", "f32le", "-ac", "1", "-ar", str(SAMPLE_RATE), "-"],
        capture_output=True, check=True,
    )
    return np.frombuffer(out.stdout, dtype="<f4").astype(np.float64)


def quietest_cut(samples: np.ndarray, target_s: float, search_s: float, window_s: float = 0.12) -> int:
    """Indice de corte en la ventana de energia mas baja cerca del objetivo.

    Cortar donde el material esta mas callado reduce muchisimo la probabilidad de partir una
    frase o una nota por la mitad.
    """
    n = samples.size
    target = int(target_s * SAMPLE_RATE)
    window = int(window_s * SAMPLE_RATE)
    lo = max(0, target - int(search_s * SAMPLE_RATE))
    hi = min(n - 1, target + int(search_s * SAMPLE_RATE))
    if hi - lo <= window:
        return min(target, n)

    hop = max(1, window // 4)
    best_idx = target
    best_energy = None
    idx = lo
    while idx + window < hi:
        energy = float(np.mean(samples[idx:idx + window] ** 2))
        if best_energy is None or energy < best_energy:
            best_energy = energy
            best_idx = idx
        idx += hop
    # El corte se pone en el centro de la ventana silenciosa, no en su arranque.
    return min(best_idx + window // 2, n)


def loop_with_crossfade(samples: np.ndarray, fade_s: float) -> np.ndarray:
    """Cierra el bucle fundiendo el final sobre el arranque con potencia constante.

    Igual que en el generador procedural: un fundido lineal sumaria amplitudes y hundiria
    unos 3 dB la union, lo que se oye como un bache al repetir.
    """
    fade = min(int(fade_s * SAMPLE_RATE), samples.size // 4)
    if fade <= 0:
        return samples
    head = samples[:fade].copy()
    tail = samples[samples.size - fade:]
    phase = np.linspace(0.0, np.pi / 2.0, fade)
    samples = samples[:samples.size - fade].copy()
    samples[:fade] = samples[:fade] * np.sin(phase) + tail * np.cos(phase)
    return samples


def encode(samples: np.ndarray, path: Path, tmp_wav: Path) -> None:
    peak = float(np.max(np.abs(samples))) or 1.0
    pcm = np.clip(samples / peak * 0.89, -1.0, 1.0)
    pcm = (pcm * 32767.0).astype("<i2")
    with wave.open(str(tmp_wav), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(pcm.tobytes())
    subprocess.run(
        [FFMPEG, "-y", "-v", "error", "-i", str(tmp_wav),
         "-c:a", "libvorbis", "-q:a", QUALITY, str(path)],
        check=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="mide sin escribir nada")
    parser.add_argument("--backup", action="store_true", help="guarda copia .bak antes de tocar")
    parser.add_argument("--target", type=float, default=TARGET_SECONDS)
    args = parser.parse_args()

    tmp_wav = REPO / "build" / "slim_tmp.wav"
    tmp_wav.parent.mkdir(parents=True, exist_ok=True)

    files = sorted(RAW.glob("mus_*.ogg"))
    if not files:
        sys.exit(f"ERROR: no hay pistas en {RAW}")

    total_before = 0
    total_after = 0
    print(f"{'pista':26s} {'antes':>9s} {'despues':>9s} {'dur':>7s}  accion")
    for f in files:
        size_before = f.stat().st_size
        total_before += size_before
        info = probe(f)
        dur = float(info.get("format", {}).get("duration", 0) or 0)

        if f.name in ONE_SHOT:
            total_after += size_before
            print(f"{f.name:26s} {size_before/1024:8.0f}K {'-':>9s} {dur:6.1f}s  intacta (un solo uso)")
            continue

        if args.dry_run:
            # Estimacion: mismo bitrate efectivo tratado como objetivo
            samples = decode_mono(f)
            cut = quietest_cut(samples, args.target, SEARCH_SECONDS)
            looped = loop_with_crossfade(samples[:cut], CROSSFADE_SECONDS)
            estimated = int(looped.size / SAMPLE_RATE * 32000 / 8 * 1.05)
            total_after += estimated
            print(f"{f.name:26s} {size_before/1024:8.0f}K {estimated/1024:8.0f}K "
                  f"{looped.size/SAMPLE_RATE:6.1f}s  recortar a {looped.size/SAMPLE_RATE:.0f}s")
            continue

        if args.backup:
            shutil.copy2(f, f.with_suffix(".ogg.bak"))

        samples = decode_mono(f)
        cut = quietest_cut(samples, args.target, SEARCH_SECONDS)
        looped = loop_with_crossfade(samples[:cut], CROSSFADE_SECONDS)
        encode(looped, f, tmp_wav)

        size_after = f.stat().st_size
        total_after += size_after
        print(f"{f.name:26s} {size_before/1024:8.0f}K {size_after/1024:8.0f}K "
              f"{looped.size/SAMPLE_RATE:6.1f}s  bucle de {looped.size/SAMPLE_RATE:.0f}s")

    tmp_wav.unlink(missing_ok=True)
    print(f"\nTOTAL: {total_before/1024/1024:.2f} MB -> {total_after/1024/1024:.2f} MB "
          f"(ahorro {(total_before-total_after)/1024/1024:.2f} MB)")
    if args.dry_run:
        print("(simulacion: no se ha escrito nada)")


if __name__ == "__main__":
    main()
