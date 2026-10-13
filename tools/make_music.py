#!/usr/bin/env python3
"""Generador de la banda sonora original de Domino Imperial: World Tour.

Toda la musica se sintetiza desde cero con numpy (osciladores, envolventes y ruido
filtrado) y se codifica a OGG Vorbis con ffmpeg. No se usa MIDI, ni SoundFonts, ni
samples de terceros: la autoria y los derechos son integros del proyecto.

Uso:
    python tools/make_music.py            # desde la raiz del repositorio
    python tools/make_music.py --only cuba tokio

Escribe los .ogg en app/src/main/res/raw/ y unos espectrogramas de comprobacion en
build/spectrograms/ (ignorado por git) para poder verificar el resultado sin oir.
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

RENDER_SR = 32000
# Frecuencia de muestreo del render. La musica de fondo no necesita 44,1 kHz: todo el
# contenido util (bajos, percusion, armonicos) vive por debajo de 10 kHz, asi que
# renderizar a 32 kHz reduce el peso del OGG sin perdida apreciable en altavoces de movil.
REPO = Path(__file__).resolve().parent.parent
RAW_DIR = REPO / "app" / "src" / "main" / "res" / "raw"
PROOF_DIR = REPO / "build" / "spectrograms"

# ---------------------------------------------------------------------------
# Localizacion de ffmpeg
# ---------------------------------------------------------------------------

FFMPEG_CANDIDATES = [
    shutil.which("ffmpeg"),
    r"C:\Users\horts\AppData\Local\Microsoft\WinGet\Packages"
    r"\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-9.0.1-full_build\bin\ffmpeg.exe",
]


def find_ffmpeg() -> str:
    for candidate in FFMPEG_CANDIDATES:
        if candidate and Path(candidate).exists():
            return str(candidate)
    sys.exit("ERROR: no encuentro ffmpeg. Anadelo al PATH o a FFMPEG_CANDIDATES.")


# ---------------------------------------------------------------------------
# Teoria musical minima
# ---------------------------------------------------------------------------

A4 = 440.0


def midi_to_hz(note: float) -> float:
    """Frecuencia en Hz de una nota MIDI (admite decimales para microtonos)."""
    return A4 * (2.0 ** ((note - 69.0) / 12.0))


def scale(root: int, intervals: tuple[int, ...]) -> list[int]:
    return [root + i for i in intervals]


MAJOR = (0, 2, 4, 5, 7, 9, 11)
MINOR = (0, 2, 3, 5, 7, 8, 10)
PENTA_MINOR = (0, 3, 5, 7, 10)
PENTA_MAJOR = (0, 2, 4, 7, 9)
DORIAN = (0, 2, 3, 5, 7, 9, 10)


def triad(root: int, kind: str = "min") -> list[int]:
    third = 4 if kind == "maj" else 3
    return [root, root + third, root + 7]


def seventh(root: int, kind: str = "min7") -> list[int]:
    table = {
        "min7": (0, 3, 7, 10),
        "maj7": (0, 4, 7, 11),
        "dom7": (0, 4, 7, 10),
        "min6": (0, 3, 7, 9),
        "dim7": (0, 3, 6, 9),
    }
    return [root + i for i in table[kind]]


# ---------------------------------------------------------------------------
# Motor de sintesis
# ---------------------------------------------------------------------------


class Score:
    """Acumula voces sobre un buffer temporal."""

    # Margen de render mas alla del final del bucle. Debe cubrir el decaimiento mas
    # largo que pueda quedar sonando en el ultimo tiempo (pads, reverbs, colas de
    # platos); si se queda corto, esas notas se cortan en seco en el punto de bucle.
    TAIL_SECONDS = 4.0

    def __init__(self, seconds: float, bpm: float) -> None:
        self.seconds = seconds
        self.bpm = bpm
        self.beat = 60.0 / bpm
        self.length = int(RENDER_SR * (seconds + self.TAIL_SECONDS))
        self.buffer = np.zeros(self.length, dtype=np.float64)

    def t(self, beats: float) -> float:
        return beats * self.beat

    def set_bars(self, bars: int, beats_per_bar: float = 4.0) -> float:
        """Fija el bucle a un numero exacto de compases.

        Es imprescindible que el punto de repeticion caiga en un limite de compas: si el
        bucle corta a mitad de compas, al volver al principio la rejilla ritmica se
        desalinea y se oye un tropiezo, por mucho fundido que se aplique.
        """
        self.seconds = bars * beats_per_bar * self.beat
        total = int(self.seconds * RENDER_SR)
        self.buffer = np.zeros(total + int(RENDER_SR * self.TAIL_SECONDS), dtype=np.float64)
        self.length = self.buffer.size
        return self.seconds

    # -- voces ---------------------------------------------------------------

    def pluck(
        self,
        note: float,
        start_beat: float,
        dur_beats: float,
        gain: float = 0.3,
        decay: float = 5.0,
        harmonics: tuple[float, ...] = (1.0, 0.32, 0.12),
        detune: float = 0.0,
        tremolo: float = 0.0,
    ) -> None:
        """Pulsacion generica: cuerda pulsada, tecla o marimba."""
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(self.t(dur_beats + 0.3) * RENDER_SR)
        if start >= self.length or n <= 0:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        freq = midi_to_hz(note)
        env = np.exp(-decay * tt) * (1.0 - np.exp(-400.0 * tt))
        if tremolo > 0.0:
            env *= 1.0 + tremolo * np.sin(2 * np.pi * 5.5 * tt)
        sig = np.zeros(n)
        for i, amp in enumerate(harmonics):
            partial = freq * (i + 1) * (1.0 + detune * (i + 1))
            if partial < RENDER_SR / 2:
                sig += amp * np.sin(2 * np.pi * partial * tt)
        self.buffer[start:start + n] += gain * env * sig

    def pad(
        self,
        note: float,
        start_beat: float,
        dur_beats: float,
        gain: float = 0.2,
        attack: float = 0.25,
        release: float = 0.6,
        voices: int = 3,
    ) -> None:
        """Cuerda sostenida con desafinado y vibrato suave."""
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(self.t(dur_beats + release) * RENDER_SR)
        if start >= self.length or n <= 0:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        freq = midi_to_hz(note)
        env = np.minimum(1.0, tt / max(1e-3, attack)) * np.exp(-tt / max(0.4, release * 3))
        sig = np.zeros(n)
        for v in range(voices):
            spread = 1.0 + (v - (voices - 1) / 2.0) * 0.004
            vib = 1.0 + 0.0025 * np.sin(2 * np.pi * (4.5 + v) * tt)
            sig += np.sin(2 * np.pi * freq * spread * vib * tt)
        sig /= voices
        self.buffer[start:start + n] += gain * env * sig

    def bass(self, note: float, start_beat: float, dur_beats: float, gain: float = 0.5) -> None:
        self.pluck(note, start_beat, dur_beats, gain=gain, decay=2.2,
                   harmonics=(1.0, 0.28, 0.06))

    def brass(self, note: float, start_beat: float, dur_beats: float, gain: float = 0.24) -> None:
        self.pluck(note, start_beat, dur_beats, gain=gain, decay=2.0,
                   harmonics=(1.0, 0.5, 0.35, 0.2, 0.1), tremolo=0.05)

    # -- percusion -----------------------------------------------------------

    def _noise(self, n: int) -> np.ndarray:
        return np.random.default_rng(7).normal(0.0, 1.0, n)

    def kick(self, start_beat: float, gain: float = 0.85) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(0.28 * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        freq = 120.0 * np.exp(-24.0 * tt) + 44.0
        env = np.exp(-13.0 * tt)
        self.buffer[start:start + n] += gain * env * np.sin(2 * np.pi * np.cumsum(freq) / RENDER_SR)

    def conga(self, start_beat: float, tone: float = 220.0, gain: float = 0.45) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(0.22 * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        env = np.exp(-17.0 * tt)
        sig = np.sin(2 * np.pi * tone * tt) + 0.4 * np.sin(2 * np.pi * tone * 1.6 * tt)
        self.buffer[start:start + n] += gain * env * sig

    def clave(self, start_beat: float, gain: float = 0.4, tone: float = 2200.0) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(0.07 * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        env = np.exp(-60.0 * tt)
        sig = np.sin(2 * np.pi * tone * tt) + 0.5 * np.sin(2 * np.pi * tone * 1.5 * tt)
        self.buffer[start:start + n] += gain * env * sig

    def snare(self, start_beat: float, gain: float = 0.4) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(0.18 * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        env = np.exp(-26.0 * tt)
        noise = self._noise(n)
        body = 0.4 * np.sin(2 * np.pi * 190.0 * tt)
        self.buffer[start:start + n] += gain * env * (noise * 0.7 + body)

    def hat(self, start_beat: float, gain: float = 0.2, open_hat: bool = False) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int((0.22 if open_hat else 0.05) * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        env = np.exp(-(30.0 if open_hat else 90.0) * tt)
        # Ruido "brillante": diferenciado para inclinar el espectro a agudos
        noise = np.diff(self._noise(n + 1))
        self.buffer[start:start + n] += gain * env * noise

    def shaker(self, start_beat: float, gain: float = 0.14) -> None:
        start = int(self.t(start_beat) * RENDER_SR)
        n = int(0.09 * RENDER_SR)
        if start >= self.length:
            return
        n = min(n, self.length - start)
        tt = np.arange(n) / RENDER_SR
        env = np.sin(np.pi * tt / (n / RENDER_SR)) ** 2
        self.buffer[start:start + n] += gain * env * np.diff(self._noise(n + 1))

    def timbale(self, start_beat: float, gain: float = 0.3) -> None:
        self.conga(start_beat, tone=330.0, gain=gain)

    # -- post-proceso --------------------------------------------------------

    def apply_space(self, wet: float = 0.18, delay_s: float = 0.11) -> None:
        """Reverb barato: dos delays realimentados para dar aire sin coste."""
        if wet <= 0:
            return
        base = self.buffer.copy()
        for factor in (1.0, 1.6):
            d = int(delay_s * factor * RENDER_SR)
            if d <= 0 or d >= self.length:
                continue
            tail = np.zeros_like(base)
            tail[d:] = base[:-d]
            fb = np.zeros_like(base)
            for _ in range(3):
                fb = tail + 0.35 * np.roll(fb, d)
            self.buffer += wet * fb

    def loopify(self, crossfade_seconds: float = 0.15) -> None:
        """Convierte el render en un bucle sin costura.

        Se compone mas alla del final del bucle y la energia de esa cola se funde sobre
        el arranque. Dos detalles importan:

        - El fundido es de POTENCIA CONSTANTE (coseno/seno). Un fundido lineal, al
          mezclar fragmentos no correlacionados, sumaria amplitudes y hundiria unos 3 dB
          la energia de la union, oyendose como un bache en cada repeticion.
        - Es CORTO (~150 ms). Los bucles ya estan alineados al compas, asi que final y
          principio coinciden en la rejilla ritmica; alargar el fundido solo mezcla
          material de fase muy distinta y emborrona el ataque del downbeat.

        El margen de render ([TAIL_SECONDS]) garantiza que ninguna nota quede cortada a
        mitad de decaimiento.
        """
        total = int(self.seconds * RENDER_SR)
        body = self.buffer[:total].copy()
        fade = min(int(crossfade_seconds * RENDER_SR), total // 5)
        if fade <= 0:
            self.buffer = body
            return
        tail = self.buffer[total:total + fade]
        phase = np.linspace(0.0, np.pi / 2.0, fade)
        body[:fade] = body[:fade] * np.sin(phase) + tail * np.cos(phase)
        self.buffer = body

    def finish(self, peak: float = 0.89) -> np.ndarray:
        out = self.buffer
        top = np.max(np.abs(out))
        if top > 0:
            out = out / top * peak
        # Fundido de entrada muy corto para evitar clics al iniciar la pista
        fade = int(0.005 * RENDER_SR)
        if out.size > fade:
            out[:fade] *= np.linspace(0.0, 1.0, fade)
        return out


# ---------------------------------------------------------------------------
# Patrones ritmicos
# ---------------------------------------------------------------------------

# Clave son 3-2, en negras dentro de un ciclo de 8
CLAVE_SON = (0, 1.5, 3, 4, 6)
# Clave rumba 3-2
CLAVE_RUMBA = (0, 2, 3, 4, 6)
# Cáscara de son sobre 8 negras
CASCARA = (0, 1.5, 2.5, 4, 5, 6.5)


def son_percussion(score: Score, bars: int = 4, with_conga: bool = True) -> None:
    """Tumbao de conga + clave + cascara sobre un ciclo de 2 compases."""
    for bar in range(bars):
        base = bar * 4
        if bar % 2 == 0:
            for off in CLAVE_SON:
                score.clave(base + off)
        for off in CASCARA:
            score.hat(base + off, gain=0.1)
        if with_conga:
            # Tumbao: grave en el "y" del 2 y del 4, agudo adelantado
            for off in (0.5, 1.5, 2.5, 3.5):
                score.conga(base + off, tone=196.0, gain=0.3)
            for off in (1.0, 3.0):
                score.conga(base + off, tone=294.0, gain=0.26)
            score.timbale(base + 3.5, gain=0.22)


def swing_eighths(beat: float) -> float:
    """Convierte una corchea recta en corchea de swing (2:1)."""
    whole = int(beat)
    frac = beat - whole
    if abs(frac - 0.5) < 1e-6:
        return whole + 2.0 / 3.0
    return beat


# ---------------------------------------------------------------------------
# Composiciones
# ---------------------------------------------------------------------------


def track_title() -> tuple[Score, str]:
    """Menu principal: tema imperial en 6/8, solemne y con aire de gala."""
    bpm = 92.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(12, 2.0)   # 12 compases de 6/8
    # 6/8 -> el "beat" interno es la corchea; 6 corcheas por compas
    lead = scale(57, MINOR)  # A menor
    # Fanfarria de apertura (Do mayor relativo, brillante)
    fanfare = [(72, 0.0, 1.0), (76, 1.0, 1.0), (79, 2.0, 2.0), (76, 4.0, 1.0), (72, 5.0, 1.0)]
    for note, off, dur in fanfare:
        for v in (0.0, 2.0, 4.0):
            sc.brass(note, off + v, dur * 0.9, gain=0.2)
    # Melodia principal, dos frases
    phrase_a = [(69, 0.0, 2.0), (72, 2.0, 1.0), (76, 3.0, 2.0), (74, 5.0, 1.0),
                (72, 6.0, 2.0), (69, 8.0, 1.0), (67, 9.0, 2.0), (69, 11.0, 1.0)]
    phrase_b = [(72, 12.0, 2.0), (76, 14.0, 1.0), (79, 15.0, 2.0), (77, 17.0, 1.0),
                (76, 18.0, 2.0), (72, 20.0, 1.0), (69, 21.0, 3.0)]
    for phrase in (phrase_a, phrase_b):
        for note, off, dur in phrase:
            sc.pluck(note, off, dur, gain=0.26, decay=1.6, harmonics=(1.0, 0.4, 0.2, 0.1))
            sc.pluck(note - 12, off, dur, gain=0.12, decay=1.8)
    # Armonia en cuerdas
    chords = [(45, "min", 0.0), (41, "maj", 6.0), (43, "maj", 12.0), (40, "maj", 18.0)]
    for root, kind, off in chords:
        for note in triad(root, kind) + [root + 12]:
            sc.pad(note, off, 6.0, gain=0.07, attack=0.5, release=1.2)
        for note in triad(root + 12, kind):
            sc.pad(note, off, 6.0, gain=0.04, attack=0.6, release=1.2)
    # Bajo en negras con puntillo
    bass_line = [45, 45, 41, 41, 43, 43, 40, 40]
    for i, note in enumerate(bass_line):
        sc.bass(note, i * 3.0, 2.6, gain=0.42)
    # Percusion de gala
    for bar in range(4):
        base = bar * 6.0
        sc.kick(base, gain=0.6)
        sc.kick(base + 3.0, gain=0.45)
        sc.snare(base + 3.0, gain=0.22)
        for off in (0.0, 1.0, 2.0, 3.0, 4.0, 5.0):
            sc.shaker(base + off, gain=0.09)
    sc.apply_space(wet=0.24, delay_s=0.13)
    return sc, "mus_title"


def track_map() -> tuple[Score, str]:
    """Mapa del mundo: viaje optimista, arpegio brillante y ritmo suave."""
    bpm = 116.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8)
    progression = [(53, "maj"), (48, "maj"), (55, "maj"), (50, "min")]
    # Arpegio continuo de corcheas
    for bar in range(8):
        root, kind = progression[bar % 4]
        notes = triad(root + 12, kind)
        for i in range(8):
            note = notes[i % 3] + (12 if i >= 6 else 0)
            sc.pluck(note, bar * 4 + i * 0.5, 0.5, gain=0.17, decay=6.0,
                     harmonics=(1.0, 0.3, 0.15))
    # Melodia simple por encima
    melody = [(77, 0.0, 1.5), (76, 2.0, 1.0), (74, 3.0, 1.0), (72, 4.0, 2.0),
              (76, 8.0, 1.5), (74, 10.0, 1.0), (72, 11.0, 1.0), (71, 12.0, 3.0)]
    for note, off, dur in melody:
        sc.brass(note, off, dur, gain=0.16)
    for note, off, dur in melody:
        sc.brass(note, off + 16.0, dur, gain=0.16)
    # Base armonica y bajo
    for bar in range(8):
        root, kind = progression[bar % 4]
        sc.pad(root + 12, bar * 4, 4.0, gain=0.08, attack=0.4, release=0.8)
        sc.pad(root + 19, bar * 4, 4.0, gain=0.05, attack=0.5, release=0.8)
        sc.bass(root - 12, bar * 4, 1.8, gain=0.45)
        sc.bass(root - 5, bar * 4 + 2.0, 1.8, gain=0.38)
    # Ritmo ligero tipo viaje
    for bar in range(8):
        base = bar * 4
        sc.kick(base, gain=0.55)
        sc.kick(base + 2.0, gain=0.4)
        sc.snare(base + 2.0, gain=0.2)
        for i in range(8):
            sc.shaker(base + i * 0.5, gain=0.11 if i % 2 else 0.07)
    sc.apply_space(wet=0.2, delay_s=0.1)
    return sc, "mus_map"


def track_region_havana() -> tuple[Score, str]:
    """La Habana: son cubano con tumbao, clave 3-2 y tres."""
    bpm = 96.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(9, 4.0)
    son_percussion(sc, bars=9)
    # Tumbao del bajo: raiz en el "y" del 2 y del 4
    roots = [41, 41, 46, 46]
    for bar in range(9):
        base = bar * 4
        root = roots[bar % 4]
        sc.bass(root, base + 1.5, 1.0, gain=0.55)
        sc.bass(root, base + 3.0, 0.9, gain=0.5)
        sc.bass(root + 7, base + 0.5, 0.6, gain=0.3)
    # Montuno del tres: arpegio sincopado sobre Fm - Bbm
    montuno = [(65, 0.0), (68, 0.5), (70, 1.5), (68, 2.0), (65, 2.5), (63, 3.5)]
    for bar in range(9):
        base = bar * 4
        shift = 0 if bar % 4 < 2 else 5
        for note, off in montuno:
            sc.pluck(note + shift, base + off, 0.45, gain=0.2, decay=7.0,
                     harmonics=(1.0, 0.35, 0.18, 0.08))
    # Melodia de trompeta, dos frases de 4 compases
    call = [(72, 0.0, 1.0), (70, 1.0, 0.5), (68, 1.5, 1.5), (65, 3.0, 1.0)]
    response = [(68, 0.0, 1.0), (70, 1.0, 0.5), (72, 1.5, 2.5)]
    for bar in range(4):
        base = 16.0 + bar * 4
        for note, off, dur in (call if bar % 2 == 0 else response):
            sc.brass(note, base + off, dur, gain=0.2)
    sc.apply_space(wet=0.16, delay_s=0.09)
    return sc, "mus_region_havana"


def track_region_mexico() -> tuple[Score, str]:
    """Ciudad de Mexico: aire de mariachi en 3/4 con guitarrón y trompeta."""
    bpm = 132.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(12, 3.0)
    # Marcamos el compas de 3/4 con 3 negras
    progression = [(50, "maj"), (55, "maj"), (57, "min"), (50, "maj")]
    for bar in range(12):
        base = bar * 3.0
        root, kind = progression[bar % 4]
        # Guitarrón: fundamental + quinta alternando
        sc.bass(root - 12, base, 1.4, gain=0.5)
        sc.bass(root - 5, base + 1.5, 1.3, gain=0.36)
        # Vihuela: rasgueo en contratiempo
        for note in triad(root, kind if kind == "maj" else "min"):
            for off in (0.5, 1.0, 2.0):
                sc.pluck(note + 12, base + off, 0.3, gain=0.09, decay=9.0)
        for off in (0.0, 1.0, 2.0):
            sc.hat(base + off, gain=0.08)
    # Trompeta: melodia con saltos amplios
    melody = [(74, 0.0, 1.5), (72, 1.5, 1.0), (69, 2.5, 0.5), (71, 3.0, 2.0),
              (74, 6.0, 1.0), (76, 7.0, 2.0), (74, 9.0, 2.5), (69, 12.0, 3.0)]
    for repeat in range(2):
        for note, off, dur in melody:
            sc.brass(note, off + repeat * 15.0, dur, gain=0.22)
    for note, off, dur in melody:
        sc.brass(note - 12, off, dur, gain=0.08)
    sc.apply_space(wet=0.15, delay_s=0.08)
    return sc, "mus_region_mexico"


def track_region_sanjuan() -> tuple[Score, str]:
    """San Juan: bomba caribeña, tambores y llamada-respuesta."""
    bpm = 104.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(9, 4.0)
    # Tambores de bomba: patron de 4 compases
    for bar in range(9):
        base = bar * 4
        for off in (0.0, 0.75, 1.5, 2.5, 3.25):
            sc.conga(base + off, tone=180.0, gain=0.34)
        for off in (1.0, 2.0, 3.5):
            sc.conga(base + off, tone=260.0, gain=0.3)
        sc.timbale(base + 3.0, gain=0.24)
        if bar % 2 == 0:
            for off in CLAVE_RUMBA:
                sc.clave(base + off, gain=0.3)
    # Bajo con raices alternando
    roots = [38, 38, 43, 45]
    for bar in range(9):
        base = bar * 4
        root = roots[bar % 4]
        sc.bass(root, base, 1.6, gain=0.5)
        sc.bass(root + 7, base + 2.0, 1.6, gain=0.4)
    # Coro en cuerdas y llamada
    for bar in range(4):
        base = bar * 4
        for note in (62, 65, 69):
            sc.pad(note, base, 3.6, gain=0.06, attack=0.3, release=0.7)
    calls = [(69, 0.0, 1.0), (72, 1.0, 0.5), (74, 1.5, 2.0),
             (72, 8.0, 1.0), (69, 9.0, 1.0), (67, 10.0, 2.0)]
    for note, off, dur in calls:
        sc.pluck(note, off + 2.0, dur, gain=0.22, decay=3.0, harmonics=(1.0, 0.4, 0.2))
    for note, off, dur in calls:
        sc.pluck(note, off + 20.0, dur, gain=0.2, decay=3.0, harmonics=(1.0, 0.4, 0.2))
    sc.apply_space(wet=0.17, delay_s=0.1)
    return sc, "mus_region_sanjuan"


def track_region_neworleans() -> tuple[Score, str]:
    """Nueva Orleans: jazz con swing, walking bass y séptimas."""
    bpm = 140.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(4, 4.0)
    # Walking bass en negras
    walk = [36, 40, 43, 45, 48, 45, 43, 41,
            38, 41, 45, 47, 50, 47, 45, 43,
            36, 40, 43, 45, 48, 52, 50, 48,
            41, 44, 47, 50, 43, 46, 48, 50]
    for i, note in enumerate(walk):
        sc.bass(note, float(i), 0.95, gain=0.45)
    # Bateria con swing y escobillas
    for bar in range(4):
        base = bar * 4.0
        for i in range(4):
            sc.hat(base + i, gain=0.1)
            sc.hat(swing_eighths(base + i + 0.5), gain=0.07)
        sc.kick(base, gain=0.42)
        sc.kick(base + 2.5, gain=0.34)
        sc.snare(base + 1.0, gain=0.16)
        sc.snare(base + 3.0, gain=0.18)
    # Piano con acordes en contratiempo
    chords = [(53, "min7"), (55, "dom7"), (53, "min7"), (48, "maj7")]
    for bar in range(4):
        base = bar * 4.0
        root, kind = chords[bar % 4]
        for off in (0.0, 1.5, 2.5):
            for note in seventh(root + 12, kind):
                sc.pluck(note, base + off, 0.6, gain=0.11, decay=7.0,
                         harmonics=(1.0, 0.35, 0.15))
    # Melodia con swing
    melody_straight = [(72, 0.0, 1.0), (74, 1.0, 0.5), (75, 1.5, 0.5), (77, 2.0, 2.0),
                       (75, 4.0, 1.0), (74, 5.0, 0.5), (72, 5.5, 2.5),
                       (70, 8.0, 1.0), (72, 9.0, 0.5), (74, 9.5, 0.5), (72, 10.0, 3.0)]
    for note, off, dur in melody_straight:
        sc.brass(note, swing_eighths(off) if off % 1 else off, dur, gain=0.2)
    for note, off, dur in melody_straight:
        sc.brass(note, (swing_eighths(off) if off % 1 else off) + 14.0, dur, gain=0.18)
    sc.apply_space(wet=0.2, delay_s=0.11)
    return sc, "mus_region_neworleans"


def track_region_tokyo() -> tuple[Score, str]:
    """Tokio: escala pentatónica en modo koto, textura contemplativa."""
    bpm = 84.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 4.0)
    penta = scale(62, PENTA_MINOR)  # D pentatonica menor
    # Koto: arpegios ascendentes y notas de adorno
    figure = [(0, 0.0), (2, 0.5), (3, 1.0), (4, 1.5), (3, 2.0), (2, 2.5), (0, 3.0)]
    for bar in range(9):
        base = bar * 4.0
        for idx, off in figure:
            note = penta[idx % len(penta)] + (12 if bar % 4 >= 2 else 0)
            sc.pluck(note, base + off, 0.7, gain=0.2, decay=5.5,
                     harmonics=(1.0, 0.25, 0.1), detune=0.001)
    # Shakuhachi: melodia larga y respirada
    breath = [(74, 0.0, 3.0), (72, 4.0, 2.0), (69, 6.0, 2.0),
              (67, 8.0, 3.0), (69, 12.0, 2.0), (72, 14.0, 4.0)]
    for note, off, dur in breath:
        sc.pad(note, off, dur, gain=0.12, attack=0.45, release=0.9, voices=2)
    # Bajo profundo y percusion minima
    for bar in range(9):
        base = bar * 4.0
        sc.bass(penta[0] - 24, base, 3.5, gain=0.4)
        if bar % 2 == 0:
            sc.bass(penta[3] - 24, base + 2.0, 1.6, gain=0.24)
        sc.kick(base, gain=0.4)
        for off in (1.0, 3.0):
            sc.shaker(base + off, gain=0.07)
    sc.apply_space(wet=0.26, delay_s=0.15)
    return sc, "mus_region_tokyo"


def track_region_montecarlo() -> tuple[Score, str]:
    """Montecarlo: vals de salón con cuerdas y clavecín."""
    bpm = 150.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 3.0)
    progression = [(57, "min"), (62, "maj"), (60, "maj"), (55, "maj")]
    for bar in range(12):
        base = bar * 3.0
        root, kind = progression[bar % 4]
        # Vals: grave en 1, acordes en 2 y 3
        sc.bass(root - 12, base, 1.0, gain=0.45)
        for off in (1.0, 2.0):
            for note in triad(root, kind):
                sc.pluck(note, base + off, 0.5, gain=0.1, decay=8.0)
        for note in triad(root + 12, kind):
            sc.pad(note, base, 2.6, gain=0.05, attack=0.3, release=0.6)
    # Melodia de cuerdas, frase de vals
    melody = [(81, 0.0, 1.0), (80, 1.0, 1.0), (78, 2.0, 1.0),
              (76, 3.0, 1.5), (74, 4.5, 1.5),
              (73, 6.0, 1.0), (74, 7.0, 1.0), (76, 8.0, 1.0),
              (78, 9.0, 2.0), (76, 11.0, 1.0),
              (74, 12.0, 1.0), (73, 13.0, 1.0), (71, 14.0, 1.0),
              (69, 15.0, 3.0)]
    for note, off, dur in melody:
        sc.brass(note, off, dur, gain=0.15)
        sc.pad(note, off, dur, gain=0.06, attack=0.15, release=0.5, voices=2)
    sc.apply_space(wet=0.28, delay_s=0.14)
    return sc, "mus_region_montecarlo"


def track_region_lasvegas() -> tuple[Score, str]:
    """Las Vegas: final mundial, fanfarria y shuffle de show."""
    bpm = 126.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 4.0)
    # Shuffle de bateria
    for bar in range(9):
        base = bar * 4.0
        sc.kick(base, gain=0.6)
        sc.kick(base + 2.5, gain=0.45)
        sc.snare(base + 1.0, gain=0.22)
        sc.snare(base + 3.0, gain=0.24)
        for i in range(4):
            sc.hat(base + i, gain=0.1)
            sc.hat(swing_eighths(base + i + 0.5), gain=0.07)
    # Fanfarria de metales
    fanfare = [(72, 0.0, 0.5), (72, 0.5, 0.5), (72, 1.0, 0.5), (76, 1.5, 1.5),
               (74, 3.0, 1.0),
               (72, 4.0, 0.5), (72, 4.5, 0.5), (72, 5.0, 0.5), (79, 5.5, 2.5),
               (77, 8.0, 1.0), (76, 9.0, 1.0), (74, 10.0, 2.0),
               (72, 12.0, 1.0), (74, 13.0, 1.0), (76, 14.0, 4.0)]
    for note, off, dur in fanfare:
        sc.brass(note, off, dur, gain=0.26)
        sc.brass(note - 12, off, dur, gain=0.1)
    # Organo y bajo
    roots = [(48, "maj"), (53, "maj"), (55, "dom7"), (48, "maj")]
    for bar in range(9):
        base = bar * 4.0
        root, kind = roots[bar % 4]
        for note in seventh(root, "maj7" if kind == "maj" else "dom7"):
            sc.pad(note, base, 3.8, gain=0.05, attack=0.2, release=0.6)
        sc.bass(root - 12, base, 1.8, gain=0.5)
        sc.bass(root - 5, base + 2.0, 1.8, gain=0.4)
    sc.apply_space(wet=0.22, delay_s=0.12)
    return sc, "mus_region_lasvegas"


def track_region_miami() -> tuple[Score, str]:
    """Miami: latin pop con bajo eléctrico y piano sincopado."""
    bpm = 112.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 4.0)
    progression = [(45, "min"), (50, "maj"), (48, "maj"), (43, "maj")]
    for bar in range(9):
        base = bar * 4.0
        root, kind = progression[bar % 4]
        # Bajo electrico con octavas
        sc.bass(root, base, 0.9, gain=0.5)
        sc.bass(root + 12, base + 1.0, 0.6, gain=0.3)
        sc.bass(root, base + 2.0, 0.9, gain=0.45)
        sc.bass(root + 7, base + 3.0, 0.8, gain=0.34)
        # Piano sincopado (montuno pop)
        for off in (0.5, 1.5, 2.5, 3.5):
            for note in triad(root + 12, kind):
                sc.pluck(note, base + off, 0.4, gain=0.1, decay=8.0)
        sc.kick(base, gain=0.55)
        sc.kick(base + 2.5, gain=0.4)
        sc.snare(base + 1.0, gain=0.2)
        sc.snare(base + 3.0, gain=0.2)
        for i in range(8):
            sc.shaker(base + i * 0.5, gain=0.09)
    # Linea melódica brillante
    lead = [(69, 0.0, 1.5), (72, 1.5, 0.5), (74, 2.0, 2.0), (72, 4.0, 1.0),
            (69, 5.0, 1.0), (67, 6.0, 2.0),
            (72, 8.0, 1.5), (74, 9.5, 0.5), (76, 10.0, 2.0), (74, 12.0, 1.0),
            (72, 13.0, 3.0)]
    for note, off, dur in lead:
        sc.brass(note, off, dur, gain=0.18)
    for note, off, dur in lead:
        sc.brass(note, off + 16.0, dur, gain=0.16)
    sc.apply_space(wet=0.18, delay_s=0.1)
    return sc, "mus_region_miami"


def track_region_bronx() -> tuple[Score, str]:
    """El Bronx: soul con aire de marcha, metales graves y groove lento."""
    bpm = 92.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 4.0)
    progression = [(41, "min"), (46, "min"), (44, "maj"), (39, "maj")]
    for bar in range(9):
        base = bar * 4.0
        root, kind = progression[bar % 4]
        # Groove de bateria con snare en el 3
        sc.kick(base, gain=0.6)
        sc.kick(base + 2.5, gain=0.4)
        sc.snare(base + 2.0, gain=0.26)
        sc.snare(base + 3.5, gain=0.14)
        for i in range(4):
            sc.hat(base + i, gain=0.1)
            sc.hat(base + i + 0.5, gain=0.06)
        # Bajo caminando sencillo
        sc.bass(root, base, 1.7, gain=0.5)
        sc.bass(root + 7, base + 2.0, 1.7, gain=0.42)
        # Organo sostenido
        for note in seventh(root + 12, "min7" if kind == "min" else "maj7"):
            sc.pad(note, base, 3.8, gain=0.05, attack=0.25, release=0.7)
    # Metales graves: riff de dos compases
    riff = [(53, 0.0, 1.0), (55, 1.0, 0.5), (56, 1.5, 0.5), (58, 2.0, 2.0),
            (56, 4.0, 1.0), (55, 5.0, 1.0), (53, 6.0, 2.0)]
    for repeat in range(3):
        for note, off, dur in riff:
            sc.brass(note, off + repeat * 8.0, dur, gain=0.2)
    # Coro de respuesta
    for off in (0.0, 4.0, 8.0, 12.0):
        for note in (65, 68):
            sc.pluck(note, off + 2.0, 1.2, gain=0.12, decay=4.0)
    sc.apply_space(wet=0.2, delay_s=0.11)
    return sc, "mus_region_bronx"


def track_region_losangeles() -> tuple[Score, str]:
    """Los Angeles: rock modal con shuffle y acordes abiertos."""
    bpm = 118.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(8, 4.0)
    # Riff modal (modo dorico sobre La)
    riff = [(45, 0.0, 0.5), (45, 0.5, 0.25), (52, 0.75, 0.25), (50, 1.0, 0.5),
            (48, 1.5, 0.5), (45, 2.0, 0.5), (43, 2.5, 0.5), (45, 3.0, 1.0)]
    for bar in range(9):
        base = bar * 4.0
        for note, off, dur in riff:
            sc.brass(note, base + off, dur, gain=0.2)
            sc.pluck(note - 12, base + off, dur, gain=0.14, decay=4.0)
        # Bateria con shuffle
        sc.kick(base, gain=0.6)
        sc.kick(base + 2.0, gain=0.42)
        sc.snare(base + 1.0, gain=0.24)
        sc.snare(base + 3.0, gain=0.26)
        for i in range(4):
            sc.hat(base + i, gain=0.11)
            sc.hat(swing_eighths(base + i + 0.5), gain=0.08)
        sc.bass(33, base, 1.8, gain=0.45)
        sc.bass(40, base + 2.0, 1.8, gain=0.4)
    # Solo de organo en la segunda mitad
    solo = [(69, 0.0, 1.0), (72, 1.0, 0.5), (74, 1.5, 0.5), (76, 2.0, 2.0),
            (74, 4.0, 1.0), (72, 5.0, 0.5), (69, 5.5, 2.5)]
    for note, off, dur in solo:
        sc.pluck(note, off + 18.0, dur, gain=0.16, decay=3.0,
                 harmonics=(1.0, 0.45, 0.25, 0.12))
    sc.apply_space(wet=0.18, delay_s=0.1)
    return sc, "mus_region_losangeles"


def track_win() -> tuple[Score, str]:
    """Victoria: fanfarria ascendente y brillante (una sola vez)."""
    bpm = 128.0
    sc = Score(seconds=4.2, bpm=bpm)
    hits = [(72, 0.0, 0.5), (76, 0.5, 0.5), (79, 1.0, 0.5), (84, 1.5, 1.5)]
    for note, off, dur in hits:
        sc.brass(note, off, dur, gain=0.3)
        sc.brass(note - 12, off, dur, gain=0.12)
        sc.pluck(note, off, dur, gain=0.14, decay=3.0)
    # Acorde final sostenido
    for note in (72, 76, 79, 84):
        sc.pad(note, 3.0, 1.2, gain=0.12, attack=0.05, release=1.4)
    sc.bass(48, 0.0, 1.0, gain=0.4)
    sc.bass(55, 1.0, 1.0, gain=0.4)
    sc.bass(60, 2.0, 2.0, gain=0.45)
    sc.kick(0.0, gain=0.7)
    sc.kick(1.0, gain=0.6)
    sc.snare(1.0, gain=0.3)
    sc.snare(2.0, gain=0.35)
    sc.clave(2.5, gain=0.3)
    sc.clave(3.0, gain=0.3)
    sc.apply_space(wet=0.3, delay_s=0.12)
    return sc, "mus_win"


def track_lose() -> tuple[Score, str]:
    """Derrota: cadencia menor descendente, sobria (una sola vez)."""
    bpm = 76.0
    sc = Score(seconds=5.0, bpm=bpm)
    fall = [(69, 0.0, 0.8), (67, 0.8, 0.8), (65, 1.6, 0.8), (64, 2.4, 0.8), (62, 3.2, 3.0)]
    for note, off, dur in fall:
        sc.brass(note, off, dur, gain=0.2)
        sc.pad(note, off, dur, gain=0.1, attack=0.12, release=1.0, voices=2)
    for note in (50, 53, 57):
        sc.pad(note, 0.0, 3.5, gain=0.07, attack=0.4, release=1.2)
    for note in (45, 48, 52):
        sc.pad(note, 3.2, 2.4, gain=0.08, attack=0.4, release=1.5)
    sc.bass(38, 0.0, 2.0, gain=0.4)
    sc.bass(36, 2.4, 3.0, gain=0.42)
    sc.kick(0.0, gain=0.4)
    sc.apply_space(wet=0.34, delay_s=0.16)
    return sc, "mus_lose"


def track_tension() -> tuple[Score, str]:
    """Tension: pulso minimo y grave, para los momentos de suspense."""
    bpm = 100.0
    sc = Score(seconds=1.0, bpm=bpm)
    sc.set_bars(4)         # el pulso se compone en 16 negras = 4 compases
    # Pulso grave repetido
    for i in range(16):
        sc.bass(33, i, 0.5, gain=0.4)
    # Intervalo inquietante que respira
    for i, note in enumerate((57, 58, 57, 60, 57, 58, 61, 60)):
        sc.pad(note, i * 2.0, 1.9, gain=0.1, attack=0.6, release=0.8, voices=2)
    # Percusion seca y esporadica
    for i in range(4):
        sc.kick(i * 4.0, gain=0.45)
        sc.kick(i * 4.0 + 2.5, gain=0.3)
    for i in range(8):
        sc.hat(i * 2.0 + 1.0, gain=0.07, open_hat=True)
    sc.apply_space(wet=0.3, delay_s=0.17)
    return sc, "mus_tension"


TRACKS = {
    "title": track_title,
    "map": track_map,
    "havana": track_region_havana,
    "mexico": track_region_mexico,
    "sanjuan": track_region_sanjuan,
    "neworleans": track_region_neworleans,
    "tokyo": track_region_tokyo,
    "montecarlo": track_region_montecarlo,
    "lasvegas": track_region_lasvegas,
    "miami": track_region_miami,
    "bronx": track_region_bronx,
    "losangeles": track_region_losangeles,
    "win": track_win,
    "lose": track_lose,
    "tension": track_tension,
}


# ---------------------------------------------------------------------------
# Salida: WAV -> OGG + espectrograma de comprobacion
# ---------------------------------------------------------------------------


def write_wav(path: Path, samples: np.ndarray) -> None:
    pcm = np.clip(samples, -1.0, 1.0)
    pcm = (pcm * 32767.0).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RENDER_SR)
        w.writeframes(pcm.tobytes())


def encode_ogg(ffmpeg: str, wav_path: Path, ogg_path: Path) -> None:
    result = subprocess.run(
        [ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-i", str(wav_path),
         "-c:a", "libvorbis", "-q:a", "3", str(ogg_path)],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg fallo para {ogg_path.name}: {result.stderr.strip()[:200]}")


def spectrogram_png(samples: np.ndarray, path: Path, title: str) -> None:
    """Espectrograma en escala de grises, para inspeccion visual sin oir."""
    from PIL import Image

    frame, hop = 2048, 512
    count = 1 + (samples.size - frame) // hop
    count = max(1, min(count, 900))
    window = np.hanning(frame)
    spec = np.zeros((frame // 2 + 1, count))
    for i in range(count):
        chunk = samples[i * hop:i * hop + frame]
        if chunk.size < frame:
            chunk = np.pad(chunk, (0, frame - chunk.size))
        spec[:, i] = np.abs(np.fft.rfft(chunk * window))
    db = 20 * np.log10(spec + 1e-6)
    db = np.clip((db - db.max() + 70) / 70, 0.0, 1.0)
    img = (db[::-1, :] * 255).astype(np.uint8)  # agudos arriba
    im = Image.fromarray(img, mode="L").resize((count, 320))
    im.save(path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only", nargs="*", help="genera solo estas pistas")
    parser.add_argument("--keep-wav", action="store_true", help="no borra los WAV temporales")
    args = parser.parse_args()

    ffmpeg = find_ffmpeg()
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    PROOF_DIR.mkdir(parents=True, exist_ok=True)
    tmp = REPO / "build" / "music_tmp"
    tmp.mkdir(parents=True, exist_ok=True)

    names = args.only or list(TRACKS)
    total_ogg = 0
    for name in names:
        if name not in TRACKS:
            sys.exit(f"ERROR: pista desconocida '{name}'. Opciones: {', '.join(TRACKS)}")
        score, res_name = TRACKS[name]()
        # El bucle se cierra antes de normalizar: asi el fundido cruzado mezcla
        # amplitudes comparables y la costura queda inaudible.
        score.loopify()
        samples = score.finish()

        wav_path = tmp / f"{res_name}.wav"
        ogg_path = RAW_DIR / f"{res_name}.ogg"
        write_wav(wav_path, samples)
        encode_ogg(ffmpeg, wav_path, ogg_path)
        spectrogram_png(samples, PROOF_DIR / f"{res_name}.png", res_name)

        size = ogg_path.stat().st_size
        total_ogg += size
        rms = float(np.sqrt(np.mean(samples ** 2)))
        peak = float(np.max(np.abs(samples)))
        print(f"{res_name:26s} {samples.size / RENDER_SR:5.1f}s  {size / 1024:7.1f} KB  "
              f"rms={rms:.3f} peak={peak:.3f}")

    if not args.keep_wav:
        shutil.rmtree(tmp, ignore_errors=True)
    print(f"\nTotal OGG: {total_ogg / 1024:.1f} KB en {len(names)} pistas")
    print(f"Espectrogramas en: {PROOF_DIR}")


if __name__ == "__main__":
    main()
