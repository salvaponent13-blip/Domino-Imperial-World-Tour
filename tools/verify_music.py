"""Verificacion de la banda sonora generada.

Comprueba lo que si es objetivo y verificable:

  1. ALINEACION DEL BUCLE: la duracion debe ser exactamente un numero entero de
     compases. Si el bucle corta a mitad de compas, la rejilla ritmica tropieza al
     repetir por mucho fundido que se aplique.

  2. SIN CORTES NI SILENCIOS: ninguna ventana de la pista puede caer en picado
     respecto a la energia media de esa misma pista (un corte de frase o un fundido
     mal calculado se ve como un hueco aislado).

  3. NIVELES SANOS: sin recorte (pico < 1.0) y con RMS dentro de un rango audible.

La variacion de energia entre ventanas se informa como dato, no como fallo: una pista
con ritmo tiene necesariamente ventanas mas fuertes y mas suaves.
"""
import subprocess
import sys
from pathlib import Path

import numpy as np

FFMPEG = (r"C:\Users\horts\AppData\Local\Microsoft\WinGet\Packages"
          r"\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-9.0.1-full_build\bin\ffmpeg.exe")
RAW = Path("app/src/main/res/raw")
SR = 44100
WIN = 0.25           # ventana de analisis en segundos
MAX_DROP_DB = 18.0   # caida maxima admisible de una ventana frente a la media de la pista
SILENCE_DB = -50.0   # por debajo de esto se considera silencio

# Duracion de bucle esperada: compases, tiempos por compas y bpm.
# Las pistas de un solo uso (win/lose) no tienen bucle.
EXPECTED = {
    "mus_title": (12, 2.0, 92.0),
    "mus_map": (8, 4.0, 116.0),
    "mus_region_havana": (9, 4.0, 96.0),
    "mus_region_mexico": (12, 3.0, 132.0),
    "mus_region_sanjuan": (9, 4.0, 104.0),
    "mus_region_neworleans": (4, 4.0, 140.0),
    "mus_region_tokyo": (8, 4.0, 84.0),
    "mus_region_montecarlo": (8, 3.0, 150.0),
    "mus_region_lasvegas": (8, 4.0, 126.0),
    "mus_region_miami": (8, 4.0, 112.0),
    "mus_region_bronx": (8, 4.0, 92.0),
    "mus_region_losangeles": (8, 4.0, 118.0),
    "mus_tension": (4, 4.0, 100.0),
    "mus_win": None,
    "mus_lose": None,
}

# Pistas de un solo uso: se reproducen con loop = false, asi que es correcto (y
# deseable) que terminen en silencio tras su golpe final.
ONE_SHOT = {"mus_win", "mus_lose"}


def decode(path: Path) -> np.ndarray:
    out = subprocess.run(
        [FFMPEG, "-v", "error", "-i", str(path), "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"],
        capture_output=True,
    )
    if out.returncode != 0:
        raise RuntimeError(out.stderr.decode()[:200])
    return np.frombuffer(out.stdout, dtype="<i2").astype(np.float64) / 32768.0


def main() -> None:
    files = sorted(RAW.glob("mus_*.ogg"))
    print(f"{'pista':24s} {'dur':>7s} {'bucle esp.':>10s} {'error':>8s} {'rms dB':>7s} "
          f"{'pico':>6s} {'caida max':>10s}  estado")
    problems = []
    for f in files:
        name = f.stem
        x = decode(f)
        dur = x.size / SR
        rms_db = 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-12)
        peak = float(np.max(np.abs(x)))

        # Energia por ventanas, para detectar cortes o silencios aislados
        w = int(WIN * SR)
        n = max(1, x.size // w)
        rms = np.array([np.sqrt(np.mean(x[i * w:(i + 1) * w] ** 2)) for i in range(n)])
        db = 20 * np.log10(rms + 1e-12)
        mean_db = float(db.mean())
        worst_drop = float(mean_db - db.min())   # caida maxima respecto a la media
        quietest = float(db.min())

        exp = EXPECTED.get(name)
        if exp is None:
            exp_s, err_s = float("nan"), "n/a"
        else:
            bars, per_bar, bpm = exp
            exp_s = bars * per_bar * 60.0 / bpm
            err_s = f"{abs(dur - exp_s) * 1000:.1f} ms"

        estado = "OK"
        if exp is not None and abs(dur - exp_s) > 0.01:
            estado = "BUCLE DESALINEADO"
            problems.append(f"{name}: bucle {dur:.3f}s, se esperaban {exp_s:.3f}s")
        if worst_drop > MAX_DROP_DB and name not in ONE_SHOT:
            estado = "HUECO DE ENERGIA"
            problems.append(f"{name}: caida de {worst_drop:.1f} dB respecto a su media")
        if quietest < SILENCE_DB and name not in ONE_SHOT:
            estado = "SILENCIO"
            problems.append(f"{name}: ventana a {quietest:.1f} dB")
        if peak > 0.999:
            estado = "RECORTE"
            problems.append(f"{name}: pico {peak:.3f}")
        if not (-32.0 < rms_db < -6.0):
            estado = "NIVEL FUERA DE RANGO"
            problems.append(f"{name}: rms {rms_db:.1f} dB")

        print(f"{name:24s} {dur:6.2f}s {exp_s:9.3f}s {err_s:>8s} {rms_db:7.1f} "
              f"{peak:6.3f} {worst_drop:9.1f}dB  {estado}")

    total = sum(f.stat().st_size for f in files)
    print(f"\n{len(files)} pistas, {total / 1024:.1f} KB en total")
    if problems:
        print("\nPROBLEMAS:")
        for p in problems:
            print("  -", p)
        sys.exit(1)
    print("Todo correcto: bucles alineados al compas, sin cortes, sin recortes.")


if __name__ == "__main__":
    main()
