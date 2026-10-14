package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.Personality

/**
 * Estrategia de control: reproduce EXACTAMENTE el comportamiento que tenia el motor antes
 * de extraer el contrato [DominoStrategy].
 *
 * Existe por dos motivos:
 *
 * 1. **Compatibilidad hacia atras**: es la estrategia por defecto, asi que el juego se
 *    comporta igual que siempre.
 * 2. **Baseline medible**: cualquier estrategia nueva se compara contra esta. Sin una
 *    referencia congelada, "mejorar la IA" es una opinion. Es lo que juega el bando de
 *    control en el benchmark A/B.
 *
 * Reglas que calca, incluida su ingenuidad:
 * - Si no hay jugada: roba si puede, y si no pasa.
 * - Personalidad `AGGRESSIVE`: elige la ficha de mas puntos.
 * - Cualquier otra personalidad: la primera jugable de la mano (o sea, ninguna heuristica).
 * - Si la ficha entra por los dos extremos: juega por el lado que mantiene la cadena mas
 *   equilibrada, para que la serpiente crezca simetrica.
 * - En `DRAW_SPECIAL` prefiere su propia rama y solo invade la de un rival que acaba de
 *   pasar; si no puede ninguna de las dos, usa la unica jugada legal que quede.
 *
 * OJO: si se cambia algo de esta clase, el baseline deja de ser comparable con las
 * mediciones anteriores del benchmark. Cualquier cambio aqui invalida el historico.
 */
class LegacyStrategy(
    /** Personalidad del asiento; solo distingue entre AGGRESSIVE y el resto. */
    private val personality: Personality = Personality.NICE
) : DominoStrategy {

    override val name: String = "legacy"

    override fun decide(view: PlayerView): AiMove {
        if (view.hasNoPlayableMove) return TurnRules.whenNoPlayableMove(view)

        val move = if (view.gameMode == com.example.dominograndprix.model.GameMode.DRAW_SPECIAL) {
            decideSpecial(view)
        } else {
            decideStandard(view)
        }
        return move ?: view.playableMoves.first()
    }

    /** Modos normales: la primera jugable, o la de mas pips si es agresivo. */
    private fun decideStandard(view: PlayerView): AiMove.Play? {
        val ordered = if (personality == Personality.AGGRESSIVE) {
            view.playableMoves.sortedByDescending { it.tile.totalPips }
        } else {
            view.playableMoves
        }
        val chosen = ordered.first()

        val sides = view.playableMoves.filter { it.tile == chosen.tile }.map { it.side }.toSet()
        return when {
            sides.size == 2 -> chosen.copy(side = balancedSide(view))
            sides.contains(BoardSide.LEFT) -> chosen.copy(side = BoardSide.LEFT)
            else -> chosen.copy(side = BoardSide.RIGHT)
        }
    }

    /**
     * Draw Special: intenta su propia rama y, si no puede, aprovecha la rama de un rival
     * que acaba de pasar antes de recurrir a cualquier jugada legal.
     */
    private fun decideSpecial(view: PlayerView): AiMove.Play? {
        val preference = listOfNotNull(view.ownBranch, view.invadableBranch)
        for (side in preference) {
            view.playableMoves.firstOrNull { it.side == side }?.let { return it }
        }
        return null
    }

    /**
     * Lado que mantiene las dos ramas equilibradas, replicando el criterio del motor:
     * se cuenta cuantas fichas hay a cada lado de la ficha inicial.
     */
    private fun balancedSide(view: PlayerView): BoardSide {
        val firstIndex = view.boardChain.indexOfFirst { it.isFirstTile }.let { if (it == -1) 0 else it }
        val leftLength = firstIndex
        val rightLength = view.boardChain.size - 1 - firstIndex
        return if (leftLength <= rightLength) BoardSide.LEFT else BoardSide.RIGHT
    }
}
