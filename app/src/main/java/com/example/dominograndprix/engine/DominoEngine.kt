package com.example.dominograndprix.engine

import com.example.dominograndprix.model.*
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
    val patternIndex: Int = 3 // Default Pattern 3: Symmetric Square-Steps from Domino Fever
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

        // Generate standard Double-6 domino set (28 tiles)
        val deck = mutableListOf<DominoTile>()
        for (i in 0..6) {
            for (j in i..6) {
                deck.add(DominoTile(j, i))
            }
        }
        deck.shuffle(Random(System.currentTimeMillis()))

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
        val starterName = players[currentPlayerIndex].name
        roundLog = "$starterName abre la mano."
    }

    fun isMatadorTile(tile: DominoTile): Boolean {
        return (tile.high == 6 && tile.low == 1) ||
                (tile.high == 5 && tile.low == 2) ||
                (tile.high == 4 && tile.low == 3) ||
                (tile.high == 0 && tile.low == 0)
    }

    fun canConnectLeft(tile: DominoTile): Boolean {
        if (boardChain.isEmpty()) return true
        val target = leftOpenPip ?: return false
        if (gameMode == GameMode.MATADOR) {
            if (isMatadorTile(tile)) return true
            if (target == 0) return isMatadorTile(tile)
            return (tile.high + target == 7 || tile.low + target == 7)
        }
        return tile.hasPip(target)
    }

    fun canConnectRight(tile: DominoTile): Boolean {
        if (boardChain.isEmpty()) return true
        val target = rightOpenPip ?: return false
        if (gameMode == GameMode.MATADOR) {
            if (isMatadorTile(tile)) return true
            if (target == 0) return isMatadorTile(tile)
            return (tile.high + target == 7 || tile.low + target == 7)
        }
        return tile.hasPip(target)
    }

    fun getPlayableTiles(playerIndex: Int): List<DominoTile> {
        val player = players[playerIndex]
        return player.hand.filter { canConnectLeft(it) || canConnectRight(it) }
    }

    /**
     * Exact Domino Fever b.java (lines 4261-4314) slot calculation for the next tile on a branch.
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

        // Validate connection
        if (boardChain.isNotEmpty()) {
            val valid = if (side == BoardSide.LEFT) canConnectLeft(tile) else canConnectRight(tile)
            if (!valid) return false
        }

        val newHand = player.hand.toMutableList()
        newHand.remove(tile)
        players[playerIndex] = player.copy(hand = newHand)
        consecutivePasses = 0

        if (boardChain.isEmpty()) {
            // First tile on board at exact Domino Fever center (105, 119), horizontal (31x17)
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
                val fives = if (sum > 0 && sum % 5 == 0) sum / 5 else 0
                val threes = if (sum > 0 && sum % 3 == 0) sum / 3 else 0
                pointsEarned = if (fives > 0 && threes > 0) (fives + threes) * 2 else fives + threes
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
     * AI logic: chooses best valid move (left or right). If no move and draw allowed, draws until playable or empty.
     */
    fun executeAIMove(): Boolean {
        val playable = getPlayableTiles(currentPlayerIndex)

        if (playable.isEmpty()) {
            if (gameMode.allowsDraw && boneyard.isNotEmpty()) {
                drawTile(currentPlayerIndex, 0)
                return true
            } else {
                passTurn(currentPlayerIndex)
                return true
            }
        }

        val player = players[currentPlayerIndex]
        val chosenTile = when (player.personality) {
            Personality.AGGRESSIVE -> playable.maxByOrNull { it.totalPips } ?: playable.first()
            else -> playable.first()
        }

        val canLeft = canConnectLeft(chosenTile)
        val canRight = canConnectRight(chosenTile)

        return if (canLeft && canRight) {
            // Balance left and right branches so both snake symmetrically
            val firstIdx = boardChain.indexOfFirst { it.isFirstTile }.let { if (it == -1) 0 else it }
            val leftLen = firstIdx
            val rightLen = boardChain.size - 1 - firstIdx
            if (leftLen <= rightLen) playTileLeft(currentPlayerIndex, chosenTile)
            else playTileRight(currentPlayerIndex, chosenTile)
        } else if (canLeft) {
            playTileLeft(currentPlayerIndex, chosenTile)
        } else {
            playTileRight(currentPlayerIndex, chosenTile)
        }
    }
}
