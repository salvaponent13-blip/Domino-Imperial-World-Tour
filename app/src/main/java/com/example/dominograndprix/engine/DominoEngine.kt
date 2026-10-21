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
    var firstPlacedTile: BoardPlacedTile? = null
    val leftBranchTiles: MutableList<BoardPlacedTile> = mutableListOf()
    val rightBranchTiles: MutableList<BoardPlacedTile> = mutableListOf()
    val upBranchTiles: MutableList<BoardPlacedTile> = mutableListOf()
    val downBranchTiles: MutableList<BoardPlacedTile> = mutableListOf()

    /**
     * En ALL_FIVES, en cuanto hay un doble en el centro, la franja central queda reservada
     * para los brazos ARRIBA/ABAJO y las serpentinas giran antes de entrar en ella.
     */
    private val reservesCenterCorridor: Boolean
        get() = gameMode == GameMode.ALL_FIVES && spinnerTile != null

    fun recomputeBoardLayout() {
        boardChain.clear()
        if (firstPlacedTile == null) return
        val laidOut = DominoBoardLayout.layoutBoardTiles(
            firstTile = firstPlacedTile,
            leftBranch = leftBranchTiles,
            rightBranch = rightBranchTiles,
            upBranch = upBranchTiles,
            downBranch = downBranchTiles,
            crossMode = reservesCenterCorridor
        )
        boardChain.addAll(laidOut)
    }
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

    /**
     * Puntos que el CIERRE de la ronda sumo al ganador: los pips de los rivales, o los de la
     * jugada que alcanzo la meta en los modos de puntos. Es lo que debe mostrar el resumen de
     * ronda; [lastPointsEarned] es solo de la ultima jugada y no sirve para eso.
     */
    var lastRoundPointsAwarded: Int = 0
        private set

    /** Puntos de mesa ganados por cada asiento durante la ronda en curso (modos de puntos). */
    val roundPointsByPlayer: MutableMap<Int, Int> = mutableMapOf()

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

    // Flag de control de robo: solo se permite robar una vez por turno
    var hasDrawnThisTurn: Boolean = false

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
    var turnNumber: Int = 0
        private set

    // Registro de fallas por jugador ("Pasó al X"): números a los que cada jugador ha pasado en esta ronda
    val playerPassedPips = mutableMapOf<Int, MutableSet<Int>>()

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
        firstPlacedTile = null
        leftBranchTiles.clear()
        rightBranchTiles.clear()
        upBranchTiles.clear()
        downBranchTiles.clear()
        leftOpenPip = null
        rightOpenPip = null
        upOpenPip = null
        downOpenPip = null
        spinnerTile = null
        consecutivePasses = 0
        isRoundOver = false
        canContinueHungarianTurn = false
        hasDrawnThisTurn = false
        blockedPlayers.clear()
        playerPassedPips.clear()
        lastPassingPlayerIndex = null
        lastPassTurnNumber = -1
        turnNumber = 0
        lastPointsEarned = 0
        lastRoundPointsAwarded = 0
        roundPointsByPlayer.clear()
        undoPoint = null
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
            lastPassingPlayerIndex = lastPassingPlayerIndex,
            upOpenPip = upOpenPip,
            downOpenPip = downOpenPip,
            spinnerTile = spinnerTile
        )
    }

    /**
     * Cálculo del siguiente hueco de una rama (coordenadas y orientación en la rejilla virtual
     * de 14 unidades). [candidateTile] permite previsualizar el hueco real de una ficha
     * concreta: un doble no ocupa lo mismo ni se orienta igual que una ficha normal.
     */
    fun getNextSlotForSide(side: BoardSide, candidateTile: DominoTile? = null): NextSlotPlacement {
        return DominoBoardLayout.getNextSlotPlacement(
            firstTile = firstPlacedTile,
            leftBranch = leftBranchTiles,
            rightBranch = rightBranchTiles,
            upBranch = upBranchTiles,
            downBranch = downBranchTiles,
            side = side,
            candidateTile = candidateTile,
            crossMode = reservesCenterCorridor
        )
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

    /**
     * En Dominó de Cinco (All Fives), si la primera ficha jugada no fue un doble,
     * el primer doble que se juegue posteriormente se convierte en el "spinner" (cruz de 4 ramas).
     * Para que las 4 ramas se desenvuelvan sin colisionar en sus cuadrantes respectivos:
     * - El nuevo doble se centra en (105, 119) como ficha de origen.
     * - La cadena previa se re-enruta a lo largo de la rama contraria (LEFT si se jugó a la derecha,
     *   o RIGHT si se jugó a la izquierda).
     * - Quedan desbloqueadas las 4 ramas en cruz (UP, DOWN, LEFT, RIGHT).
     */
    private fun reslotChainForLateSpinner(tile: DominoTile, side: BoardSide) {
        val doubleVal = tile.high
        val isPlayedOnRight = (side == BoardSide.RIGHT)
        val targetBranch = if (isPlayedOnRight) BoardSide.LEFT else BoardSide.RIGHT

        val tilesFromDouble = if (isPlayedOnRight) boardChain.reversed() else boardChain.toList()

        val reslotted = mutableListOf<BoardPlacedTile>()
        var prevOutwardPip = doubleVal

        for (i in tilesFromDouble.indices) {
            val old = tilesFromDouble[i]
            val connectingPip = prevOutwardPip
            val outwardPip = if (old.tile.high == connectingPip) old.tile.low else old.tile.high
            prevOutwardPip = outwardPip

            reslotted.add(
                old.copy(
                    pipLeft = connectingPip,
                    pipRight = outwardPip,
                    isFirstTile = false,
                    branchSide = targetBranch,
                    branchStepIndex = i + 1,
                    connectedPip = connectingPip,
                    openEndPip = outwardPip
                )
            )
        }

        val centerDouble = BoardPlacedTile(
            tile = tile,
            pipLeft = doubleVal,
            pipRight = doubleVal,
            isDouble = true,
            isFirstTile = true,
            virtX = 0,
            virtY = 0,
            isVertical = true,
            branchSide = null,
            branchStepIndex = 0,
            placementId = ++placementCounter,
            connectedPip = null,
            openEndPip = null
        )

        firstPlacedTile = centerDouble
        if (isPlayedOnRight) {
            leftBranchTiles.clear()
            leftBranchTiles.addAll(reslotted)
            rightBranchTiles.clear()
            leftOpenPip = prevOutwardPip
            rightOpenPip = doubleVal
        } else {
            rightBranchTiles.clear()
            rightBranchTiles.addAll(reslotted)
            leftBranchTiles.clear()
            rightOpenPip = prevOutwardPip
            leftOpenPip = doubleVal
        }
        upBranchTiles.clear()
        downBranchTiles.clear()

        spinnerTile = tile
        upOpenPip = doubleVal
        downOpenPip = doubleVal
        recomputeBoardLayout()
    }

    private fun playTileOnSide(playerIndex: Int, tile: DominoTile, side: BoardSide): Boolean {
        if (isRoundOver || isMatchOver) return false
        if (playerIndex != currentPlayerIndex) return false
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
        hasDrawnThisTurn = false
        blockedPlayers.remove(playerIndex)

        if (boardChain.isEmpty()) {
            // Primera ficha de la mano en el centro del tablero virtual
            openingPlayerIndex = playerIndex
            leftOpenPip = tile.high
            rightOpenPip = tile.low
            upOpenPip = if (gameMode == GameMode.ALL_FIVES && tile.isDouble) tile.high else null
            downOpenPip = if (gameMode == GameMode.ALL_FIVES && tile.isDouble) tile.high else null
            spinnerTile = if (tile.isDouble) tile else null
            firstPlacedTile = BoardPlacedTile(
                tile = tile,
                pipLeft = tile.high,
                pipRight = tile.low,
                isDouble = tile.isDouble,
                isFirstTile = true,
                virtX = 0,
                virtY = 0,
                isVertical = tile.isDouble,
                branchSide = null,
                branchStepIndex = 0,
                placementId = ++placementCounter,
                connectedPip = null,
                openEndPip = null
            )
            leftBranchTiles.clear()
            rightBranchTiles.clear()
            upBranchTiles.clear()
            downBranchTiles.clear()
            recomputeBoardLayout()
        } else if (side == BoardSide.LEFT) {
            if (gameMode == GameMode.ALL_FIVES && spinnerTile == null && tile.isDouble) {
                reslotChainForLateSpinner(tile, BoardSide.LEFT)
            } else {
                val target = leftOpenPip!!
                val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
                val leftStep = leftBranchTiles.size + 1
                val placed = BoardPlacedTile(
                    tile = tile,
                    pipLeft = connectingPip,
                    pipRight = newOpenPip,
                    isDouble = tile.isDouble,
                    isFirstTile = false,
                    virtX = 0,
                    virtY = 0,
                    isVertical = false,
                    branchSide = BoardSide.LEFT,
                    branchStepIndex = leftStep,
                    placementId = ++placementCounter,
                    connectedPip = connectingPip,
                    openEndPip = newOpenPip
                )
                leftBranchTiles.add(placed)
                leftOpenPip = newOpenPip
                recomputeBoardLayout()
            }
        } else if (side == BoardSide.RIGHT) {
            if (gameMode == GameMode.ALL_FIVES && spinnerTile == null && tile.isDouble) {
                reslotChainForLateSpinner(tile, BoardSide.RIGHT)
            } else {
                val target = rightOpenPip!!
                val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
                val rightStep = rightBranchTiles.size + 1
                val placed = BoardPlacedTile(
                    tile = tile,
                    pipLeft = connectingPip,
                    pipRight = newOpenPip,
                    isDouble = tile.isDouble,
                    isFirstTile = false,
                    virtX = 0,
                    virtY = 0,
                    isVertical = false,
                    branchSide = BoardSide.RIGHT,
                    branchStepIndex = rightStep,
                    placementId = ++placementCounter,
                    connectedPip = connectingPip,
                    openEndPip = newOpenPip
                )
                rightBranchTiles.add(placed)
                rightOpenPip = newOpenPip
                recomputeBoardLayout()
            }
        } else if (side == BoardSide.UP) {
            val target = upOpenPip ?: spinnerTile!!.high
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val upStep = upBranchTiles.size + 1
            val placed = BoardPlacedTile(
                tile = tile,
                pipLeft = connectingPip,
                pipRight = newOpenPip,
                isDouble = tile.isDouble,
                isFirstTile = false,
                virtX = 0,
                virtY = 0,
                isVertical = false,
                branchSide = BoardSide.UP,
                branchStepIndex = upStep,
                placementId = ++placementCounter,
                connectedPip = connectingPip,
                openEndPip = newOpenPip
            )
            upBranchTiles.add(placed)
            upOpenPip = newOpenPip
            recomputeBoardLayout()
        } else if (side == BoardSide.DOWN) {
            val target = downOpenPip ?: spinnerTile!!.high
            val (connectingPip, newOpenPip) = resolveConnectingAndOpenPips(tile, target)
            val downStep = downBranchTiles.size + 1
            val placed = BoardPlacedTile(
                tile = tile,
                pipLeft = connectingPip,
                pipRight = newOpenPip,
                isDouble = tile.isDouble,
                isFirstTile = false,
                virtX = 0,
                virtY = 0,
                isVertical = false,
                branchSide = BoardSide.DOWN,
                branchStepIndex = downStep,
                placementId = ++placementCounter,
                connectedPip = connectingPip,
                openEndPip = newOpenPip
            )
            downBranchTiles.add(placed)
            downOpenPip = newOpenPip
            recomputeBoardLayout()
        }

        roundLog = "${player.name} jugó $tile"
        // Si vuelve a jugar, las fallas que se le apuntaron dejan de estar vigentes: el chip
        // "pasó al 5" no debe quedarse pegado toda la ronda.
        playerPassedPips.remove(playerIndex)
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
     * En las reglas oficiales de dominó americano (All Fives / Muggins con spinner):
     * - Si hay una sola ficha en mesa: cuenta doble si es doble (ej. [6|6] = 12), o la suma de sus dos extremos si no.
     * - Si hay un spinner establecido en All Fives: hay 4 extremos abiertos (LEFT, RIGHT, UP, DOWN).
     *   Cada rama abierta no jugada aporta el extremo del spinner (tile.high). Una vez jugada una rama,
     *   aporta el extremo abierto de la ficha más externa (contando doble sólo si esa ficha extrema es doble).
     * - En los modos sin spinner (All Threes, Fives & Threes, o All Fives antes del primer doble):
     *   se suman únicamente los extremos LEFT y RIGHT.
     */
    fun calculateOpenEndsSum(): Int {
        if (boardChain.isEmpty()) return 0
        if (boardChain.size == 1) {
            val tile = boardChain.first()
            return if (tile.isDouble) tile.pipLeft * 2 else tile.pipLeft + tile.pipRight
        }

        val hasCrossBranches = boardChain.any { it.branchSide == BoardSide.UP || it.branchSide == BoardSide.DOWN }

        if (gameMode == GameMode.ALL_FIVES && spinnerTile != null && hasCrossBranches) {
            val leftTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
            val leftVal = if (leftTile != null && leftTile.isDouble && leftTile.tile.high == leftOpenPip) {
                (leftOpenPip ?: 0) * 2
            } else {
                leftOpenPip ?: 0
            }

            val rightTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
            val rightVal = if (rightTile != null && rightTile.isDouble && rightTile.tile.high == rightOpenPip) {
                (rightOpenPip ?: 0) * 2
            } else {
                rightOpenPip ?: 0
            }

            val upTile = boardChain.lastOrNull { it.branchSide == BoardSide.UP }
            val upVal = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) {
                (upOpenPip ?: 0) * 2
            } else {
                upOpenPip ?: 0
            }

            val downTile = boardChain.lastOrNull { it.branchSide == BoardSide.DOWN }
            val downVal = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) {
                (downOpenPip ?: 0) * 2
            } else {
                downOpenPip ?: 0
            }

            return leftVal + rightVal + upVal + downVal
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
        return leftVal + rightVal
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

        val sum = calculateOpenEndsSum()
        val parts = mutableListOf<String>()
        val hasCrossBranches = boardChain.any { it.branchSide == BoardSide.UP || it.branchSide == BoardSide.DOWN }

        if (gameMode == GameMode.ALL_FIVES && spinnerTile != null && hasCrossBranches) {
            val leftTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
            val leftVal = if (leftTile != null && leftTile.isDouble && leftTile.tile.high == leftOpenPip) (leftOpenPip ?: 0) * 2 else (leftOpenPip ?: 0)
            val lDesc = if (leftTile != null && leftTile.isDouble && leftTile.tile.high == leftOpenPip) "[$leftOpenPip|$leftOpenPip=$leftVal]" else "[$leftOpenPip]"
            parts.add(lDesc)

            val rightTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
            val rightVal = if (rightTile != null && rightTile.isDouble && rightTile.tile.high == rightOpenPip) (rightOpenPip ?: 0) * 2 else (rightOpenPip ?: 0)
            val rDesc = if (rightTile != null && rightTile.isDouble && rightTile.tile.high == rightOpenPip) "[$rightOpenPip|$rightOpenPip=$rightVal]" else "[$rightOpenPip]"
            parts.add(rDesc)

            val upTile = boardChain.lastOrNull { it.branchSide == BoardSide.UP }
            val upVal = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) (upOpenPip ?: 0) * 2 else (upOpenPip ?: 0)
            val uDesc = if (upTile != null && upTile.isDouble && upTile.tile.high == upOpenPip) "[$upOpenPip|$upOpenPip=$upVal]" else "[$upOpenPip]"
            parts.add(uDesc)

            val downTile = boardChain.lastOrNull { it.branchSide == BoardSide.DOWN }
            val downVal = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) (downOpenPip ?: 0) * 2 else (downOpenPip ?: 0)
            val dDesc = if (downTile != null && downTile.isDouble && downTile.tile.high == downOpenPip) "[$downOpenPip|$downOpenPip=$downVal]" else "[$downOpenPip]"
            parts.add(dDesc)
        } else {
            val firstTile = boardChain.firstOrNull { it.isFirstTile }
            val leftTile = boardChain.firstOrNull { it.branchSide == BoardSide.LEFT }
                ?: firstTile
                ?: boardChain.first()
            val rightTile = boardChain.lastOrNull { it.branchSide == BoardSide.RIGHT }
                ?: firstTile
                ?: boardChain.last()

            if (leftTile == rightTile) {
                val v = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) (leftOpenPip ?: 0) * 2 else (leftOpenPip ?: 0)
                val d = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) "[$leftOpenPip|$leftOpenPip=$v]" else "[$leftOpenPip]"
                parts.add(d)
            } else {
                val leftVal = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) (leftOpenPip ?: 0) * 2 else (leftOpenPip ?: 0)
                val rightVal = if (rightTile.isDouble && rightTile.tile.high == rightOpenPip) (rightOpenPip ?: 0) * 2 else (rightOpenPip ?: 0)
                val lDesc = if (leftTile.isDouble && leftTile.tile.high == leftOpenPip) "[$leftOpenPip|$leftOpenPip=$leftVal]" else "[$leftOpenPip]"
                val rDesc = if (rightTile.isDouble && rightTile.tile.high == rightOpenPip) "[$rightOpenPip|$rightOpenPip=$rightVal]" else "[$rightOpenPip]"
                parts.add(lDesc)
                parts.add(rDesc)
            }
        }

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
            roundPointsByPlayer[playerIndex] = (roundPointsByPlayer[playerIndex] ?: 0) + pointsEarned
            val p = players[playerIndex]
            val newScore = p.score + pointsEarned
            players[playerIndex] = p.copy(score = newScore)
            roundLog = "${p.name} +$pointsEarned pts (Extremos: $sum)"
            if (teamScore(p.team) >= targetScore) {
                isMatchOver = true
                winningTeam = p.team
                winnerPlayerId = playerIndex
                lastRoundPointsAwarded = pointsEarned
                finishMatchByPoints(winnerIndex = playerIndex)
            }
        }
    }

    private fun finishMatchByPoints(winnerIndex: Int) {
        if (isRoundOver) return
        isRoundOver = true
        undoPoint = null
        lastRoundWasBlocked = false
        canContinueHungarianTurn = false
        winnerPlayerId = winnerIndex
        val winner = players[winnerIndex]
        winningTeam = winner.team
        isMatchOver = true
        roundLog = "¡${winner.name} alcanza la meta de $targetScore puntos y gana la partida!"

        val playerTeam = players.firstOrNull()?.team ?: 0
        matchRoundsPlayed++
        if (winner.team == playerTeam) {
            matchRoundsWon++
            val scoredThisRound = roundPointsForTeam(playerTeam)
            matchPointsScored += scoredThisRound
            if (scoredThisRound > matchBestRoundPoints) matchBestRoundPoints = scoredThisRound
        }
    }

    fun drawTile(playerIndex: Int, boneyardIndex: Int = 0): DominoTile? {
        if (isRoundOver || isMatchOver) return null
        if (playerIndex != currentPlayerIndex) return null
        if (hasDrawnThisTurn) return null
        if (!gameMode.allowsDraw || boneyard.isEmpty()) return null
        val safeIdx = boneyardIndex.coerceIn(0, boneyard.lastIndex)
        val drawn = boneyard.removeAt(safeIdx)
        val p = players[playerIndex]
        players[playerIndex] = p.copy(hand = p.hand + drawn)
        hasDrawnThisTurn = true
        // Se recuerda para las misiones de sede del tipo "gana sin robar del pozo": hay que
        // saber si el jugador robo ALGUNA VEZ, no si le quedaban fichas al terminar.
        matchDrewFromBoneyard = true
        roundLog = "${p.name} robó ficha del pozo."
        return drawn
    }

    fun passTurn(playerIndex: Int) {
        if (isRoundOver || isMatchOver) return
        if (playerIndex != currentPlayerIndex) return
        consecutivePasses++
        // Registrar el pase: en Draw Special abre la rama de este jugador al resto mientras siga bloqueado.
        blockedPlayers.add(playerIndex)
        val currentOpenPips = listOfNotNull(leftOpenPip, rightOpenPip, upOpenPip, downOpenPip).distinct()
        playerPassedPips.getOrPut(playerIndex) { mutableSetOf() }.addAll(currentOpenPips)
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
        hasDrawnThisTurn = false
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    private fun resolveBlockedRound() {
        lastRoundWasBlocked = true
        if (!isTeamGame) {
            // Partida individual: gana el jugador con menos puntos en la mano (desempate por cercanía a la salida)
            val minPips = players.minOf { it.hand.sumOf { t -> t.totalPips } }
            val tied = players.filter { it.hand.sumOf { t -> t.totalPips } == minPips }
            val winner = if (tied.size == 1) {
                tied.first()
            } else {
                tied.minByOrNull { (it.id - openingPlayerIndex + players.size) % players.size } ?: tied.first()
            }
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
        undoPoint = null
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
        // Lo que este cierre sumo de verdad al ganador. Es la unica fuente valida para el
        // desglose del resumen de ronda (lastPointsEarned es solo de la ultima jugada).
        lastRoundPointsAwarded = totalOpponentPips

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
            // Los puntos de la ronda completos, no los de la ultima jugada: en los modos de
            // tanteo estos se acumulan jugada a jugada y lastPointsEarned solo guarda la ultima.
            val scoredThisRound = roundPointsForTeam(playerTeam)
            matchPointsScored += scoredThisRound
            if (scoredThisRound > matchBestRoundPoints) matchBestRoundPoints = scoredThisRound
        }
        if (!byDomino) matchBlockedRounds++
    }

    /**
     * Puntos que la ronda aporto al equipo indicado, tal y como se contabilizan en la carrera.
     *
     * En los modos de tanteo (All Fives y familia) los puntos se ganan durante la ronda, asi que
     * se suman los acumulados por los asientos del equipo. En el resto, el unico ingreso es el
     * cierre: los pips de los rivales, que es lo que guarda [lastRoundPointsAwarded].
     */
    private fun roundPointsForTeam(team: Int): Int {
        if (gameMode.isPointGame) {
            return players.filter { it.team == team }.sumOf { roundPointsByPlayer[it.id] ?: 0 }
        }
        val winner = players.getOrNull(winnerPlayerId ?: -1) ?: return 0
        return if (winner.team == team) lastRoundPointsAwarded else 0
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
        if (gameMode.allowsDraw && boneyard.isNotEmpty() && !hasDrawnThisTurn) {
            val drawn = drawTile(playerIndex, 0)
            if (drawn != null) {
                val playableSides = getPlayableSides(playerIndex, drawn)
                if (playableSides.isNotEmpty()) {
                    playTile(playerIndex, drawn, playableSides.first())
                    return true
                }
            }
            passTurn(playerIndex)
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
            upOpenPip = upOpenPip,
            downOpenPip = downOpenPip,
            spinnerTile = spinnerTile,
            winnerPlayerId = winnerPlayerId,
            roundLog = roundLog,
            randomSeed = randomSeed,
            lastRoundBonusType = lastRoundBonusType,
            lastRoundPointsAwarded = lastRoundPointsAwarded,
            blockedPlayers = blockedPlayers.toSet(),
            matchRoundsPlayed = matchRoundsPlayed,
            matchRoundsWon = matchRoundsWon,
            matchBlockedRounds = matchBlockedRounds,
            matchDominos = matchDominos,
            matchTrancasWon = matchTrancasWon,
            matchWinsWithSingleTile = matchWinsWithSingleTile,
            matchPointsScored = matchPointsScored,
            matchBestRoundPoints = matchBestRoundPoints,
            matchDrewFromBoneyard = matchDrewFromBoneyard
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
        firstPlacedTile = snapshot.boardChain.firstOrNull { it.isFirstTile }
        leftBranchTiles.clear()
        leftBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.LEFT }.sortedBy { it.branchStepIndex })
        rightBranchTiles.clear()
        rightBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.RIGHT }.sortedBy { it.branchStepIndex })
        upBranchTiles.clear()
        upBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.UP }.sortedBy { it.branchStepIndex })
        downBranchTiles.clear()
        downBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.DOWN }.sortedBy { it.branchStepIndex })
        placementCounter = snapshot.boardChain.maxOfOrNull { it.placementId } ?: 0
        boneyard.clear()
        leftOpenPip = snapshot.leftOpenPip
        rightOpenPip = snapshot.rightOpenPip
        upOpenPip = snapshot.upOpenPip
        downOpenPip = snapshot.downOpenPip
        spinnerTile = snapshot.spinnerTile
        // Despues de restaurar el spinner: el layout necesita saber si hay 4 ramas activas.
        recomputeBoardLayout()

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
        lastRoundPointsAwarded = snapshot.lastRoundPointsAwarded
        roundPointsByPlayer.clear()

        lastRoundBonusType = snapshot.lastRoundBonusType
        blockedPlayers.clear()
        blockedPlayers.addAll(snapshot.blockedPlayers)
        matchRoundsPlayed = snapshot.matchRoundsPlayed
        matchRoundsWon = snapshot.matchRoundsWon
        matchBlockedRounds = snapshot.matchBlockedRounds
        matchDominos = snapshot.matchDominos
        matchTrancasWon = snapshot.matchTrancasWon
        matchWinsWithSingleTile = snapshot.matchWinsWithSingleTile
        matchPointsScored = snapshot.matchPointsScored
        matchBestRoundPoints = snapshot.matchBestRoundPoints
        matchDrewFromBoneyard = snapshot.matchDrewFromBoneyard

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
        val lastRoundBonusType: DominoBonusType,
        val lastRoundPointsAwarded: Int,
        val roundPointsByPlayer: Map<Int, Int>,
        val playerPassedPips: Map<Int, Set<Int>>
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
        lastRoundBonusType = lastRoundBonusType,
        lastRoundPointsAwarded = lastRoundPointsAwarded,
        roundPointsByPlayer = roundPointsByPlayer.toMap(),
        playerPassedPips = playerPassedPips.mapValues { it.value.toSet() }
    )

    private fun restore(snapshot: StateSnapshot) {
        players.clear()
        players.addAll(snapshot.players)
        boardChain.clear()
        firstPlacedTile = snapshot.boardChain.firstOrNull { it.isFirstTile }
        leftBranchTiles.clear()
        leftBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.LEFT }.sortedBy { it.branchStepIndex })
        rightBranchTiles.clear()
        rightBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.RIGHT }.sortedBy { it.branchStepIndex })
        upBranchTiles.clear()
        upBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.UP }.sortedBy { it.branchStepIndex })
        downBranchTiles.clear()
        downBranchTiles.addAll(snapshot.boardChain.filter { it.branchSide == BoardSide.DOWN }.sortedBy { it.branchStepIndex })
        boneyard.clear()
        boneyard.addAll(snapshot.boneyard)
        leftOpenPip = snapshot.leftOpenPip
        rightOpenPip = snapshot.rightOpenPip
        upOpenPip = snapshot.upOpenPip
        downOpenPip = snapshot.downOpenPip
        spinnerTile = snapshot.spinnerTile
        // Despues de restaurar el spinner: el layout necesita saber si hay 4 ramas activas.
        recomputeBoardLayout()
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
        lastRoundPointsAwarded = snapshot.lastRoundPointsAwarded
        roundPointsByPlayer.clear()
        roundPointsByPlayer.putAll(snapshot.roundPointsByPlayer)
        playerPassedPips.clear()
        snapshot.playerPassedPips.forEach { (k, v) -> playerPassedPips[k] = v.toMutableSet() }
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
            currentPlayerIndex = playerIndex
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

    // --- Deshacer y pista (solo para asientos humanos) ---

    /**
     * Estado al que volveria un "deshacer". Se marca antes de cada jugada humana: deshacer
     * devuelve la mesa al momento anterior a esa jugada, incluida la respuesta de la IA, que es
     * lo que espera el jugador cuando se arrepiente.
     */
    private var undoPoint: StateSnapshot? = null

    val canUndo: Boolean get() = undoPoint != null

    fun markUndoPoint() {
        undoPoint = snapshot()
    }

    fun clearUndoPoint() {
        undoPoint = null
    }

    fun undo(): Boolean {
        val point = undoPoint ?: return false
        undoPoint = null
        restore(point)
        return true
    }

    /**
     * Jugada recomendada para un asiento humano (boton de pista). Usa la misma evaluacion que
     * la IA media, asi que respeta las reglas de cada modalidad sin duplicarlas.
     */
    fun suggestedMove(playerIndex: Int): AiMove? {
        if (isRoundOver || isMatchOver) return null
        val view = viewFor(playerIndex)
        if (view.playableMoves.isEmpty()) return null
        val personality = players.getOrNull(playerIndex)?.personality ?: Personality.NICE
        val advisor = MediumStrategy(
            weights = StrategyCatalog.weightsFor(personality, Difficulty.STANDARD)
        ) { move -> simulate(playerIndex, move) }
        return advisor.decide(view)
    }
}
