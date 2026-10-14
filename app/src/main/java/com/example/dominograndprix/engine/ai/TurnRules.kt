package com.example.dominograndprix.engine.ai

/**
 * Comportamiento comun de turno que comparten las estrategias.
 *
 * Robar o pasar no es una decision estrategica: lo dictan las reglas. Cualquier estrategia
 * que no tenga jugada legal debe hacer exactamente esto, asi que vive aqui y no duplicado
 * en cada implementacion.
 */
internal object TurnRules {

    /**
     * Que hacer cuando no hay ninguna jugada legal: robar si el modo y el pozo lo permiten,
     * y si no, pasar.
     */
    fun whenNoPlayableMove(view: PlayerView): AiMove =
        if (view.canDraw) AiMove.Draw else AiMove.Pass
}
