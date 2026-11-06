package audittools

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.GameMode

/**
 * Comprueba si los puntos que se van anotando durante una partida de tanteo se conservan
 * hasta el final, y que es lo que ensena el resumen de cada mano frente a lo que se sumo
 * de verdad al marcador.
 */

private fun header(text: String) {
    println()
    println("=".repeat(78))
    println(text)
    println("=".repeat(78))
}

private fun line(vararg cols: Any?) {
    println(cols.joinToString(" | ") { it?.toString() ?: "" })
}

fun main() {
    for (mode in GameMode.values()) {
        header("Partida completa ${mode.name}: marcador de cada mano frente al resumen")
        val e = DominoEngine.allAi(gameMode = mode, targetScore = 100, seed = 2024L)
        var awarded = 0
        var rounds = 0
        var guard = 0
        var sumDialogMesa = 0
        var sumDialogTotal = 0
        var sumRealTeam0 = 0
        while (!e.isMatchOver && guard < 2000) {
            guard++
            val team0Before = e.teamScore(0)
            // jugar la mano entera
            while (!e.isRoundOver && !e.isMatchOver) {
                val before = e.boardChain.size
                e.executeAIMove()
                if (e.boardChain.size > before) awarded += e.lastPointsEarned
            }
            val realTeam0 = e.teamScore(0) - team0Before
            val mesa = e.roundPointsByPlayer.values.sum()
            sumDialogMesa += mesa
            sumDialogTotal += e.lastRoundPointsAwarded
            sumRealTeam0 += realTeam0
            rounds++
            line(
                "mano #$rounds",
                "gana asiento ${e.winnerPlayerId}",
                "tranca=${e.lastRoundWasBlocked}",
                "mesa=$mesa",
                "resumenMesa=${e.lastRoundTablePoints}",
                "resumenCierre=${e.lastRoundClosingPoints}",
                "resumenTotal=${e.lastRoundPointsAwarded}",
                "realAñadidoEquipo0=$realTeam0",
                "marcador=${e.teamScore(0)}-${e.teamScore(1)}"
            )
            if (e.isMatchOver) break
            e.startNewRound(e.nextRoundOpener())
        }
        val totalScores = e.players.sumOf { it.score }
        line()
        line("fin de partida:", "isMatchOver=${e.isMatchOver}", "ganador=${e.winningTeam}")
        line("puntos acreditados jugada a jugada =", awarded)
        line("suma de los marcadores finales      =", totalScores)
        line("puntos de mas o de menos            =", totalScores - awarded)
        line("suma de 'puntos de mesa' (resumen)  =", sumDialogMesa)
        line("suma de 'total sumado' (resumen)    =", sumDialogTotal)
        line("suma real añadida al equipo 0       =", sumRealTeam0)
    }
}
