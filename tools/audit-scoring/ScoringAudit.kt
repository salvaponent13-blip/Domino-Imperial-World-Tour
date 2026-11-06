package audittools

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode

/**
 * Auditoria de la puntuacion de mesa de All Fives y Fives & Threes.
 *
 * No forma parte de la app: es un programa de apoyo para comprobar, contra el motor real,
 * si la suma de extremos y los puntos acreditados cuadran con la regla.
 */

private fun t(high: Int, low: Int) = DominoTile(high, low)

private fun engine(mode: GameMode) = DominoEngine.allAi(gameMode = mode, targetScore = 100_000, seed = 7L)

/** Fija mano exacta por asiento (sin relleno: los asientos que no juegan se quedan vacios). */
private fun stage(e: DominoEngine, vararg hands: Pair<Int, List<DominoTile>>) {
    val bySeat = hands.toMap()
    e.players.indices.forEach { seat ->
        e.players[seat] = e.players[seat].copy(hand = bySeat[seat] ?: emptyList())
    }
}

private fun playSide(e: DominoEngine, seat: Int, tile: DominoTile, side: BoardSide): Boolean {
    e.currentPlayerIndex = seat
    return e.playTile(seat, tile, side)
}

/** Igual que el helper `play()` del test: elige LEFT si esta permitido. */
private fun playAuto(e: DominoEngine, seat: Int, tile: DominoTile): Boolean {
    e.currentPlayerIndex = seat
    val sides = e.getPlayableSides(seat, tile)
    val side = if (sides.contains(BoardSide.LEFT)) BoardSide.LEFT else BoardSide.RIGHT
    return e.playTile(seat, tile, side)
}

/**
 * Suma de referencia segun la regla oficial de All Fives (pagat.com/domino/cross/all_fives.html),
 * calculada SOLO con el estado del tablero, sin usar calculateOpenEndsSum():
 *
 *  - Un doble al final de un brazo vale sus dos mitades (12 para el [6|6]).
 *  - El spinner (primer doble) aporta sus dos mitades mientras solo tenga cubierto UNO de sus
 *    dos lados; cuando los dos lados tienen ficha deja de aportar y cuenta como una ficha mas
 *    de la linea.
 *  - A partir de ahi solo cuentan los extremos exteriores de los brazos realmente empezados:
 *    el tercer brazo cuenta cuando se empieza, y el cuarto cuando se empieza; un brazo sin
 *    empezar NO es un extremo abierto.
 */
