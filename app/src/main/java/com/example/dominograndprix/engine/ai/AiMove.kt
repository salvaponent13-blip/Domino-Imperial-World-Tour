package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile

/**
 * Jugada decidida por una estrategia de IA.
 *
 * Es una decision en abstracto: no toca el motor. Quien la ejecuta es [com.example.dominograndprix.engine.DominoEngine],
 * que ademas valida que sea legal antes de aplicarla. Asi una estrategia nunca puede
 * corromper el estado de la partida aunque se equivoque.
 */
sealed class AiMove {

    /** Jugar [tile] por el extremo [side]. */
    data class Play(val tile: DominoTile, val side: BoardSide) : AiMove()

    /** Robar del pozo. Solo tiene sentido en modos que lo permiten. */
    object Draw : AiMove()

    /** Pasar el turno. */
    object Pass : AiMove()

    override fun toString(): String = when (this) {
        is Play -> "Play($tile, $side)"
        Draw -> "Draw"
        Pass -> "Pass"
    }
}
