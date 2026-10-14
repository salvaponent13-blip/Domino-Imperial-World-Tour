package com.example.dominograndprix.data

import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Personality
import kotlinx.serialization.Serializable

/**
 * Fotografia de una partida en curso, para poder reanudarla si el sistema mata el proceso.
 *
 * Alcance acordado (enfoque 80/20): se guarda **al cerrar cada ronda**, nunca a mitad de
 * jugada. Eso deja fuera los micro-estados en vuelo (un encadenamiento hungaro a medias, la
 * ventana de invasion de Draw Special, una animacion a medio hacer) que son la principal
 * fuente de fallos al restaurar. El precio es que si el proceso muere en mitad de una ronda,
 * esa ronda se pierde; el jugador conserva la partida, los marcadores y su dinero.
 *
 * [schemaVersion] permite descartar un guardado antiguo en lugar de intentar interpretarlo:
 * ante la duda, perder la partida es mejor que arrancar con el estado corrupto.
 */
@Serializable
data class MatchSnapshot(
    /** Version del formato. Si no coincide con la actual, el guardado se descarta. */
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,

    // --- Identidad de la partida ---
    val gameMode: GameMode,
    val difficulty: Difficulty,
    val targetScore: Int,
    val patternIndex: Int,
    val cityName: String,
    val venueId: String?,
    val betAmount: Int,

    // --- Progreso del match ---
    /** Marcadores acumulados por asiento, en orden de asiento. */
    val scores: List<Int>,
    val isMatchOver: Boolean,
    val winningTeam: Int?,

    /** Personalidades de los asientos, para reconstruir el rival que estaba en la mesa. */
    val personalities: List<Personality>,

    /**
     * Asientos ocupados por personas.
     *
     * Sin esto, una partida de pass-and-play se reanudaria como si fuera de un solo jugador y
     * la IA empezaria a mover fichas de los asientos humanos. Tiene valor por defecto para que
     * los guardados anteriores a esta version sigan cargando.
     */
    val humanPlayerIndices: Set<Int> = setOf(0),

    // --- Ronda cerrada que el jugador tenia delante ---
    val boardChain: List<BoardPlacedTile>,
    val hands: List<List<DominoTile>>,
    val leftOpenPip: Int?,
    val rightOpenPip: Int?,
    val winnerPlayerId: Int?,
    val roundLog: String,

    /**
     * Semilla del generador de esta partida.
     *
     * No se guarda el estado interno del generador (no es serializable de forma portable),
     * asi que al reanudar la siguiente ronda se reparte con la semilla original. Las manos de
     * la ronda que se reanuda SI son exactas: vienen en [hands].
     */
    val randomSeed: Long
) {
    companion object {
        /** Subir esto invalida los guardados anteriores a proposito. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }

    /** Si el guardado es de una version que este build sabe interpretar. */
    val isCompatible: Boolean get() = schemaVersion == CURRENT_SCHEMA_VERSION
}