private fun referenceSum(e: DominoEngine): Int {
    if (e.boardChain.isEmpty()) return 0
    // Solo All Fives tiene spinner: en los otros modos un doble es una ficha mas de la linea.
    val spinner = if (e.gameMode == GameMode.ALL_FIVES) e.spinnerTile else null

    fun branchEnd(branch: List<BoardPlacedTile>, openPip: Int?): Int {
        if (branch.isEmpty()) return 0
        val pip = openPip ?: return 0
        return if (branch.last().isDouble) pip * 2 else pip
    }

    val leftBranch = e.leftBranchTiles
    val rightBranch = e.rightBranchTiles
    val upBranch = e.upBranchTiles
    val downBranch = e.downBranchTiles

    // Una sola ficha en la mesa: vale por si misma, una sola vez (un doble, sus dos mitades).
    if (spinner == null && leftBranch.isEmpty() && rightBranch.isEmpty() && upBranch.isEmpty() && downBranch.isEmpty()) {
        val first = e.firstPlacedTile ?: return 0
        return if (first.isDouble) first.tile.high * 2 else first.tile.high + first.tile.low
    }

    if (spinner == null) {
        // Modos de linea (All Threes, Fives & Threes): los dos extremos.
        val first = e.firstPlacedTile
        val leftVal = if (leftBranch.isEmpty()) {
            val pip = e.leftOpenPip ?: return branchEnd(rightBranch, e.rightOpenPip)
            if (first?.isDouble == true) pip * 2 else pip
        } else branchEnd(leftBranch, e.leftOpenPip)
        val rightVal = if (rightBranch.isEmpty()) {
            val pip = e.rightOpenPip ?: return leftVal
            if (first?.isDouble == true) pip * 2 else pip
        } else branchEnd(rightBranch, e.rightOpenPip)
        return leftVal + rightVal
    }

    // All Fives con spinner: extremos de los brazos empezados.
    var sum = branchEnd(leftBranch, e.leftOpenPip) +
        branchEnd(rightBranch, e.rightOpenPip) +
        branchEnd(upBranch, e.upOpenPip) +
        branchEnd(downBranch, e.downOpenPip)

    val sidesCovered = (if (leftBranch.isNotEmpty()) 1 else 0) + (if (rightBranch.isNotEmpty()) 1 else 0)
    if (sidesCovered < 2) sum += spinner.high * 2
    return sum
}

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
    header("1) ALL FIVES: spinner [6|6] y sus lados en cruz")
    run {
        val e = engine(GameMode.ALL_FIVES)
        stage(
            e,
            0 to listOf(t(6, 6), t(1, 1)),
            1 to listOf(t(6, 3), t(1, 1)),
            2 to listOf(t(6, 2), t(1, 1)),
            3 to listOf(t(6, 4), t(1, 1))
        )
        playSide(e, 0, t(6, 6), BoardSide.LEFT)
        line("tras [6|6]             suma motor =", e.calculateOpenEndsSum(), "| referencia =", referenceSum(e), "| puntos jugada =", e.lastPointsEarned)
        playSide(e, 1, t(6, 3), BoardSide.RIGHT)
        line("tras [6|3] a la derecha suma motor =", e.calculateOpenEndsSum(), "| referencia =", referenceSum(e), "| puntos jugada =", e.lastPointsEarned)
        playSide(e, 2, t(6, 2), BoardSide.LEFT)
        line("tras [6|2] a la izquierda suma motor =", e.calculateOpenEndsSum(), "| referencia =", referenceSum(e), "| puntos jugada =", e.lastPointsEarned, "(el spinner deja de contar: correcto)")
        playSide(e, 3, t(6, 4), BoardSide.UP)
        line("tras [6|4] arriba      suma motor =", e.calculateOpenEndsSum(), "| referencia =", referenceSum(e), "| puntos jugada =", e.lastPointsEarned, "(3er brazo: ya no cuenta el 4o)")
        line("HUD:", e.getOpenEndsEvaluationText())
        line("marcadores:", e.players.joinToString { "${it.name}=${it.score}" })
    }

    header("1b) ALL FIVES: los dos casos que fallaban con el 4o brazo")
    run {
        // Estado: spinner [6|6] con los dos lados cubiertos y UN solo brazo transversal.
        // Oficial: extremos 3 + 1 + 0 = 4 -> no puntua. Motor: 4 + 6 = 10 -> paga 10 puntos.
        val e = engine(GameMode.ALL_FIVES)
        stage(
            e,
            0 to listOf(t(6, 6), t(1, 1)),
            1 to listOf(t(6, 1), t(1, 1)),
            2 to listOf(t(6, 3), t(1, 1)),
            3 to listOf(t(6, 0), t(1, 1))
        )
        playSide(e, 0, t(6, 6), BoardSide.LEFT)
        playSide(e, 1, t(6, 1), BoardSide.RIGHT)
        playSide(e, 2, t(6, 3), BoardSide.LEFT)
        playSide(e, 3, t(6, 0), BoardSide.UP)
        line("oficial = 4 -> 0 pts ; motor =", e.calculateOpenEndsSum(), "-> paga", e.lastPointsEarned, "puntos")
        line("HUD:", e.getOpenEndsEvaluationText())
    }
    run {
        // El mismo 6 de mas, pero esta vez tapa puntos que si existian.
        // Oficial: extremos 3 + 2 + 0 = 5 -> 5 puntos. Motor: 11 -> 0 puntos.
        val e = engine(GameMode.ALL_FIVES)
        stage(
            e,
            0 to listOf(t(6, 6), t(1, 1)),
            1 to listOf(t(6, 2), t(1, 1)),
            2 to listOf(t(6, 3), t(1, 1)),
            3 to listOf(t(6, 0), t(1, 1))
        )
        playSide(e, 0, t(6, 6), BoardSide.LEFT)
        playSide(e, 1, t(6, 2), BoardSide.RIGHT)
        playSide(e, 2, t(6, 3), BoardSide.LEFT)
        playSide(e, 3, t(6, 0), BoardSide.UP)
        line("oficial = 5 -> 5 pts ; motor =", e.calculateOpenEndsSum(), "-> paga", e.lastPointsEarned, "puntos")
        line("HUD:", e.getOpenEndsEvaluationText())
    }

    header("2) ALL FIVES: mismo caso con el 5 (puntos visibles)")
    run {
        val e = engine(GameMode.ALL_FIVES)
        stage(
            e,
            0 to listOf(t(5, 5), t(1, 1)),
            1 to listOf(t(5, 2), t(1, 1)),
            2 to listOf(t(5, 3), t(1, 1)),
            3 to listOf(t(5, 1), t(1, 1))
        )
        playSide(e, 0, t(5, 5), BoardSide.LEFT)
        line("[5|5] solo             suma =", e.calculateOpenEndsSum(), "| puntos =", e.lastPointsEarned)
        playSide(e, 1, t(5, 2), BoardSide.RIGHT)
        line("+[5|2] derecha         suma =", e.calculateOpenEndsSum(), "| puntos =", e.lastPointsEarned, "| referencia =", referenceSum(e))
        playSide(e, 2, t(5, 3), BoardSide.LEFT)
        line("+[5|3] izquierda       suma =", e.calculateOpenEndsSum(), "| puntos =", e.lastPointsEarned, "| referencia =", referenceSum(e))
        line("HUD:", e.getOpenEndsEvaluationText())
    }

    header("3) FIVES & THREES: secuencia del test fivesAndThrees_tracksScoreAcrossAPlayingSequence")
    run {
        val e = engine(GameMode.FIVES_AND_THREES)
        stage(
            e,
            0 to listOf(t(3, 3), t(6, 6)),
            1 to listOf(t(3, 6), t(6, 6)),
            2 to listOf(t(6, 0), t(6, 6)),
            3 to listOf(t(3, 0), t(6, 6))
        )
        playAuto(e, 0, t(3, 3))
        line("[3|3]                  suma =", e.calculateOpenEndsSum(), "| puntos jugada =", e.lastPointsEarned, "| acumulado asiento0 =", e.players[0].score)
        playAuto(e, 1, t(3, 6))
        line("+[3|6]                 suma =", e.calculateOpenEndsSum(), "| puntos jugada =", e.lastPointsEarned, "| acumulado asiento1 =", e.players[1].score)
        playAuto(e, 2, t(6, 0))
        line("+[6|0]                 suma =", e.calculateOpenEndsSum(), "| puntos jugada =", e.lastPointsEarned, "| acumulado asiento2 =", e.players[2].score)
        playAuto(e, 3, t(3, 0))
        line("+[3|0]                 suma =", e.calculateOpenEndsSum(), "| puntos jugada =", e.lastPointsEarned, "| acumulado asiento3 =", e.players[3].score)
        line("el test espera 3 puntos para el asiento 3 ->", if (e.players[3].score == 3) "PASA" else "FALLA")
    }

    header("4) FIVES & THREES: tabla de pagos y alcanzabilidad del 15")
    run {
        fun points(sum: Int): Int {
            val fives = if (sum > 0 && sum % 5 == 0) sum / 5 else 0
            val threes = if (sum > 0 && sum % 3 == 0) sum / 3 else 0
            return fives + threes
        }
        for (s in 0..15) line("suma extremos =", s, "-> puntos =", points(s))
        // ?Se puede dar 15 en doble-seis? Doble en un extremo (12) + 3 en el otro.
        val e = engine(GameMode.FIVES_AND_THREES)
        stage(
            e,
            0 to listOf(t(6, 6), t(1, 1)),
            1 to listOf(t(6, 3), t(1, 1)),
            2 to listOf(t(3, 3), t(1, 1)),
            3 to listOf(t(6, 6), t(1, 1))
        )
        playSide(e, 0, t(6, 6), BoardSide.LEFT)
        playSide(e, 1, t(6, 3), BoardSide.RIGHT)
        line("[6|6] + [6|3]: suma =", e.calculateOpenEndsSum(), "-> puntos jugada =", e.lastPointsEarned)
        line("HUD:", e.getOpenEndsEvaluationText())
        line("=> el 15 es alcanzable y ahora el HUD anuncia los 8 puntos que paga el motor")
    }

    header("5) FIVES & THREES: HUD vs puntos acreditados")
    run {
        val e = engine(GameMode.FIVES_AND_THREES)
        stage(
            e,
            0 to listOf(t(3, 3), t(6, 6)),
            1 to listOf(t(3, 2), t(6, 6))
        )
        playSide(e, 0, t(3, 3), BoardSide.LEFT)
        line("[3|3] suma =", e.calculateOpenEndsSum(), "| puntos =", e.lastPointsEarned)
        line("HUD:", e.getOpenEndsEvaluationText(), "  (HUD y motor ya dicen lo mismo: el HUD", e.lastPointsEarned, "puntos)")
        playSide(e, 1, t(3, 2), BoardSide.RIGHT)
        line("+[3|2] suma =", e.calculateOpenEndsSum(), "| puntos =", e.lastPointsEarned)
        line("HUD:", e.getOpenEndsEvaluationText(), "  (HUD y motor ya dicen lo mismo: el HUD", e.lastPointsEarned, "puntos)")
    }

    header("6) Cierre de ronda: que suma cada modo y que ensena el resumen")
    for (mode in listOf(GameMode.ALL_FIVES, GameMode.ALL_THREES, GameMode.FIVES_AND_THREES)) {
        val e = engine(mode)
        stage(
            e,
            0 to listOf(t(5, 5)),
            1 to listOf(t(6, 4), t(4, 4)),
            2 to listOf(t(6, 5)),
            3 to listOf(t(6, 3), t(3, 3))
        )
        playSide(e, 0, t(5, 5), BoardSide.LEFT)
        val tablePoints = e.roundPointsByPlayer.values.sum()
        line(
            mode.name,
            "| dominó con [5|5] -> isRoundOver =", e.isRoundOver,
            "| lastRoundPointsAwarded =", e.lastRoundPointsAwarded,
            "| puntos de mesa (roundPointsByPlayer) =", tablePoints,
            "| score asiento0 =", e.players[0].score,
            "| marcador equipo0 =", e.teamScore(0)
        )
    }

    header("7) Juego aleatorio: suma del motor vs referencia")
    for (mode in listOf(GameMode.ALL_FIVES, GameMode.ALL_THREES, GameMode.FIVES_AND_THREES)) {
        var mismatch = 0
        var checked = 0
        var phantomAwards = 0
        var missedAwards = 0
        var phantomPoints = 0
        var missedPoints = 0
        var thirdArmStates = 0
        var thirdArmMismatch = 0
        var oneSideBothCrossMismatch = 0
        var otherMismatch = 0
        val samples = mutableListOf<String>()
        val e = DominoEngine.allAi(gameMode = mode, targetScore = 10_000_000, seed = 12345L)
        var rounds = 0
        while (rounds < 400) {
            val before = e.boardChain.size
            e.executeAIMove()
            if (e.boardChain.size > before) {
                checked++
                val eng = e.calculateOpenEndsSum()
                val ref = referenceSum(e)
                if (eng != ref) mismatch++
                // Estado del fallo: los dos lados del spinner cubiertos y un solo brazo transversal.
                val bothHorizontals = e.leftBranchTiles.isNotEmpty() && e.rightBranchTiles.isNotEmpty()
                val crossArms = (if (e.upBranchTiles.isNotEmpty()) 1 else 0) + (if (e.downBranchTiles.isNotEmpty()) 1 else 0)
                val horizontalSides = (if (e.leftBranchTiles.isNotEmpty()) 1 else 0) + (if (e.rightBranchTiles.isNotEmpty()) 1 else 0)
                val inThirdArmState = e.spinnerTile != null && bothHorizontals && crossArms == 1
                val inOneSideBothCross = e.spinnerTile != null && horizontalSides == 1 && crossArms == 2
                if (eng != ref) {
                    when {
                        inThirdArmState -> thirdArmMismatch++
                        inOneSideBothCross -> oneSideBothCrossMismatch++
                        else -> otherMismatch++
                    }
                }
                if (inThirdArmState) thirdArmStates++
                val expected = when (mode) {
                    GameMode.ALL_FIVES -> if (ref > 0 && ref % 5 == 0) ref else 0
                    GameMode.ALL_THREES -> if (ref > 0 && ref % 3 == 0) ref else 0
                    GameMode.FIVES_AND_THREES -> {
                        val f = if (ref > 0 && ref % 5 == 0) ref / 5 else 0
                        val tr = if (ref > 0 && ref % 3 == 0) ref / 3 else 0
                        f + tr
                    }
                    else -> 0
                }
                val actual = e.lastPointsEarned
                if (actual != expected) {
                    if (actual > expected) {
                        phantomAwards++
                        phantomPoints += actual - expected
                    } else {
                        missedAwards++
                        missedPoints += expected - actual
                    }
                    if (samples.size < 3) {
                        samples.add(
                            "spinner=${e.spinnerTile} L=${e.leftOpenPip} R=${e.rightOpenPip} " +
                                "U=${e.upOpenPip} D=${e.downOpenPip} motor=$eng ref=$ref " +
                                "puntos motor=$actual referencia=$expected " +
                                "ramas[L=${e.leftBranchTiles.size} R=${e.rightBranchTiles.size} " +
                                "U=${e.upBranchTiles.size} D=${e.downBranchTiles.size}]"
                        )
                    }
                }
            }
            if (e.isRoundOver || e.isMatchOver) {
                rounds++
                e.startNewRound()
            }
        }
        line(mode.name, "| jugadas comprobadas =", checked, "| suma distinta =", mismatch,
            "| de ellas: 3er brazo =", thirdArmMismatch,
            "| 1 lado + 2 transversales =", oneSideBothCrossMismatch,
            "| otras =", otherMismatch,
            "| jugadas en el estado '3er brazo' =", thirdArmStates)
        line("   ", "jugadas con puntos de mas =", phantomAwards, "(+$phantomPoints pts)",
            "| jugadas con puntos de menos =", missedAwards, "(-$missedPoints pts)")
        samples.forEach { line("   ", it) }
    }

    header("8) Lo que ensena el resumen de ronda vs lo que suma de verdad")
    for (mode in listOf(GameMode.ALL_FIVES, GameMode.ALL_THREES, GameMode.FIVES_AND_THREES)) {
        val e = engine(mode)
        stage(
            e,
            0 to listOf(t(5, 5)),
            1 to listOf(t(6, 4), t(4, 4)),
            2 to listOf(t(6, 5)),
            3 to listOf(t(6, 3), t(3, 3))
        )
        val scoreBefore = e.teamScore(0)
        playSide(e, 0, t(5, 5), BoardSide.LEFT)
        val realAdded = e.teamScore(0) - scoreBefore
        line(
            mode.name,
            "| suma real al equipo ganador =", realAdded,
            "| resumen: puntos de mesa =", e.lastRoundTablePoints,
            "| resumen: pips de cierre =", e.lastRoundClosingPoints,
            "| resumen: total sumado =", e.lastRoundPointsAwarded,
            "| desglose por asiento =", e.roundPointsByPlayer.values.sum()
        )
    }

    header("9) Meta alcanzada a mitad de ronda con varios tanteos previos")
    run {
        val e = DominoEngine.allAi(gameMode = GameMode.ALL_FIVES, targetScore = 30, seed = 3L)
        stage(
            e,
            0 to listOf(t(5, 5), t(0, 0), t(3, 3)),
            1 to listOf(t(1, 1)),
            2 to listOf(t(5, 0), t(2, 2)),
            3 to listOf(t(1, 1))
        )
        val before = e.teamScore(0)
        playSide(e, 0, t(5, 5), BoardSide.LEFT)
        line("jugada 1: +", e.lastPointsEarned, "| equipo0 =", e.teamScore(0))
        playSide(e, 2, t(5, 0), BoardSide.RIGHT)
        line("jugada 2: +", e.lastPointsEarned, "| equipo0 =", e.teamScore(0))
        playSide(e, 0, t(0, 0), BoardSide.RIGHT)
        line("jugada 3: +", e.lastPointsEarned, "| equipo0 =", e.teamScore(0), "| partida terminada =", e.isMatchOver)
        line("la ronda sumo de verdad =", e.teamScore(0) - before,
            "| desglose de la ronda =", e.roundPointsByPlayer.values.sum(),
            "| resumen: mesa =", e.lastRoundTablePoints,
            "| resumen: cierre =", e.lastRoundClosingPoints,
            "| resumen: total =", e.lastRoundPointsAwarded)
    }

    header("11) Fives & Threes: reparto y pozo (la regla oficial no roba)")
    run {
        for (seats in listOf(2, 4)) {
            val e = DominoEngine(gameMode = GameMode.FIVES_AND_THREES, targetScore = 60, playerCount = seats)
            line("$seats jugadores -> allowsDraw =", e.gameMode.allowsDraw,
                "| fichas por asiento =", e.players.map { it.hand.size },
                "| fichas en el pozo =", e.boneyard.size)
        }
        val e = DominoEngine(gameMode = GameMode.FIVES_AND_THREES, targetScore = 60, playerCount = 2)
        e.players.indices.forEach { seat -> e.players[seat] = e.players[seat].copy(hand = listOf(t(6, 6))) }
        e.currentPlayerIndex = 0
        val drawn = e.drawTile(0)
        line("robo en Fives & Threes ->", drawn, "| mano resultante =", e.players[0].hand)
    }

    header("10) Cierre de ronda por tranca en ALL_FIVES")
    run {
        val e = engine(GameMode.ALL_FIVES)
        stage(
            e,
            0 to listOf(t(6, 4)),
            1 to listOf(t(6, 5)),
            2 to listOf(t(6, 3)),
            3 to listOf(t(6, 2))
        )
        playSide(e, 0, t(6, 4), BoardSide.LEFT)
        for (s in 1..3) { e.currentPlayerIndex = s; e.passTurn(s) }
        line("trancada -> isRoundOver =", e.isRoundOver, "| ganador =", e.winnerPlayerId,
            "| lastRoundPointsAwarded =", e.lastRoundPointsAwarded,
            "| puntos de mesa =", e.roundPointsByPlayer.values.sum(),
            "| marcadores =", e.players.joinToString { "${it.name}=${it.score}" })
    }
}
