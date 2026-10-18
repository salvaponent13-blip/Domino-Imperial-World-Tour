package com.example.dominograndprix.engine

import com.example.dominograndprix.data.MatchSnapshot
import com.example.dominograndprix.engine.ai.AiMove
import com.example.dominograndprix.engine.ai.DominoStrategy
import com.example.dominograndprix.engine.ai.EasyStrategy
import com.example.dominograndprix.engine.ai.LegacyStrategy
import com.example.dominograndprix.engine.ai.MediumStrategy
import com.example.dominograndprix.engine.ai.MediumWeights
import com.example.dominograndprix.engine.ai.MoveSimulation
import com.example.dominograndprix.engine.ai.PlayerView
import com.example.dominograndprix.engine.ai.StrategyCatalog
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
    val playerCount: Int = 4,
    val patternIndex: Int = 3, // Patrón por defecto: escalones simétricos en S
    /**
     * Generador aleatorio del motor. Se inyecta para poder reproducir una partida entera:
     * con la misma semilla, dos motores reparten las mismas manos ronda tras ronda.
     * Es la base del benchmark de IA y del desafio diario. En produccion se deja el
     * generador por defecto.
     */
    private val random: Random = Random.Default,
    /**
     * Semilla con la que se construyo [random], si se conoce. Se guarda en la partida para
     * poder reanudarla (ver `MatchSnapshot`).
     */
    var randomSeed: Long = 0L,
    /**
     * Nivel del rival. Decide con que estrategia juegan los asientos automaticos: `LEGACY` es
     * el comportamiento historico (y el valor por defecto, para no alterar tests ni
     * benchmarks), `RELAXED` juega sin criterio y `STANDARD`/`TOUGH` con la heuristica medida.
     */
    val difficulty: Difficulty = Difficulty.LEGACY,
    /** Personalidades iniciales de los cuatro asientos, en orden. */
    val personalities: List<Personality> = DEFAULT_PERSONALITIES,
    /**
     * Nombres de los asientos, en orden. Si se omite, se usan los del modo carrera.
     *
     * En pass-and-play local la interfaz pasa "Jugador 1", "Jugador 2"... en el idioma activo,
     * porque "Tu" o "Carlos" no tienen sentido cuando las cuatro sillas las ocupan personas
     * que comparten el telefono.
     */
    val playerNames: List<String> = SEAT_NAMES,
    /**
     * Asientos ocupados por personas.
     *
     * Por defecto VACIO: todos los asientos los juega la IA. Ese es el escenario de los
     * benchmarks y de los tests de reglas, que estudian el juego en si. La interfaz declara
     * siempre quien juega: el modo carrera pasa `setOf(0)` y el pass-and-play local, el
     * conjunto de asientos de las personas.
     *
     * El motor no decide por los asientos humanos: [executeAIMove] se niega a actuar en ellos
     * de forma incondicional, porque mover una ficha ajena seria peor que no mover.
     */
    humanPlayerIndices: Set<Int> = emptySet()
) {
    /**
     * Asientos que juegan personas, en lectura publica y escritura controlada.
     *
     * Se puede reconfigurar despues de crear el motor (la interfaz lo hace al arrancar la
     * partida) para no tener que reconstruirlo, pero nadie de fuera puede cambiarlo sin pasar
     * por [setHumanPlayerIndices].
     */
    var humanPlayerIndices: Set<Int> = humanPlayerIndices.toSet()
        private set

    /**
     * Declara que asientos juegan personas.
     *
     * Refresca tambien la bandera `isHuman` de cada asiento: el reparto inicial se hace en el
     * constructor y puede haber ocurrido antes de que la interfaz sepa cuantos jugadores hay,
     * asi que sin esta sincronizacion las banderas quedarian desfasadas respecto al conjunto.
     */
    fun setHumanPlayerIndices(seats: Set<Int>) {
        humanPlayerIndices = seats.toSet()
        players.indices.forEach { seat ->
            players[seat] = players[seat].copy(isHuman = seat in humanPlayerIndices)
        }
    }

    /**
     * Renombra los asientos.
     *
     * El pass-and-play local usa nombres neutros ("Jugador 1", "Jugador 2") porque "Tu" o
     * "Carlos" no tienen sentido cuando las sillas las ocupan personas que comparten telefono.
     */
    fun setPlayerNames(names: List<String>) {
        players.indices.forEach { seat ->
            val name = names.getOrNull(seat) ?: return@forEach
            players[seat] = players[seat].copy(name = name)
        }
    }

    companion object {
        /**
         * Personalidades por defecto de los cuatro asientos: el jugador, dos rivales y el
         * companero. Se conservan como estaban para no cambiar el comportamiento historico.
         */
        val DEFAULT_PERSONALITIES: List<Personality> = listOf(
            Personality.CONFIDENT, // Tú
            Personality.AGGRESSIVE, // Carlos
            Personality.NICE, // Mateo (compañero)
            Personality.CONFIDENT // Rodrigo
        )

        /** Nombres de los asientos, en orden. */
        val SEAT_NAMES = listOf("Tú", "Carlos", "Mateo", "Rodrigo")

        /** Equipos: 0 y 2 contra 1 y 3. */
        private val SEAT_TEAMS = listOf(0, 1, 0, 1)

        /**
         * Motor SIN ninguna persona: todos los asientos los juega la IA.
         *
         * Es lo que necesitan los benchmarks y los tests de reglas, que estudian el juego en si
         * y no la interaccion con un jugador. Existe como fabrica con nombre propio para que no
         * haya que recordar que el valor por defecto del constructor si tiene una persona.
         */
        fun allAi(
            gameMode: GameMode,
            targetScore: Int = 100,
            seed: Long = 0L,
            patternIndex: Int = 3,
            difficulty: Difficulty = Difficulty.LEGACY
        ): DominoEngine = DominoEngine(
            gameMode = gameMode,
            targetScore = targetScore,
            patternIndex = patternIndex,
            random = Random(seed),
            randomSeed = seed,
            difficulty = difficulty
        )

        /**
         * Arranca una partida nueva con una semilla conocida, para que el guardado posterior
         * pueda reconstruirla.
         */
        fun newMatch(
            gameMode: GameMode,
            targetScore: Int,
            difficulty: Difficulty,
            seed: Long = System.currentTimeMillis(),
            patternIndex: Int = 3,
            playerCount: Int = 4,
            isTeamGame: Boolean = true
        ): DominoEngine = DominoEngine(
            gameMode = gameMode,
            targetScore = targetScore,
            patternIndex = patternIndex,
            random = Random(seed),
            randomSeed = seed,
            difficulty = difficulty,
            playerCount = playerCount,
            isTeamGame = isTeamGame
        )

        /**
         * Reanuda la partida descrita por [snapshot].
         *
         * El motor nace con la semilla guardada y las identidades de los asientos, y despues
         * se le aplica el estado. Deja la ronda cerrada, que es el punto en el que se guardo.
         */
        fun resume(snapshot: MatchSnapshot): DominoEngine = DominoEngine(
            gameMode = snapshot.gameMode,
            targetScore = snapshot.targetScore,
            patternIndex = snapshot.patternIndex,
            random = Random(snapshot.randomSeed),
            randomSeed = snapshot.randomSeed,
            difficulty = snapshot.difficulty,
            personalities = snapshot.personalities,
            humanPlayerIndices = snapshot.humanPlayerIndices
        ).apply { restoreFrom(snapshot) }
    }

    // Deck & Boneyard
    var boneyard: MutableList<DominoTile> = mutableListOf()
    val players: MutableList<Player> = mutableListOf()

    // Board Chain (Left to Right)
    val boardChain: MutableList<BoardPlacedTile> = mutableListOf()
    var leftOpenPip: Int? = null
    var rightOpenPip: Int? = null
    var upOpenPip: Int? = null
    var downOpenPip: Int? = null
    var spinnerTile: DominoTile? = null

    // Turn control
    var currentPlayerIndex: Int = 0
    var consecutivePasses: Int = 0
    var isRoundOver: Boolean = false
    var isMatchOver: Boolean = false
    var winnerPlayerId: Int? = null
    var winningTeam: Int? = null
    var roundLog: String = ""

    /** Contador de colocaciones, para dar un identificador unico a cada ficha puesta. */
    private var placementCounter: Int = 0

    /**
     * Si la ronda que acaba de cerrarse termino con la mesa trancada (nadie podia jugar) en
     * lugar de porque alguien vaciara la mano. Lo usan las estadisticas de carrera.
     */
    var lastRoundWasBlocked: Boolean = false
        private set

    /** Puntos que otorgo la ultima jugada (0 en modos que no puntuan extremos). */
    var lastPointsEarned: Int = 0
        private set

    /** Bonificación por cierre de dominó (con doble, capicúa o doble capicúa). */
    var lastRoundBonusType: DominoBonusType = DominoBonusType.NONE
        private set

    // --- Acumuladores de la PARTIDA ---
    //
    // Los indicadores de ronda (lastRoundWasBlocked, winnerPlayerId) solo describen la ULTIMA
    // ronda. Estos acumulan lo ocurrido en toda la partida, que es lo que necesitan las
    // estadisticas de carrera: sin ellos, una partida de cinco rondas se contaria como una.

    /** Rondas ganadas por el bando humano en esta partida. */
    var matchRoundsWon: Int = 0
        private set

    /**
     * Rondas cerradas en esta partida, las gane quien las gane.
     *
     * Es un contador aparte y no `matchRoundsWon + matchBlockedRounds`: una ronda que gana el
     * rival no es ninguna de las dos, asi que aquella suma no da el total de rondas jugadas.
     */
    var matchRoundsPlayed: Int = 0
        private set

    /** Rondas que termino la mesa trancada en esta partida. */
    var matchBlockedRounds: Int = 0
        private set

    /** Rondas que gano el bando humano vaciando la mano. */
    var matchDominos: Int = 0
        private set

    /** Rondas que gano el bando humano con la mesa trancada. */
    var matchTrancasWon: Int = 0
        private set

    /** Rondas que gano un asiento humano con una sola ficha en la mano. */
    var matchWinsWithSingleTile: Int = 0
        private set

    /** Puntos de extremos anotados por los asientos humanos en toda la partida. */
    var matchPointsScored: Int = 0
        private set

    /**
     * Marcador mas alto alcanzado por el bando del jugador en una sola ronda de esta partida.
     *
     * Lo necesitan las misiones de sede del tipo "consigue N puntos en una ronda": el total
     * acumulado no sirve, porque premiaria arrastrar puntos de muchas rondas en lugar de
     * conseguir una ronda buena.
     */
    var matchBestRoundPoints: Int = 0
        private set

    /** Si el jugador robo alguna vez del pozo en esta partida. */
    var matchDrewFromBoneyard: Boolean = false
        private set

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

    /**
     * Si el turno pertenece a una persona. La interfaz lo usa para saber si debe esperar una
     * jugada del jugador o si el motor puede resolver el turno solo.
     */
    val isCurrentPlayerHuman: Boolean
        get() = currentPlayerIndex in humanPlayerIndices

    /** Asientos automaticos: los que no ocupa una persona. */
    val aiPlayerIndices: Set<Int>
        get() = players.indices.toSet() - humanPlayerIndices
    


    // Jugadores que han pasado y aún no han podido volver a colocar ficha.
    // En Draw Special mantiene abierta la rama del rival hasta que este vuelva a jugar.
    private val blockedPlayers = mutableSetOf<Int>()
    private var lastPassingPlayerIndex: Int? = null
    private var lastPassTurnNumber: Int = -1
    private var turnNumber: Int = 0

    val currentPattern: BoardPatternTemplate
        get() = DominoLayoutTemplates.PATTERNS[patternIndex.coerceIn(0, DominoLayoutTemplates.PATTERNS.lastIndex)]

    init {
        setupGame()
    }

    fun setupGame() {
        // Los acumuladores de partida se reinician aqui, que es donde empieza una partida nueva.
        // No en startNewRound(): eso borraria las estadisticas en cada ronda.
        matchRoundsWon = 0
        matchRoundsPlayed = 0
        matchBlockedRounds = 0
        matchDominos = 0
        matchTrancasWon = 0
        matchWinsWithSingleTile = 0
        matchPointsScored = 0
        matchBestRoundPoints = 0
        matchDrewFromBoneyard = false
        isMatchOver = false
        winnerPlayerId = null
        winningTeam = null
        players.clear()
        val numSeats = playerCount.coerceIn(2, 4)
        for (seat in 0 until numSeats) {
            val team = if (isTeamGame && numSeats == 4) SEAT_TEAMS[seat] else seat
            players.add(
                Player(
                    id = seat,
                    name = playerNames.getOrElse(seat) { SEAT_NAMES.getOrElse(seat) { "Asiento $seat" } },
                    isHuman = seat in humanPlayerIndices,
                    personality = personalities.getOrElse(seat) { Personality.NICE },
                    team = team
                )
            )
        }
        startNewRound()
    }

    fun startNewRound() {
        startNewRound(openerOverride = null)
    }

    /**
     * Empieza una ronda nueva.
     *
     * @param openerOverride asiento que abre, o `null` para aplicar la regla del doble mas alto.
     *   El arrastre de mano ("tener la salida") es una ventaja tactica real en domino, asi que
     *   quien gana una ronda abre la siguiente. La interfaz pasa aqui el ganador anterior; el
     *   motor no lo deduce solo porque el llamante puede querer otra cosa (por ejemplo, un test
     *   que fije el orden de turnos).
     */
    fun startNewRound(openerOverride: Int?) {
        boardChain.clear()
        leftOpenPip = null
        rightOpenPip = null
        upOpenPip = null
        downOpenPip = null
        spinnerTile = null
        consecutivePasses = 0
        isRoundOver = false
        canContinueHungarianTurn = false
        blockedPlayers.clear()
        lastPassingPlayerIndex = null
        lastPassTurnNumber = -1
        turnNumber = 0
        lastPointsEarned = 0
        lastRoundWasBlocked = false
        lastRoundBonusType = DominoBonusType.NONE

        // Generate standard Double-6 domino set (28 tiles)
        val deck = mutableListOf<DominoTile>()
        for (i in 0..6) {
            for (j in i..6) {
                deck.add(DominoTile(j, i))
            }
        }
        // Barajado con el generador inyectado: fijando su semilla, la partida es reproducible.
        deck.shuffle(random)

        // En 1v1 (2 jugadores), 7 fichas cada uno. En 4 jugadores: Draw reparte 5 y Block reparte 7.
        val tilesPerPlayer = if (players.size == 2) 7 else if (gameMode.allowsDraw) 5 else 7
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
        // Arrastre de mano: si viene un apertura impuesta, manda sobre el doble mas alto.
        if (openerOverride != null && openerOverride in players.indices) {
            startingPlayer = openerOverride
        }
        currentPlayerIndex = startingPlayer
        openingPlayerIndex = startingPlayer
        val starterName = players[currentPlayerIndex].name
        roundLog = "$starterName abre la mano."
    }

    /**
     * Asiento que debe abrir la ronda SIGUIENTE a la que acaba de cerrarse.
     *
     * Arrastre de mano, regla clasica de club: tener la salida es una ventaja tactica real, asi
     * que abre quien gano. Si la ronda se cerro por tranca, gana quien menos puntos tiene en la
     * mano, que es el mismo criterio con el que se resolvio la ronda.
     *
     * Devuelve `null` si no hay ronda cerrada de la que sacar conclusion, para que el llamante
     * deje actuar a la regla por defecto (el doble mas alto).
     */
    fun nextRoundOpener(): Int? {
        if (!isRoundOver) return null
        return winnerPlayerId
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
     * está bloqueado (ha pasado y aún no ha colocado ninguna ficha).
     */
    private fun isBranchOpenFor(playerIndex: Int, side: BoardSide): Boolean {
        if (gameMode != GameMode.DRAW_SPECIAL) return true
        val own = branchForPlayer(playerIndex)
        if (own == side) return true
        return blockedPlayers.any { blockedSeat ->
            blockedSeat != playerIndex && branchForPlayer(blockedSeat) == side
        }
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
        if (gameMode == GameMode.ALL_FIVES && spinnerTile != null) {
            if (canConnectUp(tile)) sides.add(BoardSide.UP)
            if (canConnectDown(tile)) sides.add(BoardSide.DOWN)
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
     * En cada turno se resuelve asi:
     * 1. Si el asiento tiene una estrategia inyectada (benchmarks y pruebas), se usa.
     * 2. Si no, se construye la que corresponde a su dificultad y personalidad.
     *
     * No se cachea a proposito: en los tests se cambian personalidades y overrides sobre la
     * marcha y el motor debe reflejarlo.
     */
    fun strategyFor(playerIndex: Int): DominoStrategy {
        strategyOverrides[playerIndex]?.let { return it }
        val personality = players.getOrNull(playerIndex)?.personality ?: Personality.NICE
        return when (difficulty) {
            // Referencia historica: es la que miden los benchmarks como baseline.
            Difficulty.LEGACY -> LegacyStrategy(personality)
            // Rival de iniciacion: no tiene criterio, elige al azar entre sus jugadas.
            Difficulty.RELAXED -> EasyStrategy(Random(randomSeed + playerIndex))
            Difficulty.STANDARD, Difficulty.TOUGH -> MediumStrategy(
                weights = StrategyCatalog.weightsFor(personality, difficulty)
            ) { move -> simulate(playerIndex, move) }
        }
    }

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
     * Crea una estrategia MEDIA enlazada con la simulacion de ESTE motor y atada a un asiento.
     *
     * El asiento es obligatorio para simular: una estrategia decide para un jugador concreto,
     * y si simulara para el jugador que tiene el turno en ese instante, la evaluacion seria
     * sobre otra mano. Atarlo aqui hace imposible ese error.
     *
     * La estrategia no puede simular por su cuenta porque necesita las reglas de colocacion,
     * que viven en el motor; esta fabrica es el punto de union, y evita que la estrategia
     * reciba una referencia al motor entero con capacidad de mutarlo.
     */
    fun newMediumStrategy(
        playerIndex: Int,
        weights: MediumWeights = MediumWeights.ALL
    ): MediumStrategy = MediumStrategy(weights) { move -> simulate(playerIndex, move) }

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
        val firstTilePlaced = boardChain.firstOrNull { it.isFirstTile }
        val isFirstDouble = firstTilePlaced?.isDouble == true

        if (gameMode == GameMode.ALL_FIVES) {
            val spinnerPlaced = boardChain.firstOrNull { it.tile == spinnerTile }
                ?: firstTilePlaced
                ?: boardChain.first()
            val sx = spinnerPlaced.virtX
            val sy = spinnerPlaced.virtY

            val steps = when (side) {
                BoardSide.LEFT -> DominoLayoutTemplates.FOUR_BRANCH_PATTERN.leftBranch
                BoardSide.RIGHT -> DominoLayoutTemplates.FOUR_BRANCH_PATTERN.rightBranch
                BoardSide.UP -> DominoLayoutTemplates.FOUR_BRANCH_PATTERN.upBranch
                BoardSide.DOWN -> DominoLayoutTemplates.FOUR_BRANCH_PATTERN.downBranch
            }
            val branchCount = boardChain.count { it.branchSide == side }
            val stepIdx = (branchCount + 1).coerceAtMost(steps.lastIndex)
            val step = steps[stepIdx]

            return NextSlotPlacement(
                virtX = sx + step.dx,
                virtY = sy + step.dy,
                isVertical = step.isVertical,
                isForward = step.isForward
            )
        }

        if (side == BoardSide.UP) {
            val prevUp = boardChain.lastOrNull { it.branchSide == BoardSide.UP }
            return if (prevUp != null) {
                NextSlotPlacement(virtX = prevUp.virtX, virtY = prevUp.virtY - 29, isVertical = true, isForward = false)
            } else {
                val spinnerPlaced = boardChain.firstOrNull { it.tile == spinnerTile } ?: firstTilePlaced
                val sx = spinnerPlaced?.virtX ?: 105
                val sy = spinnerPlaced?.virtY ?: 119
                NextSlotPlacement(virtX = sx, virtY = sy - 29, isVertical = true, isForward = false)
            }
        }
        if (side == BoardSide.DOWN) {
            val prevDown = boardChain.lastOrNull { it.branchSide == BoardSide.DOWN }
            return if (prevDown != null) {
                NextSlotPlacement(virtX = prevDown.virtX, virtY = prevDown.virtY + 29, isVertical = true, isForward = true)
            } else {
                val spinnerPlaced = boardChain.firstOrNull { it.tile == spinnerTile } ?: firstTilePlaced
                val sx = spinnerPlaced?.virtX ?: 105
                val sy = spinnerPlaced?.virtY ?: 119
                NextSlotPlacement(virtX = sx, virtY = sy + 29, isVertical = true, isForward = true)
            }
        }

        if (side == BoardSide.LEFT) {
            val branchCount = boardChain.count { it.branchSide == BoardSide.LEFT }
            if (branchCount == 0 && isFirstDouble) {
                // El spinner inicial está vertical (17x31) en (105, 119).
                // Conectar rama izquierda horizontal (31x17) tocando el lado izquierdo:
                // virtX = 105 - 29 = 76, virtY = 119 + 7 = 126
                return NextSlotPlacement(virtX = 76, virtY = 126, isVertical = false, isForward = false)
            }
            val branchSteps = currentPattern.leftBranch
            val prevTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
                ?: firstTilePlaced
                ?: boardChain.first()

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
        } else {
            // BoardSide.RIGHT
            val branchCount = boardChain.count { it.branchSide == BoardSide.RIGHT }
            if (branchCount == 0 && isFirstDouble) {
                // El spinner inicial está vertical (17x31) en (105, 119) con borde derecho en 122.
                // Conectar rama derecha horizontal (31x17) tocando el lado derecho (con solapamiento 2px):
                // virtX = 105 + 15 = 120, virtY = 119 + 7 = 126
                return NextSlotPlacement(virtX = 120, virtY = 126, isVertical = false, isForward = true)
            }
            val branchSteps = currentPattern.rightBranch
            val prevTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
                ?: firstTilePlaced
                ?: boardChain.last()

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
    }

    fun playTileLeft(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.LEFT)
    }

    fun playTileRight(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.RIGHT)
    }

    fun playTileUp(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.UP)
    }

    fun playTileDown(playerIndex: Int, tile: DominoTile): Boolean {
        return playTileOnSide(playerIndex, tile, BoardSide.DOWN)
    }

    fun playTile(playerIndex: Int, tile: DominoTile, side: BoardSide): Boolean =
        playTileOnSide(playerIndex, tile, side)

    fun canConnectUp(tile: DominoTile): Boolean {
        if (gameMode != GameMode.ALL_FIVES || spinnerTile == null) return false
        val target = upOpenPip ?: spinnerTile!!.high
        return if (gameMode == GameMode.MATADOR) {
            (tile.high + target == 7) || (tile.low + target == 7) || isMatadorTile(tile)
        } else {
            tile.hasPip(target)
        }
    }

    fun canConnectDown(tile: DominoTile): Boolean {
        if (gameMode != GameMode.ALL_FIVES || spinnerTile == null) return false
        val target = downOpenPip ?: spinnerTile!!.high
        return if (gameMode == GameMode.MATADOR) {
            (tile.high + target == 7) || (tile.low + target == 7) || isMatadorTile(tile)
        } else {
            tile.hasPip(target)
        }
    }

    private fun resolveConnectingAndOpenPips(tile: DominoTile, target: Int): Pair<Int, Int> {
        return if (gameMode == GameMode.MATADOR) {
            if (tile.high + target == 7) {
                Pair(tile.high, tile.low)
            } else if (tile.low + target == 7) {
                Pair(tile.low, tile.high)
            } else if (isMatadorTile(tile)) {
                // Comodín Matador jugado cuando no suma 7 directo (o sobre un 0/blanca)
                Pair(tile.low, tile.high)
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

        val wasCapicuaCandidate = if (player.hand.size == 1 && boardChain.isNotEmpty()) {
            val leftPip = leftOpenPip
            val rightPip = rightOpenPip
            leftPip != null && rightPip != null && canConnectLeft(tile) && canConnectRight(tile)
        } else false

        val newHand = player.hand.toMutableList()
        newHand.remove(tile)
        players[playerIndex] = player.copy(hand = newHand)
        consecutivePasses = 0
        turnNumber++
        blockedPlayers.remove(playerIndex)

        if (boardChain.isEmpty()) {
            // Primera ficha de la mano en el centro del tablero virtual, en horizontal (31x17).
            // Quien abre de verdad define el reparto de ramas de Draw Special.
            openingPlayerIndex = playerIndex
            leftOpenPip = tile.high
            rightOpenPip = tile.low
            upOpenPip = null
            downOpenPip = null
            spinnerTile = if (tile.isDouble) tile else null
            boardChain.add(
                BoardPlacedTile(
                    tile = tile,
                    pipLeft = tile.high,
                    pipRight = tile.low,
                    isDouble = tile.isDouble,
                    isFirstTile = true,
                    virtX = 105,
                    virtY = 119,
                    isVertical = if (gameMode == GameMode.ALL_FIVES) false else tile.isDouble,
                    branchSide = null,
                    branchStepIndex = 0,
                    placementId = ++placementCounter
                )
            )
        } else if (side == BoardSide.LEFT) {
            val target = leftOpenPip!!
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.LEFT)
            val leftStep = boardChain.count { it.branchSide == BoardSide.LEFT } + 1

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
                    branchStepIndex = leftStep,
                    placementId = ++placementCounter
                )
            )
            leftOpenPip = newOpenPip
        } else if (side == BoardSide.RIGHT) {
            val target = rightOpenPip!!
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.RIGHT)
            val rightStep = boardChain.count { it.branchSide == BoardSide.RIGHT } + 1

            val pipTopOrLeft = if (slot.isForward) connectingPip else newOpenPip
            val pipBottomOrRight = if (slot.isForward) newOpenPip else connectingPip

            val lastRightIdx = boardChain.indexOfLast { it.branchSide == BoardSide.RIGHT }
            val insertIdx = if (lastRightIdx != -1) {
                lastRightIdx + 1
            } else {
                val firstIdx = boardChain.indexOfFirst { it.isFirstTile }
                if (firstIdx != -1) firstIdx + 1 else boardChain.size
            }

            boardChain.add(
                insertIdx,
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
                    branchStepIndex = rightStep,
                    placementId = ++placementCounter
                )
            )
            rightOpenPip = newOpenPip
        } else if (side == BoardSide.UP) {
            val target = upOpenPip ?: spinnerTile!!.high
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.UP)
            val upStep = boardChain.count { it.branchSide == BoardSide.UP } + 1

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
                    branchSide = BoardSide.UP,
                    branchStepIndex = upStep,
                    placementId = ++placementCounter
                )
            )
            upOpenPip = newOpenPip
        } else if (side == BoardSide.DOWN) {
            val target = downOpenPip ?: spinnerTile!!.high
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val slot = getNextSlotForSide(BoardSide.DOWN)
            val downStep = boardChain.count { it.branchSide == BoardSide.DOWN } + 1

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
                    branchSide = BoardSide.DOWN,
                    branchStepIndex = downStep,
                    placementId = ++placementCounter
                )
            )
            downOpenPip = newOpenPip
        }

        roundLog = "${player.name} jugó $tile"
        evaluatePointScoring(playerIndex)

        if (newHand.isEmpty()) {
            lastRoundBonusType = when {
                tile.isDouble && wasCapicuaCandidate -> DominoBonusType.DOUBLE_CAPICUA
                tile.isDouble -> DominoBonusType.DOUBLE
                wasCapicuaCandidate -> DominoBonusType.CAPICUA
                else -> DominoBonusType.NONE
            }
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

    /**
     * Calcula la suma de los extremos abiertos para los modos de puntos (All Fives, All Threes, Fives & Threes).
     * En las reglas oficiales de dominó americano (All Fives / Muggins), cuando una ficha doble queda en un
     * extremo abierto sin cubrir, sus puntos cuentan por ambos extremos (doble).
     * Por ejemplo, [6|6] en un extremo y [4|4] en el otro suman 12 + 8 = 20 puntos.
     */
    fun calculateOpenEndsSum(): Int {
        if (boardChain.isEmpty()) return 0
        if (boardChain.size == 1) {
            val tile = boardChain.first()
            return if (tile.isDouble) tile.pipLeft * 2 else tile.pipLeft + tile.pipRight
        }
        val firstTile = boardChain.firstOrNull { it.isFirstTile }
        val leftTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
            ?: firstTile
            ?: boardChain.first()
        val rightTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
            ?: firstTile
            ?: boardChain.last()

        val leftVal = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) (leftOpenPip ?: 0) * 2 else (leftOpenPip ?: 0)
        val rightVal = if (rightTile.isDouble && rightTile.tile.high == rightOpenPip) (rightOpenPip ?: 0) * 2 else (rightOpenPip ?: 0)
        var sum = leftVal + rightVal

        if (gameMode == GameMode.ALL_FIVES) {
            if (upOpenPip != null) {
                val upTile = boardChain.lastOrNull { it.branchSide == BoardSide.UP }
                val upVal = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) upOpenPip!! * 2 else upOpenPip!!
                sum += upVal
            }
            if (downOpenPip != null) {
                val downTile = boardChain.lastOrNull { it.branchSide == BoardSide.DOWN }
                val downVal = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) downOpenPip!! * 2 else downOpenPip!!
                sum += downVal
            }
        }
        return sum
    }

    /**
     * Texto explicativo y transparente de los extremos abiertos para el HUD, desglosando
     * las fichas dobles y la suma de puntos en juego.
     */
    fun getOpenEndsEvaluationText(): String {
        if (boardChain.isEmpty()) return ""
        if (boardChain.size == 1) {
            val tile = boardChain.first()
            val pts = if (tile.isDouble) tile.pipLeft * 2 else tile.pipLeft + tile.pipRight
            val desc = if (tile.isDouble) "[${tile.pipLeft}|${tile.pipLeft}=$pts]" else "[${tile.pipLeft}]+[${tile.pipRight}]"
            val isScoring = when (gameMode) {
                GameMode.ALL_FIVES -> pts > 0 && pts % 5 == 0
                GameMode.ALL_THREES -> pts > 0 && pts % 3 == 0
                GameMode.FIVES_AND_THREES -> pts > 0 && (pts % 5 == 0 || pts % 3 == 0)
                else -> false
            }
            return if (isScoring) "$desc = $pts pts ⭐" else "$desc = $pts"
        }
        val firstTile = boardChain.firstOrNull { it.isFirstTile }
        val leftTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
            ?: firstTile
            ?: boardChain.first()
        val rightTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
            ?: firstTile
            ?: boardChain.last()

        val leftVal = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) (leftOpenPip ?: 0) * 2 else (leftOpenPip ?: 0)
        val rightVal = if (rightTile.isDouble && rightTile.tile.high == rightOpenPip) (rightOpenPip ?: 0) * 2 else (rightOpenPip ?: 0)

        val parts = mutableListOf<String>()
        val lDesc = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) "[$leftOpenPip|$leftOpenPip=$leftVal]" else "[$leftOpenPip]"
        val rDesc = if (rightTile.isDouble && rightTile.tile.high == rightOpenPip) "[$rightOpenPip|$rightOpenPip=$rightVal]" else "[$rightOpenPip]"
        parts.add(lDesc)
        parts.add(rDesc)

        if (gameMode == GameMode.ALL_FIVES) {
            if (upOpenPip != null) {
                val upTile = boardChain.lastOrNull { it.branchSide == BoardSide.UP }
                val upVal = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) upOpenPip!! * 2 else upOpenPip!!
                val uDesc = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) "[$upOpenPip|$upOpenPip=$upVal]" else "[$upOpenPip]"
                parts.add(uDesc)
            }
            if (downOpenPip != null) {
                val downTile = boardChain.lastOrNull { it.branchSide == BoardSide.DOWN }
                val downVal = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) downOpenPip!! * 2 else downOpenPip!!
                val dDesc = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) "[$downOpenPip|$downOpenPip=$downVal]" else "[$downOpenPip]"
                parts.add(dDesc)
            }
        }
        val sum = calculateOpenEndsSum()
        val isScoring = when (gameMode) {
            GameMode.ALL_FIVES -> sum > 0 && sum % 5 == 0
            GameMode.ALL_THREES -> sum > 0 && sum % 3 == 0
            GameMode.FIVES_AND_THREES -> sum > 0 && (sum % 5 == 0 || sum % 3 == 0)
            else -> false
        }
        val badge = parts.joinToString(" + ")
        return if (isScoring) "$badge = $sum pts ⭐" else if (gameMode in listOf(GameMode.ALL_FIVES, GameMode.ALL_THREES, GameMode.FIVES_AND_THREES)) "$badge = $sum" else badge
    }

    /** Puntos acumulados por un equipo. */
    fun teamScore(team: Int): Int =
        players.filter { it.team == team }.sumOf { it.score }

    private fun evaluatePointScoring(playerIndex: Int) {
        val sum = calculateOpenEndsSum()
        var pointsEarned = 0
        lastPointsEarned = 0

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
            lastPointsEarned = pointsEarned
            val p = players[playerIndex]
            val newScore = p.score + pointsEarned
            players[playerIndex] = p.copy(score = newScore)
            roundLog = "${p.name} +$pointsEarned pts (Extremos: $sum)"
            if (teamScore(p.team) >= targetScore) {
                isMatchOver = true
                winningTeam = p.team
                winnerPlayerId = playerIndex
                finishRound(winnerIndex = playerIndex, byDomino = false)
            }
        }
    }

    fun drawTile(playerIndex: Int, boneyardIndex: Int = 0): DominoTile? {
        if (!gameMode.allowsDraw || boneyard.isEmpty()) return null
        val safeIdx = boneyardIndex.coerceIn(0, boneyard.lastIndex)
        val drawn = boneyard.removeAt(safeIdx)
        val p = players[playerIndex]
        players[playerIndex] = p.copy(hand = p.hand + drawn)
        // Se recuerda para las misiones de sede del tipo "gana sin robar del pozo": hay que
        // saber si el jugador robo ALGUNA VEZ, no si le quedaban fichas al terminar.
        matchDrewFromBoneyard = true
        roundLog = "${p.name} robó ficha del pozo."
        return drawn
    }

    fun passTurn(playerIndex: Int) {
        consecutivePasses++
        // Registrar el pase: en Draw Special abre la rama de este jugador al resto mientras siga bloqueado.
        blockedPlayers.add(playerIndex)
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
        lastRoundWasBlocked = true
        if (!isTeamGame) {
            // Partida individual: gana el jugador con menos puntos en la mano
            val winner = players.minByOrNull { it.hand.sumOf { t -> t.totalPips } }
                ?: players.getOrElse(openingPlayerIndex) { players.first() }
            val minPips = winner.hand.sumOf { it.totalPips }
            roundLog = "¡Mesa trancada! Gana ${winner.name} con $minPips pts en mano."
            finishRound(winnerIndex = winner.id, byDomino = false)
            return
        }

        // En partida 2v2 se comparan los puntos totales de cada equipo (regla oficial de parejas)
        val team0Pips = players.filter { it.team == 0 }.sumOf { it.hand.sumOf { t -> t.totalPips } }
        val team1Pips = players.filter { it.team == 1 }.sumOf { it.hand.sumOf { t -> t.totalPips } }

        val winTeam = if (team0Pips < team1Pips) {
            0
        } else if (team1Pips < team0Pips) {
            1
        } else {
            // Regla oficial clásica de desempate en tranca: en caso de empate a puntos,
            // desempata la salida (la pareja del jugador que abrió la ronda).
            players.getOrNull(openingPlayerIndex)?.team ?: 0
        }

        val winPips = if (winTeam == 0) team0Pips else team1Pips
        val losingPips = if (winTeam == 0) team1Pips else team0Pips
        val winner = players.filter { it.team == winTeam }
            .minByOrNull { it.hand.sumOf { t -> t.totalPips } } ?: players.first { it.team == winTeam }

        roundLog = if (team0Pips == team1Pips) {
            "¡Tranca empatada ($team0Pips pts)! Desempata la salida: gana equipo ${winTeam + 1}."
        } else {
            "¡Cerrado! Gana equipo ${winTeam + 1} ($winPips pts vs $losingPips pts)."
        }
        finishRound(winnerIndex = winner.id, byDomino = false)
    }

    private fun finishRound(winnerIndex: Int, byDomino: Boolean) {
        // Una ronda solo se cierra una vez. Sin esta guarda, cerrar dos veces contaria dos
        // rondas y corromperia las estadisticas de carrera.
        if (isRoundOver) return
        isRoundOver = true
        lastRoundWasBlocked = !byDomino
        // La ronda ha terminado: no puede quedar pendiente un encadenamiento húngaro,
        // o la UI seguiría pidiendo tirar cuando la mano ya está cerrada.
        canContinueHungarianTurn = false
        winnerPlayerId = winnerIndex
        val winner = players[winnerIndex]
        winningTeam = winner.team

        // En ALL_FIVES (fidelidad a MagnoJuegos) los puntos se ganan en la mesa durante la partida,
        // no sumando pips brutos de las manos al cerrar.
        val totalOpponentPips = if (gameMode == GameMode.ALL_FIVES) 0 else {
            players.filter { it.team != winner.team }
                .sumOf { it.hand.sumOf { tile -> tile.totalPips } }
        }

        val newScore = winner.score + totalOpponentPips
        players[winnerIndex] = winner.copy(score = newScore)

        if (byDomino) {
            val bonusTag = when (lastRoundBonusType) {
                DominoBonusType.DOUBLE_CAPICUA -> " 👑🌟 ¡DOBLE CAPICÚA!"
                DominoBonusType.DOUBLE -> " 👑 ¡CON DOBLE!"
                DominoBonusType.CAPICUA -> " 🌟 ¡CAPICÚA!"
                DominoBonusType.NONE -> ""
            }
            roundLog = if (totalOpponentPips > 0) {
                "¡DOMINÓ de ${winner.name}!$bonusTag (+$totalOpponentPips pts)"
            } else {
                "¡DOMINÓ de ${winner.name}!$bonusTag"
            }
        }

        if (teamScore(winner.team) >= targetScore) {
            isMatchOver = true
        }

        // --- Acumuladores de la PARTIDA (no de la ronda) ---
        //
        // Hacen falta porque los indicadores de ronda (lastRoundWasBlocked, winnerPlayerId)
        // solo describen la ULTIMA ronda. Sin esto, una partida de cinco rondas se contaria
        // como una sola en las estadisticas de carrera.
        //
        // El "bando del jugador" es SIEMPRE el equipo del asiento 0, que es el punto de vista
        // de la carrera y del pass-and-play (los asientos 0 y 2 son equipos distintos). Se usa
        // esto y no "el equipo de un asiento humano" porque en los benchmarks y los tests de
        // reglas no hay ningun asiento humano y el contador quedaria muerto.
        val playerTeam = players.firstOrNull()?.team ?: 0
        matchRoundsPlayed++
        if (winner.team == playerTeam) {
            matchRoundsWon++
            if (byDomino) matchDominos++ else matchTrancasWon++
            // Una sola ficha en la mano: victoria apurada. Solo puede pasar ganando por tranca,
            // porque quien cierra con domino se queda sin ninguna.
            if (winner.hand.size == 1) matchWinsWithSingleTile++
            matchPointsScored += lastPointsEarned
            if (lastPointsEarned > matchBestRoundPoints) matchBestRoundPoints = lastPointsEarned
        }
        if (!byDomino) matchBlockedRounds++
    }

    /**
     * Turno de un jugador automatico: pide la decision a su estrategia y la aplica.
     *
     * La estrategia solo propone; aqui se valida y se ejecuta. Si devuelve una jugada que
     * no esta entre las legales, se descarta y se cae al comportamiento por defecto de la
     * estrategia heredada, para que una estrategia defectuosa no pueda corromper la partida.
     */
    fun executeAIMove(): Boolean {
        // Con la partida o la ronda ya cerradas no hay turno que jugar. Seguir moviendo fichas
        // aqui inflaria las estadisticas de la partida y ademas no tendria sentido.
        if (isMatchOver || isRoundOver) return false

        // Salvaguarda incondicional: el motor NO mueve el turno de una persona. Si esto se
        // dispara, es un fallo de quien llama (la interfaz solo debe pedir turno a la IA), y
        // no mover es preferible a mover una ficha ajena.
        if (isCurrentPlayerHuman) return false

        val seat = currentPlayerIndex
        val view = viewFor(seat)
        lastAiMove = strategyFor(seat).decide(view)

        val play = lastAiMove as? AiMove.Play
        if (play != null && view.playableMoves.contains(play)) {
            return playTile(seat, play.tile, play.side)
        }

        // Jugada no legal (o una estrategia que no supo que hacer): se usa la referencia.
        if (play != null && view.playableMoves.isNotEmpty()) {
            val fallback = LegacyStrategy(players[seat].personality).decide(view)
            lastAiMove = fallback
            val safe = fallback as? AiMove.Play ?: return fallbackTurn(seat)
            return playTile(seat, safe.tile, safe.side)
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

    // ------------------------------------------------------------------
    // Guardado y reanudacion de la partida
    // ------------------------------------------------------------------

    /**
     * Fotografia de la partida para poder reanudarla.
     *
     * Solo tiene sentido llamarlo con la ronda cerrada (`isRoundOver`): es el punto que se
     * acordo guardar, porque a mitad de jugada hay estados en vuelo (encadenamiento hungaro,
     * ventana de invasion) que complican la restauracion sin aportar valor al jugador.
     */
    fun toSnapshot(cityName: String, venueId: String?, betAmount: Int): MatchSnapshot =
        MatchSnapshot(
            gameMode = gameMode,
            difficulty = difficulty,
            targetScore = targetScore,
            patternIndex = patternIndex,
            cityName = cityName,
            venueId = venueId,
            betAmount = betAmount,
            scores = players.map { it.score },
            isMatchOver = isMatchOver,
            winningTeam = winningTeam,
            personalities = players.map { it.personality },
            humanPlayerIndices = humanPlayerIndices,
            boardChain = boardChain.toList(),
            hands = players.map { it.hand },
            leftOpenPip = leftOpenPip,
            rightOpenPip = rightOpenPip,
            winnerPlayerId = winnerPlayerId,
            roundLog = roundLog,
            randomSeed = randomSeed
        )

    /**
     * Reanuda la partida descrita por [snapshot].
     *
     * Deja el motor tal y como estaba al cerrar la ronda: marcadores, tablero, manos y el
     * ganador de esa ronda. `isRoundOver` queda en `true` para que la interfaz muestre el
     * resultado de la ronda y el jugador continue con la siguiente, que es exactamente el
     * punto en el que se guardo.
     */
    fun restoreFrom(snapshot: MatchSnapshot) {
        players.clear()
        snapshot.hands.forEachIndexed { seat, hand ->
            players.add(
                Player(
                    id = seat,
                    name = SEAT_NAMES.getOrElse(seat) { "Asiento $seat" },
                    // Los asientos humanos vienen del guardado y no de la configuracion actual:
                    // una partida local debe reanudarse con las mismas personas en la mesa.
                    isHuman = seat in snapshot.humanPlayerIndices,
                    personality = snapshot.personalities.getOrElse(seat) { Personality.NICE },
                    hand = hand,
                    score = snapshot.scores.getOrElse(seat) { 0 },
                    team = SEAT_TEAMS.getOrElse(seat) { 0 }
                )
            )
        }

        boardChain.clear()
        boardChain.addAll(snapshot.boardChain)
        // Se continua la numeracion por encima de lo restaurado para no repetir identificadores.
        placementCounter = snapshot.boardChain.maxOfOrNull { it.placementId } ?: 0
        boneyard.clear()
        leftOpenPip = snapshot.leftOpenPip
        rightOpenPip = snapshot.rightOpenPip

        isRoundOver = true
        isMatchOver = snapshot.isMatchOver
        winnerPlayerId = snapshot.winnerPlayerId
        winningTeam = snapshot.winningTeam
        roundLog = snapshot.roundLog
        canContinueHungarianTurn = false
        consecutivePasses = 0
        lastPassingPlayerIndex = null
        lastPassTurnNumber = -1
        lastPointsEarned = 0

        // El turno se reanuda en un asiento humano: la siguiente accion del jugador es
        // confirmar el fin de ronda y empezar la siguiente.
        currentPlayerIndex = humanPlayerIndices.minOrNull() ?: 0
        openingPlayerIndex = currentPlayerIndex
        turnNumber = 0
    }

    /**
     * Instantanea de todo el estado mutable del motor.
     *
     * Se copia TODO lo que puede cambiar una jugada, no solo lo evidente. Si algun dia se
     * anade un campo de estado y se olvida incluirlo aqui, la simulacion lo filtraria a la
     * partida real: por eso hay un test que compara el estado antes y despues de simular.
     */
    private class StateSnapshot(
        val players: List<Player>,
        val boardChain: List<BoardPlacedTile>,
        val boneyard: List<DominoTile>,
        val leftOpenPip: Int?,
        val rightOpenPip: Int?,
        val upOpenPip: Int?,
        val downOpenPip: Int?,
        val spinnerTile: DominoTile?,
        val currentPlayerIndex: Int,
        val consecutivePasses: Int,
        val isRoundOver: Boolean,
        val isMatchOver: Boolean,
        val winnerPlayerId: Int?,
        val winningTeam: Int?,
        val roundLog: String,
        val lastPointsEarned: Int,
        val canContinueHungarianTurn: Boolean,
        val openingPlayerIndex: Int,
        val blockedPlayers: Set<Int>,
        val lastPassingPlayerIndex: Int?,
        val lastPassTurnNumber: Int,
        val turnNumber: Int,
        // Los acumuladores de partida entran en la instantanea porque `simulate()` aplica
        // jugadas de verdad sobre el motor y las deshace despues. Si no se guardaran y
        // restauraran, una simulacion que cierra una ronda contaria una ronda que nunca
        // ocurrio: dos cierres reales se contaban como cuatro.
        val matchRoundsPlayed: Int,
        val matchRoundsWon: Int,
        val matchBlockedRounds: Int,
        val matchDominos: Int,
        val matchTrancasWon: Int,
        val matchWinsWithSingleTile: Int,
        val matchPointsScored: Int,
        val matchBestRoundPoints: Int,
        val matchDrewFromBoneyard: Boolean,
        val lastRoundBonusType: DominoBonusType
    )

    private fun snapshot(): StateSnapshot = StateSnapshot(
        players = players.toList(),
        boardChain = boardChain.toList(),
        boneyard = boneyard.toList(),
        leftOpenPip = leftOpenPip,
        rightOpenPip = rightOpenPip,
        upOpenPip = upOpenPip,
        downOpenPip = downOpenPip,
        spinnerTile = spinnerTile,
        currentPlayerIndex = currentPlayerIndex,
        consecutivePasses = consecutivePasses,
        isRoundOver = isRoundOver,
        isMatchOver = isMatchOver,
        winnerPlayerId = winnerPlayerId,
        winningTeam = winningTeam,
        roundLog = roundLog,
        lastPointsEarned = lastPointsEarned,
        canContinueHungarianTurn = canContinueHungarianTurn,
        openingPlayerIndex = openingPlayerIndex,
        blockedPlayers = blockedPlayers.toSet(),
        lastPassingPlayerIndex = lastPassingPlayerIndex,
        lastPassTurnNumber = lastPassTurnNumber,
        turnNumber = turnNumber,
        matchRoundsPlayed = matchRoundsPlayed,
        matchRoundsWon = matchRoundsWon,
        matchBlockedRounds = matchBlockedRounds,
        matchDominos = matchDominos,
        matchTrancasWon = matchTrancasWon,
        matchWinsWithSingleTile = matchWinsWithSingleTile,
        matchPointsScored = matchPointsScored,
        matchBestRoundPoints = matchBestRoundPoints,
        matchDrewFromBoneyard = matchDrewFromBoneyard,
        lastRoundBonusType = lastRoundBonusType
    )

    private fun restore(snapshot: StateSnapshot) {
        players.clear()
        players.addAll(snapshot.players)
        boardChain.clear()
        boardChain.addAll(snapshot.boardChain)
        boneyard.clear()
        boneyard.addAll(snapshot.boneyard)
        leftOpenPip = snapshot.leftOpenPip
        rightOpenPip = snapshot.rightOpenPip
        upOpenPip = snapshot.upOpenPip
        downOpenPip = snapshot.downOpenPip
        spinnerTile = snapshot.spinnerTile
        currentPlayerIndex = snapshot.currentPlayerIndex
        consecutivePasses = snapshot.consecutivePasses
        isRoundOver = snapshot.isRoundOver
        isMatchOver = snapshot.isMatchOver
        winnerPlayerId = snapshot.winnerPlayerId
        winningTeam = snapshot.winningTeam
        roundLog = snapshot.roundLog
        lastPointsEarned = snapshot.lastPointsEarned
        canContinueHungarianTurn = snapshot.canContinueHungarianTurn
        openingPlayerIndex = snapshot.openingPlayerIndex
        blockedPlayers.clear()
        blockedPlayers.addAll(snapshot.blockedPlayers)
        lastPassingPlayerIndex = snapshot.lastPassingPlayerIndex
        lastPassTurnNumber = snapshot.lastPassTurnNumber
        turnNumber = snapshot.turnNumber
        matchRoundsPlayed = snapshot.matchRoundsPlayed
        matchRoundsWon = snapshot.matchRoundsWon
        matchBlockedRounds = snapshot.matchBlockedRounds
        matchDominos = snapshot.matchDominos
        matchTrancasWon = snapshot.matchTrancasWon
        matchWinsWithSingleTile = snapshot.matchWinsWithSingleTile
        matchPointsScored = snapshot.matchPointsScored
        matchBestRoundPoints = snapshot.matchBestRoundPoints
        matchDrewFromBoneyard = snapshot.matchDrewFromBoneyard
        lastRoundBonusType = snapshot.lastRoundBonusType
    }

    /**
     * Aplica [move] sobre una copia del estado y devuelve sus consecuencias observables,
     * dejando la partida exactamente como estaba.
     *
     * Es lo que permite a una estrategia razonar sobre el FUTURO (¿esta jugada tranca la
     * mesa? ¿gana la ronda? ¿cuantos puntos da?) sin duplicar las reglas de colocacion, que
     * viven aqui y solo aqui.
     */
    fun simulate(playerIndex: Int, move: AiMove.Play): MoveSimulation {
        val snapshot = snapshot()
        try {
            val applied = playTile(playerIndex, move.tile, move.side)
            check(applied) { "No se puede simular una jugada ilegal: $move" }

            val hand = players[playerIndex].hand
            val opponents = players.filter { it.team != players[playerIndex].team }
                .sumOf { player -> player.hand.sumOf { it.totalPips } }

            return MoveSimulation(
                move = move,
                myRemainingTiles = hand.size,
                myRemainingPips = hand.sumOf { it.totalPips },
                myRemainingDoubles = hand.count { it.isDouble },
                opponentsPips = opponents,
                ownTeamPips = players.filter { it.team == players[playerIndex].team }
                    .sumOf { player -> player.hand.sumOf { it.totalPips } },
                leftOpenPip = leftOpenPip,
                rightOpenPip = rightOpenPip,
                blocksTheBoard = isBoardBlocked(),
                pointsEarned = lastPointsEarned,
                winsTheRound = isRoundOver
            )
        } finally {
            restore(snapshot)
        }
    }

    /**
     * ¿La mesa esta trancada? Es decir: nadie puede jugar y, en los modos con robo, tampoco
     * puede nadie seguir sacando fichas del pozo. Sin la comprobacion del pozo, un modo de
     * robo en el que todos tuvieran que robar se reportaria como trancado sin serlo.
     */
    private fun isBoardBlocked(): Boolean {
        val anyoneCanPlay = players.indices.any { getPlayableTiles(it).isNotEmpty() }
        if (anyoneCanPlay) return false
        return !gameMode.allowsDraw || boneyard.isEmpty()
    }
}
