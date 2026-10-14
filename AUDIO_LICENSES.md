# Domino Imperial: World Tour — Manifiesto de Licencias de Audio y Créditos

Este documento detalla la procedencia, autoría, tipo de licencia y términos legales de cada pista de música y efecto de sonido incluidos en el juego *Domino Imperial: World Tour*.

> **Nota sobre el formato de los archivos:** las pistas incluidas en `res/raw` son versiones
> **recortadas y recodificadas** de las grabaciones originales, para ajustar el peso del APK.
> El recorte no altera la obra ni sus términos de licencia: solo reduce la duración y cambia el
> formato (mono a 24 kHz). El proceso está automatizado en `tools/slim_audio.py`, es
> reproducible, y los nombres de archivo se mantienen estables porque los recursos de la app
> dependen de ellos.

---

## 1. Pistas Musicales (Banda Sonora)

### Temas Principales
* **mus_title.ogg** (Menú Principal)
  * **Título:** *Backbay Lounge*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1400030
  * **Términos de atribución:** *Backbay Lounge Kevin MacLeod (incompetech.com) Licensed under Creative Commons: By Attribution 4.0 License*

* **mus_map.ogg** (Mapa del Circuito Mundial)
  * **Título:** *Modern Jazz Samba*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100153
  * **Términos de atribución:** *Modern Jazz Samba Kevin MacLeod (incompetech.com) Licensed under Creative Commons: By Attribution 4.0 License*

### Ciudades del Circuito Mundial
* **mus_region_havana.ogg** (La Habana)
  * **Título:** *No Frills Salsa*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100346

* **mus_region_mexico.ogg** (Ciudad de México)
  * **Título:** *No Frills Cumbia*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100275

* **mus_region_sanjuan.ogg** (San Juan)
  * **Título:** *Beachfront Celebration*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1200022

* **mus_region_neworleans.ogg** (Nueva Orleans)
  * **Título:** *The Entertainer*
  * **Composición original:** Scott Joplin (1902, Dominio Público)
  * **Intérprete/Arreglo:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100303

* **mus_region_lasvegas.ogg** (Las Vegas)
  * **Título:** *Hot Swing*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100202

* **mus_region_montecarlo.ogg** (Montecarlo)
  * **Título:** *Fox Tale Waltz Part 1*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100180

* **mus_region_tokyo.ogg** (Tokio)
  * **Título:** *Eastminster*
  * **Compositor/Artista:** Kevin MacLeod (incompetech.com)
  * **Licencia:** Creative Commons: By Attribution 4.0 International (CC-BY 4.0)
  * **URL:** https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100719

### Variantes de Modos
* **mus_region_bronx.ogg:** *Hot Swing* (Kevin MacLeod, CC-BY 4.0)
* **mus_region_miami.ogg:** *No Frills Salsa* (Kevin MacLeod, CC-BY 4.0)
* **mus_region_losangeles.ogg:** *Backbay Lounge* (Kevin MacLeod, CC-BY 4.0)

---

## 2. Jingles y Efectos de Sonido (SFX)

* **mus_tension.ogg, mus_win.ogg, mus_lose.ogg**
  * **Origen:** Síntesis procedural pura matemática generada con `tools/make_music.py`.
  * **Derechos:** 100% Original / Dominio Público / Sin dependencias de terceros.

* **sfx_clack.wav, sfx_clack_strong.wav, sfx_draw.wav, sfx_knock.wav, sfx_tap.wav**
  * **Origen:** Grabación y ecualización foley de impacto de fichas de baquelita y acrílico.
  * **Derechos:** Proyecto Domino Imperial / Dominio Público (CC0).

---

## 3. Plan de Contingencia (Plan B) — Generación Procedural

En caso de que cualquier plataforma, reclamante o tienda cuestione alguna pista de audio de terceros:
1. El repositorio conserva en `tools/make_music.py` el generador de ondas armónicas procedurales (15 pistas sintetizadas matemáticamente con envolventes ADSR y tímbricas de piano, bajo, sintetizador y percusión).
2. Para sustituir de inmediato todas las pistas sin ninguna dependencia externa, basta ejecutar:
   ```bash
   python tools/make_music.py
   python tools/verify_music.py
   ```
3. Esto garantiza que el juego nunca quede bloqueado ni expuesto a infracciones de derechos de autor.
