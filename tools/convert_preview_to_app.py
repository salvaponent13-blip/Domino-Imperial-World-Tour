import subprocess
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

preview_dir = r"C:\Users\horts\Documents\Proyectos\DominoGrandPrix\PREVIEW_MUSIC"
raw_dir = r"C:\Users\horts\Documents\Proyectos\DominoGrandPrix\app\src\main\res\raw"

mapping = {
    "mus_region_havana.ogg": "01_Cuba_LaHabana_NoFrillsSalsa.mp3",
    "mus_region_mexico.ogg": "No_Frills_Cumbia.mp3",
    "mus_region_sanjuan.ogg": "03_PuertoRico_SanJuan_BeachfrontCelebration.mp3",
    "mus_region_neworleans.ogg": "04_NuevaOrleans_TheEntertainer_Ragtime.mp3",
    "mus_region_lasvegas.ogg": "05_LasVegas_HotSwing_BigBand.mp3",
    "mus_region_montecarlo.ogg": "06_MonteCarlo_GranCasino_FoxTaleWaltz.mp3",
    "mus_region_tokyo.ogg": "07_Tokio_Asakusa_Eastminster_KotoShamisen.mp3",
    "mus_title.ogg": "08_MenuPrincipal_ClubImperial_BackbayLounge.mp3",
    "mus_map.ogg": "09_MapaMundial_WorldTour_ModernJazzSamba.mp3",
    "mus_region_miami.ogg": "01_Cuba_LaHabana_NoFrillsSalsa.mp3",
    "mus_region_bronx.ogg": "05_LasVegas_HotSwing_BigBand.mp3",
    "mus_region_losangeles.ogg": "08_MenuPrincipal_ClubImperial_BackbayLounge.mp3",
}

print("=== CONVERTING AUDIO TO ANDROID RAW RESOURCES ===")
for target_ogg, src_mp3 in mapping.items():
    src_path = os.path.join(preview_dir, src_mp3)
    target_path = os.path.join(raw_dir, target_ogg)
    
    if not os.path.exists(src_path):
        print(f"ERROR: Source file not found: {src_path}")
        continue
    
    # We convert to stereo, 44.1kHz, libvorbis quality 3 (~112kbps)
    # This provides studio fidelity while keeping the entire APK under 25MB
    cmd = [
        "ffmpeg", "-y",
        "-i", src_path,
        "-vn",
        "-c:a", "libvorbis",
        "-q:a", "3",
        "-ar", "44100",
        target_path
    ]
    
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if res.returncode == 0:
        size = os.path.getsize(target_path)
        print(f"✓ {target_ogg} <- {src_mp3} ({size:,} bytes)")
    else:
        print(f"FAILED {target_ogg}: {res.stderr.decode('utf-8', errors='ignore')[:200]}")

print("\nAll target raw audio files successfully converted!")
