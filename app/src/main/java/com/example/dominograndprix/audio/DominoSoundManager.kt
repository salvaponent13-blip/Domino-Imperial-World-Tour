package com.example.dominograndprix.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dominograndprix.R

/**
 * Gestor global de audio y musica regional de Domino Imperial: World Tour.
 *
 * - Musica de fondo: pistas OGG originales (ver `tools/make_music.py`, que las sintetiza
 *   desde cero) reproducidas en bucle con MediaPlayer. Se usa OGG Vorbis porque es un
 *   formato que Android reproduce de forma nativa y sin licencias.
 * - Efectos: fichas fisicas de acrilico (golpe, doble, robo, toque y toque de paso)
 *   mediante SoundPool.
 */
object DominoSoundManager {

    var isMuted by mutableStateOf(false)
        private set

    var currentTrackTitle by mutableStateOf("Tema Imperial")
        private set

    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusicResId: Int = 0

    private var soundPool: SoundPool? = null
    private var sfxClackId: Int = 0
    private var sfxClackStrongId: Int = 0
    private var sfxTapId: Int = 0
    private var sfxDrawId: Int = 0
    private var sfxKnockId: Int = 0

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build().apply {
                    sfxClackId = load(appContext, R.raw.sfx_clack, 1)
                    sfxClackStrongId = load(appContext, R.raw.sfx_clack_strong, 1)
                    sfxTapId = load(appContext, R.raw.sfx_tap, 1)
                    sfxDrawId = load(appContext, R.raw.sfx_draw, 1)
                    sfxKnockId = load(appContext, R.raw.sfx_knock, 1)
                }
        } catch (_: Exception) {
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            try {
                mediaPlayer?.pause()
            } catch (_: Exception) {
            }
        } else {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                } else if (currentMusicResId != 0) {
                    playMusicRes(currentMusicResId, currentTrackTitle, loop = true)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun playTitleMusic() {
        playMusicRes(R.raw.mus_title, "Obertura Domino Imperial", loop = true)
    }

    fun playMapMusic() {
        playMusicRes(R.raw.mus_map, "Pasaporte World Tour", loop = true)
    }

    /**
     * Musica propia de cada sede. Todas las pistas son composiciones originales del
     * proyecto (generadas con `tools/make_music.py`), asi que cada ciudad tiene su
     * propio caracter sin depender de material de terceros.
     */
    fun playVenueMusic(venueId: String?) {
        val (resId, title) = when (venueId) {
            "mexico" -> R.raw.mus_region_mexico to "Fiesta en el Zócalo (Ciudad de México)"
            "puerto_rico" -> R.raw.mus_region_sanjuan to "Tambores del Morro (San Juan)"
            "cuba" -> R.raw.mus_region_havana to "Ritmo del Malecón (La Habana)"
            "new_orleans" -> R.raw.mus_region_neworleans to "Bourbon Street Nocturne (Nueva Orleans)"
            "tokyo" -> R.raw.mus_region_tokyo to "Farolillos de Asakusa (Tokio)"
            "monte_carlo" -> R.raw.mus_region_montecarlo to "Vals del Gran Casino (Montecarlo)"
            "las_vegas" -> R.raw.mus_region_lasvegas to "Gran Final del Strip (Las Vegas)"
            "miami" -> R.raw.mus_region_miami to "Brisa de Ocean Drive (Miami)"
            "bronx" -> R.raw.mus_region_bronx to "Aceras del Bronx (Nueva York)"
            "los_angeles" -> R.raw.mus_region_losangeles to "Luces de Sunset (Los Ángeles)"
            else -> R.raw.mus_region_havana to "Ritmo del Malecón (La Habana)"
        }
        playMusicRes(resId, title, loop = true)
    }

    fun playVictoryJingle() {
        playMusicRes(R.raw.mus_win, "¡Victoria en la Mesa!", loop = false)
    }

    fun playDefeatJingle() {
        playMusicRes(R.raw.mus_lose, "Fin de Partida", loop = false)
    }

    /** Cortina de suspense, para los momentos de tension de la partida. */
    fun playTensionMusic() {
        playMusicRes(R.raw.mus_tension, "Momento Decisivo", loop = true)
    }

    private fun playMusicRes(resId: Int, trackTitle: String, loop: Boolean) {
        currentTrackTitle = trackTitle
        if (currentMusicResId == resId && mediaPlayer?.isPlaying == true) {
            return
        }
        currentMusicResId = resId
        val ctx = appContext ?: return
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null

        if (isMuted) return

        try {
            mediaPlayer = MediaPlayer.create(ctx, resId)?.apply {
                isLooping = loop
                setVolume(0.65f, 0.65f)
                start()
            }
        } catch (_: Exception) {
        }
    }

    fun playTileClack(isDouble: Boolean = false) {
        if (isMuted) return
        val pool = soundPool ?: return
        val id = if (isDouble) sfxClackStrongId else sfxClackId
        if (id != 0) {
            pool.play(id, 0.95f, 0.95f, 1, 0, if (isDouble) 0.96f else 1.0f)
        }
    }

    fun playTileTap() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (sfxTapId != 0) {
            pool.play(sfxTapId, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    fun playDrawTile() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (sfxDrawId != 0) {
            pool.play(sfxDrawId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    fun playPassKnock() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (sfxKnockId != 0) {
            pool.play(sfxKnockId, 0.9f, 0.9f, 1, 0, 1.0f)
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    fun resumeMusic() {
        if (isMuted) return
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (_: Exception) {
        }
    }
}
