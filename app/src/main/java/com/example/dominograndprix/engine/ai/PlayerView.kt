package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode

/**
 * Fotografia inmutable del estado del juego desde el punto de vista de UN jugador.
 *
 * Es lo unico que ve una estrategia de IA, y esta construida a proposito para que no
 * pueda ver mas que un humano sentado en la mesa:
 *
 * - La mano propia ([myHand]).
 * - Lo PUBLICO: la cadena colocada, los extremos abiertos, cuantas fichas quedan en el
 *   pozo y los marcadores.
 * - La informacion derivada que el motor ya calcula con sus reglas ([playableMoves]).
 *
 * NO contiene las manos de los demas. Si algun dia alguien las anade aqui, la IA tendria
 * informacion privilegiada y el benchmark de fuerza mediria a un tramposo. Hay un test
 * que lo vigila (EngineReproducibilityTest.playerViewDoesNotExposeOtherPlayersHands).
 */
data class PlayerView(
    /** Asiento que debe decidir. */
    val playerIndex: Int,
    /** Mano de este jugador. */
    val myHand: List<DominoTile>,
    /** Cadena del tablero, de izquierda a derecha. Es informacion publica. */
    val boardChain: List<BoardPlacedTile>,
    /** Punto abierto del extremo izquierdo, o `null` si la mesa esta vacia. */
    val leftOpenPip: Int?,
    /** Punto abierto del extremo derecho, o `null` si la mesa esta vacia. */
    val rightOpenPip: Int?,
    /** Fichas que quedan sin repartir. */
    val boneyardSize: Int,
    /** Modalidad en curso: cambia por completo que jugada es buena. */
    val gameMode: GameMode,
    /** Si el modo es por equipos (0/2 contra 1/3). */
    val isTeamGame: Boolean,
    /** Marcadores de todos los asientos, indexados por asiento. */
    val scores: List<Int>,
    /** Numero de turno dentro de la ronda. */
    val turnNumber: Int,
    /** Rondas consecutivas sin jugar, para saber si la mesa se esta cerrando. */
    val consecutivePasses: Int,
    /**
     * Jugadas legales AHORA, ya resueltas por el motor.
     *
     * Se entrega calculada a proposito: la legalidad de una jugada es responsabilidad del
     * motor (Matador, ramas de Draw Special, encadenamiento hungaro...), no de cada
     * estrategia. Asi ninguna puede divergir de las reglas por reimplementarlas.
     */
    val playableMoves: List<AiMove.Play>,
    /** Rama asignada a este asiento en Draw Special, o `null` en el resto de modos. */
    val ownBranch: BoardSide?,
    /** Turno en que paso el jugador cuyo pase dejo una rama abierta, o `null`. */
    val lastPassTurnNumber: Int?,
    /** Asiento que paso por ultima vez, o `null`. */
    val lastPassingPlayerIndex: Int?,
    /** Punto abierto superior (All Fives con spinner), o null. */
    val upOpenPip: Int? = null,
    /** Punto abierto inferior (All Fives con spinner), o null. */
    val downOpenPip: Int? = null,
    /** Ficha spinner activa en All Fives, o null. */
    val spinnerTile: DominoTile? = null
) {
    /** Cuantas fichas tiene este jugador. */
    val handSize: Int get() = myHand.size

    /** La mesa esta vacia: toca abrir. */
    val isOpeningMove: Boolean get() = boardChain.isEmpty()

    /** Suma de los extremos abiertos (0 si la mesa esta vacia). */
    val openEndsSum: Int get() = (leftOpenPip ?: 0) + (rightOpenPip ?: 0) + (upOpenPip ?: 0) + (downOpenPip ?: 0)

    /** Puntos que le quedan en la mano; en las reglas de cierre decide quien gana. */
    val myRemainingPips: Int get() = myHand.sumOf { it.totalPips }

    /**
     * Rama de un rival que este jugador puede invadir AHORA en Draw Special.
     *
     * Solo ocurre si el dueño de esa rama acaba de pasar en este mismo turno.
     */
    val invadableBranch: BoardSide?
        get() {
            if (gameMode != GameMode.DRAW_SPECIAL) return null
            val passer = lastPassingPlayerIndex ?: return null
            if (passer == playerIndex) return null
            if (lastPassTurnNumber != turnNumber) return null
            val opened = if (ownBranch == BoardSide.LEFT) BoardSide.RIGHT else BoardSide.LEFT
            return opened
        }

    /** Si este jugador puede robar del pozo. */
    val canDraw: Boolean get() = gameMode.allowsDraw && boneyardSize > 0

    /** No hay ninguna jugada legal: toca robar o pasar. */
    val hasNoPlayableMove: Boolean get() = playableMoves.isEmpty()
}
