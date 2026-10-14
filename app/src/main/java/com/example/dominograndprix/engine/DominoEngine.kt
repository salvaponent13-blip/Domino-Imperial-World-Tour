package com.example.dominograndprix.engine

import com.example.dominograndprix.engine.ai.AiMove
import com.example.dominograndprix.engine.ai.DominoStrategy
import com.example.dominograndprix.engine.ai.LegacyStrategy
import com.example.dominograndprix.engine.ai.PlayerView
import com.example.dominograndprix.model.*
import kotlin.math.abs
import kotlin.random.Random

data class NextSlotPlacement(
    val virtX: Int,
    val virtY: Int,
    val isVertical: Boolean,
    val isForward: Boolean
)

class DominoEngine(
    val gameMode: GameMode,
    val targetScore: Int = 100,
    val isTeamGame: Boolean = true,
    val patternIndex: Int = 3, // Patrón por defecto: escalones simétricos en S
    /**
     * Generador aleatorio del motor. Se inyecta para poder reproducir una partida entera:
     * con la misma semilla, dos motores reparten las mismas manos ronda tras ronda.
     * Es la base del benchmark de IA y del desafio diario. En produccion se deja el
     * generador por defecto.
     */
    private val random: Random = Random.Default
) {
    // Deck & Boneyard
    var boneyard: MutableList<DominoTile> = mutableListOf()
    val players: MutableList<Player> = mutableListOf()

    // Board Chain (Left to Right)
    val boardChain: MutableList<BoardPlacedTile> = mutableListOf()
    var leftOpenPip: Int? = null
    var rightOpenPip: Int? = null

    // Turn control
    var currentPlayerIndex: Int = 0
    var consecutivePasses: Int = 0
    var isRoundOver: Boolean = false
    var isMatchOver: Boolean = false
    var winnerPlayerId: Int? = null
    var winningTeam: Int? = null
    var roundLog: String = ""

    // Hungarian chaining flag
    var canContinueHungarianTurn: Boolean = false

    // Draw Special: jugador que abrió la mano. Determina qué rama le corresponde
    // al asiento 0 y, por rotación, a todos los demás.
    private var openingPlayerIndex: Int = 0

    /**
     * Estrategia por asiento, para benchmarks y pruebas.
     *
     * Vacio (lo normal en el juego): cada asiento usa [LegacyStrategy] con su personalidad,
     * que es el comportamiento historico. Relleno: el asiento indicado usa la estrategia
     * inyectada, lo que permite enfrentar dos estrategias en la misma mesa.
     */
    private val strategyOverrides: MutableMap<Int, DominoStrategy> = mutableMapOf()

    /** Ultima decision tomada por una IA. Util para depurar y para los tests. */
    var lastAiMove: AiMove? = null
        private set

    // Último jugador que pasó y el turno en que lo hizo. Sirve para saber si la rama
    // de un rival sigue bloqueada y por tanto se puede invadir.
    private var lastPassingPlayerIndex: Int? = null
    private var lastPassTurnNumber: Int = -1
    private var turnNumber: Int = 0

    val currentPattern: BoardPatternTemplate
        get() = DominoLayoutTemplates.PATTERNS[patternIndex.coerceIn(0, DominoLayoutTemplates.PATTERNS.lastIndex)]

    init {
        setupGame()
    }

    fun setupGame() {
        players.clear()
        val p1 = Player(0, "Tú", isHuman = true, personality = Personality.CONFIDENT, team = 0)
        val p2 = Player(1, "Carlos", isHuman = false, personality = Personality.AGGRESSIVE, team = 1)
        val p3 = Player(2, "Mateo", isHuman = false, personality = Personality.NICE, team = 0)
        val p4 = Player(3, "Rodrigo", isHuman = false, personality = Personality.CONFIDENT, team = 1)
        players.addAll(listOf(p1, p2, p3, p4))
        startNewRound()
    }

    fun startNewRound() {
        boardChain.clear()
        leftOpenPip = null
        rightOpenPip = null
        consecutivePasses = 0
        isRoundOver = false
        canContinueHungarianTurn = false
        lastPassingPlayerIndex = null
        lastPassTurnNumber = -1
        turnNumber = 0

        // Generate standard Double-6 domino set (28 tiles)
        val deck = mutableListOf<DominoTile>()
        for (i in 0..6) {
            for (j in i..6) {
                deck.add(DominoTile(j, i))
            }
        }
        // Barajado con el generador inyectado: fijando su semilla, la partida es reproducible.
        deck.shuffle(random)

        // In Draw modes with 4 players, deal 5 tiles each so 8 tiles remain in the boneyard (pozo) to draw!
        // In Block modes, deal 7 tiles each.
        val tilesPerPlayer = if (gameMode.allowsDraw) 5 else 7
        for (i in players.indices) {
            val hand = mutableListOf<DominoTile>()
            repeat(tilesPerPlayer) {
                if (deck.isNotEmpty()) hand.add(deck.removeAt(0))
            }
            players[i] = players[i].copy(hand = hand)
        }
        boneyard = deck

        // Determine starting player by highest double
        var bestDouble = -1
        var startingPlayer = 0
        for (i in players.indices) {
            for (tile in players[i].hand) {
                if (tile.isDouble && tile.high > bestDouble) {
                    bestDouble = tile.high
                    startingPlayer = i
                }
            }
        }
        currentPlayerIndex = startingPlayer
        openingPlayerIndex = startingPlayer
        val starterName = players[currentPlayerIndex].name
        roundLog = "$starterName abre la mano."
    }

    fun isMatadorTile(tile: DominoTile): Boolean {
        return (tile.high == 6 && tile.low == 1) ||
                (tile.high == 5 && tile.low == 2) ||
                (tile.high == 4 && tile.low == 3) ||
                (tile.high == 0 && tile.low == 0)
    }

    /** ¿La ficha encaja por pura coincidencia de puntos con el extremo indicado? */
    private fun canConnectByPips(tile: DominoTile, target: Int): Boolean {
        if (gameMode == GameMode.MATADOR) {
            if (isMatadorTile(tile)) return true
            if (target == 0) return isMatadorTile(tile)
            return (tile.high + target == 7 || tile.low + target == 7)
        }
        return tile.hasPip(target)
    }

    fun canConnectLeft(tile: DominoTile): Boolean {
        if (boardChain.isEmpty()) return true
        val target = leftOpenPip ?: return false
        return canConnectByPips(tile, target)
    }

    fun canConnectRight(tile: DominoTile): Boolean {
        if (boardChain.isEmpty()) return true
        val target = rightOpenPip ?: return false
        return canConnectByPips(tile, target)
    }

    // ---------------------------------------------------------------------
    // Draw Special: cada asiento tiene una rama asignada. La rama del jugador
    // que abrió la mano es la izquierda y el resto se reparte por rotación, de
    // modo que compañeros de equipo (asientos 0 y 2 / 1 y 3) no comparten rama.
    // ---------------------------------------------------------------------

    /** Rama asignada a un asiento, o `null` antes de que la ronda empiece. */
    fun branchForPlayer(playerIndex: Int): BoardSide? {
        if (gameMode != GameMode.DRAW_SPECIAL) return null
        if (players.isEmpty()) return null
        val seats = players.size
        val seat = ((playerIndex - openingPlayerIndex) % seats + seats) % seats
        return if (seat % 2 == 0) BoardSide.LEFT else BoardSide.RIGHT
    }

    /** Jugador cuyo turno es inmediatamente anterior al asiento indicado. */
    private fun previousPlayerIndex(playerIndex: Int): Int {
        if (players.isEmpty()) return playerIndex
        val seats = players.size
        return ((playerIndex - 1) % seats + seats) % seats
    }

    /**
     * En Draw Special la rama de un rival solo se puede invadir si ese rival
     * acaba de pasar (no tenía jugada propia). Si nadie ha pasado, cada jugador
     * queda confinado a su rama.
     */
    private fun isBranchOpenFor(playerIndex: Int, side: BoardSide): Boolean {
        if (gameMode != GameMode.DRAW_SPECIAL) return true
        val own = branchForPlayer(playerIndex)
        if (own == side) return true
        val blocked = lastPassingPlayerIndex ?: return false
        if (lastPassTurnNumber != turnNumber) return false
        if (blocked == playerIndex) return false
        return branchForPlayer(blocked) == side
    }

    /**
     * Lados por los que esta ficha puede jugarse legalmente para este jugador.
     * Devuelve un conjunto vacío si la ficha no encaja en ningún lado permitido.
     */
    fun getPlayableSides(playerIndex: Int, tile: DominoTile): Set<BoardSide> {
        if (isRoundOver) return emptySet()
        if (boardChain.isEmpty()) return setOf(BoardSide.LEFT, BoardSide.RIGHT)

        val sides = mutableSetOf<BoardSide>()
        if (canConnectLeft(tile) && isBranchOpenFor(playerIndex, BoardSide.LEFT)) {
            sides.add(BoardSide.LEFT)
        }
        if (canConnectRight(tile) && isBranchOpenFor(playerIndex, BoardSide.RIGHT)) {
            sides.add(BoardSide.RIGHT)
        }
        return sides
    }

    /** ¿La rama indicada está cerrada para este jugador por la regla de Draw Special? */
    fun isBranchRestrictedFor(playerIndex: Int, side: BoardSide): Boolean =
        gameMode == GameMode.DRAW_SPECIAL && !isBranchOpenFor(playerIndex, side)

    fun getPlayableTiles(playerIndex: Int): List<DominoTile> {
        val player = players[playerIndex]
        return player.hand.filter { getPlayableSides(playerIndex, it).isNotEmpty() }
    }

    /**
     * Estrategia que debe decidir por el asiento indicado.
     *
     * Se resuelve en cada turno (no se cachea) a proposito: en los tests se cambian
     * personalidades y overrides sobre la marcha y el motor debe reflejarlo.
     */
    fun strategyFor(playerIndex: Int): DominoStrategy =
        strategyOverrides[playerIndex]
            ?: LegacyStrategy(players.getOrNull(playerIndex)?.personality ?: Personality.NICE)

    /** Fija la estrategia de un asiento. Sirve para enfrentar dos estrategias en la mesa. */
    fun setStrategyOverride(playerIndex: Int, strategy: DominoStrategy) {
        strategyOverrides[playerIndex] = strategy
    }

    /** Deja el asiento sin estrategia inyectada: vuelve a la referencia historica. */
    fun clearStrategyOverride(playerIndex: Int) {
        strategyOverrides.remove(playerIndex)
    }

    /** Quita todas las estrategias inyectadas. */
    fun clearAllStrategyOverrides() {
        strategyOverrides.clear()
    }

    /**
     * Construye la vista que necesita una estrategia de IA para decidir.
     *
     * Incluye lo publico (cadena, extremos, pozo, marcadores) y las jugadas legales ya
     * resueltas por el motor, pero nunca las manos de los demas. La legalidad se calcula
     * aqui, en el unico sitio que conoce las reglas de todas las modalidades.
     */
    fun viewFor(playerIndex: Int): PlayerView {
        val player = players[playerIndex]
        val moves = player.hand.flatMap { tile ->
            getPlayableSides(playerIndex, tile).map { side -> AiMove.Play(tile, side) }
        }
        return PlayerView(
            playerIndex = playerIndex,
            myHand = player.hand,
            boardChain = boardChain.toList(),
            leftOpenPip = leftOpenPip,
            rightOpenPip = rightOpenPip,
            boneyardSize = boneyard.size,
            gameMode = gameMode,
            isTeamGame = isTeamGame,
            scores = players.map { it.score },
            turnNumber = turnNumber,
            consecutivePasses = consecutivePasses,
            playableMoves = moves,
            ownBranch = branchForPlayer(playerIndex),
            lastPassTurnNumber = if (lastPassingPlayerIndex != null) lastPassTurnNumber else null,
            lastPassingPlayerIndex = lastPassingPlayerIndex
        )
    }

    /**
     * Cálculo del siguiente hueco de una rama a partir de la plantilla de recorrido
     * (coordenadas y orientación en el espacio virtual de 240x320).
     */
    fun getNextSlotForSide(side: BoardSide): NextSlotPlacement {
        if (boardChain.isEmpty()) {
            return NextSlotPlacement(virtX = 105, virtY = 119, isVertical = false, isForward = true)
        }
        val firstIndex = boardChain.indexOfFirst { it.isFirstTile }.let { if (it == -1) 0 else it }
        val branchSteps = if (side == BoardSide.LEFT) currentPattern.leftBranch else currentPattern.rightBranch
        val branchCount = if (side == BoardSide.LEFT) firstIndex else (boardChain.size - 1 - firstIndex)
        val prevTile = if (side == BoardSide.LEFT) boardChain.first() else boardChain.last()

        val s2 = branchSteps.size
        var n6 = branchCount
        var n7 = n6 + 1
        if (n6 >= s2) {
            n6 = 4 + (n6 - 4) % (s2 - 4)
            n7 = n6 + 1
        }
        if (n7 >= s2) {
            n7 = 4 + (n7 - 4) % (s2 - 4)
            n6 = n7 - 1
        }

        val stepPrev = branchSteps[n6]
        val stepCurr = branchSteps[n7]
        val dx = stepCurr.dx - stepPrev.dx
        val dy = stepCurr.dy - stepPrev.dy

        return NextSlotPlacement(
            virtX = prevTile.virtX + dx,
            virtY = prevTile.virtY + dy,
            isVertical = stepCurr.isVertical,
            isForward = stepCurr.isForward
        )
    }

    fun playTileLeft(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.LEFT)
    }

    fun playTileRight(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.RIGHT)
    }

    private fun resolveConnectingAndOpenPips(tile: DominoTile, target: Int): Pair<Int, Int> {
        return if (gameMode == GameMode.MATADOR) {
            if (isMatadorTile(tile)) {
                Pair(tile.low, tile.high)
            } else if (tile.high + target == 7) {
                Pair(tile.high, tile.low)
            } else {
                Pair(tile.low, tile.high)
            }
        } else {
            if (tile.high == target) {
                Pair(tile.high, tile.low)
            } else {
                Pair(tile.low, tile.high)
            }
        }
    }

    private fun playTileOnSide(playerIndex: Int, tile: DominoTile, side: BoardSide): Boolean {
        if (isRoundOver) return false
        val player = players[playerIndex]
        if (!player.hand.contains(tile)) return false

        // Validación de jugada: coincidencia de puntos con el extremo y, en Draw Special,
        // que la rama esté realmente disponible para este jugador.
        if (boardChain.isNotEmpty() && !getPlayableSides(playerIndex, tile).contains(side)) return false

        val newHand = player.hand.toMutableList()
        newHand.remove(tile)
        players[playerIndex] = player.copy(hand = newHand)
        consecutivePasses = 0
        turnNumber++

        if (boardChain.isEmpty()) {
            // Primera ficha de la mano en el centro del tablero virtual, en horizontal (31x17).
            // Quien abre de verdad define el reparto de ramas de Draw Special.
            openingPlayerIndex = playerIndex
            leftOpenPip = tile.high
            rightOpenPip = tile.low
            boardChain.add(
                BoardPlacedTile(
                    tile = tile,
                    pipLeft = tile.high,
                    pipRight = tile.low,
                    isDouble = tile.isDouble,
                    isFirstTile = true,
                    virtX = 105,
                    virtY = 119,
                    isVertical = false,
                    branchSide = null,
                    branchStepIndex = 0
                )
            )
        } else if (side == BoardSide.LEFT) {
            val target = leftOpenPip!!
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.LEFT)
            val firstIdx = boardChain.indexOfFirst { it.isFirstTile }.let { if (it == -1) 0 else it }

            val pipTopOrLeft = if (slot.isForward) connectingPip else newOpenPip
            val pipBottomOrRight = if (slot.isForward) newOpenPip else connectingPip

            boardChain.add(
                0,
                BoardPlacedTile(
                    tile = tile,
                    pipLeft = pipTopOrLeft,
                    pipRight = pipBottomOrRight,
                    isDouble = tile.isDouble,
                    isFirstTile = false,
                    virtX = slot.virtX,
                    virtY = slot.virtY,
                    isVertical = slot.isVertical,
                    branchSide = BoardSide.LEFT,
                    branchStepIndex = firstIdx + 1
                )
            )
            leftOpenPip = newOpenPip
        } else {
            // side == BoardSide.RIGHT
            val target = rightOpenPip!!
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.RIGHT)
            val firstIdx = boardChain.indexOfFirst { it.isFirstTile }.let { if (it == -1) 0 else it }
            val rightStep = boardChain.size - firstIdx

            val pipTopOrLeft = if (slot.isForward) connectingPip else newOpenPip
            val pipBottomOrRight = if (slot.isForward) newOpenPip else connectingPip

            boardChain.add(
                BoardPlacedTile(
                    tile = tile,
                    pipLeft = pipTopOrLeft,
                    pipRight = pipBottomOrRight,
                    isDouble = tile.isDouble,
                    isFirstTile = false,
                    virtX = slot.virtX,
                    virtY = slot.virtY,
                    isVertical = slot.isVertical,
                    branchSide = BoardSide.RIGHT,
                    branchStepIndex = rightStep
                )
            )
            rightOpenPip = newOpenPip
        }

        roundLog = "${player.name} jugó $tile"
        evaluatePointScoring(playerIndex)

        if (newHand.isEmpty()) {
            finishRound(winnerIndex = playerIndex, byDomino = true)
            return true
        }

        if (gameMode == GameMode.BLOCK_HUNGARIAN) {
            val morePlayable = getPlayableTiles(playerIndex)
            if (morePlayable.isNotEmpty()) {
                canContinueHungarianTurn = true
                roundLog = "${player.name} sigue tirando (Húngaro)"
                return true
            }
        }

        canContinueHungarianTurn = false
        advanceTurn()
        return true
    }

    private fun evaluatePointScoring(playerIndex: Int) {
        val sum = (leftOpenPip ?: 0) + (rightOpenPip ?: 0)
        var pointsEarned = 0

        when (gameMode) {
            GameMode.ALL_FIVES -> {
                if (sum > 0 && sum % 5 == 0) pointsEarned = sum
            }
            GameMode.ALL_THREES -> {
                if (sum > 0 && sum % 3 == 0) pointsEarned = sum
            }
            GameMode.FIVES_AND_THREES -> {
                // Regla clásica: 1 punto por cada 5 y 1 punto por cada 3.
                // Extremos sumando 15 => 3 (por el 5) + 5 (por el 3) = 8 puntos.
                val fives = if (sum > 0 && sum % 5 == 0) sum / 5 else 0
                val threes = if (sum > 0 && sum % 3 == 0) sum / 3 else 0
                pointsEarned = fives + threes
            }
            else -> {}
        }

        if (pointsEarned > 0) {
            val p = players[playerIndex]
            val newScore = p.score + pointsEarned
            players[playerIndex] = p.copy(score = newScore)
            roundLog = "${p.name} +$pointsEarned pts (Extremos: $sum)"
            if (newScore >= targetScore) {
                isMatchOver = true
                winningTeam = p.team
            }
        }
    }

    fun drawTile(playerIndex: Int, boneyardIndex: Int = 0): DominoTile? {
        if (!gameMode.allowsDraw || boneyard.isEmpty()) return null
        val safeIdx = boneyardIndex.coerceIn(0, boneyard.lastIndex)
        val drawn = boneyard.removeAt(safeIdx)
        val p = players[playerIndex]
        players[playerIndex] = p.copy(hand = p.hand + drawn)
        roundLog = "${p.name} robó ficha del pozo."
        return drawn
    }

    fun passTurn(playerIndex: Int) {
        consecutivePasses++
        // Registrar el pase: en Draw Special abre la rama de este jugador al resto.
        lastPassingPlayerIndex = playerIndex
        lastPassTurnNumber = turnNumber
        val p = players[playerIndex]
        roundLog = "${p.name} pasa."

        if (consecutivePasses >= players.size) {
            resolveBlockedRound()
            return
        }

        advanceTurn()
    }

    fun advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    private fun resolveBlockedRound() {
        val minPips = players.minOf { it.hand.sumOf { tile -> tile.totalPips } }
        val winner = players.first { it.hand.sumOf { tile -> tile.totalPips } == minPips }
        roundLog = "¡Cerrado! Gana ${winner.name} ($minPips pts en mano)."
        finishRound(winnerIndex = winner.id, byDomino = false)
    }

    private fun finishRound(winnerIndex: Int, byDomino: Boolean) {
        isRoundOver = true
        // La ronda ha terminado: no puede quedar pendiente un encadenamiento húngaro,
        // o la UI seguiría pidiendo tirar cuando la mano ya está cerrada.
        canContinueHungarianTurn = false
        winnerPlayerId = winnerIndex
        val winner = players[winnerIndex]
        winningTeam = winner.team

        val totalOpponentPips = players.filter { it.team != winner.team }
            .sumOf { it.hand.sumOf { tile -> tile.totalPips } }

        val newScore = winner.score + totalOpponentPips
        players[winnerIndex] = winner.copy(score = newScore)

        if (byDomino) {
            roundLog = "¡DOMINÓ de ${winner.name}! (+$totalOpponentPips pts)"
        }

        if (newScore >= targetScore) {
            isMatchOver = true
        }
    }

    /**
     * Turno de un jugador automatico: pide la decision a su estrategia y la aplica.
     *
     * La estrategia solo propone; aqui se valida y se ejecuta. Si devuelve una jugada que
     * no esta entre las legales, se descarta y se cae al comportamiento por defecto de la
     * estrategia heredada, para que una estrategia defectuosa no pueda corromper la partida.
     */
    fun executeAIMove(): Boolean {
        val seat = currentPlayerIndex
        val view = viewFor(seat)
        lastAiMove = strategyFor(seat).decide(view)

        val play = lastAiMove as? AiMove.Play
        if (play != null && view.playableMoves.contains(play)) {
            return if (play.side == BoardSide.LEFT) {
                playTileLeft(seat, play.tile)
            } else {
                playTileRight(seat, play.tile)
            }
        }

        // Jugada no legal (o una estrategia que no supo que hacer): se usa la referencia.
        if (play != null && view.playableMoves.isNotEmpty()) {
            val fallback = LegacyStrategy(players[seat].personality).decide(view)
            lastAiMove = fallback
            val safe = fallback as? AiMove.Play ?: return fallbackTurn(seat)
            return if (safe.side == BoardSide.LEFT) {
                playTileLeft(seat, safe.tile)
            } else {
                playTileRight(seat, safe.tile)
            }
        }

        return fallbackTurn(seat)
    }

    /** Robar si se puede y si no pasar, para cuando la estrategia no devolvio una jugada util. */
    private fun fallbackTurn(playerIndex: Int): Boolean {
        if (gameMode.allowsDraw && boneyard.isNotEmpty()) {
            drawTile(playerIndex, 0)
        } else {
            passTurn(playerIndex)
        }
        return true
    }
}
