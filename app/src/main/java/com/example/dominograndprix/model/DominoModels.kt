package com.example.dominograndprix.model

import kotlinx.serialization.Serializable

/**
 * Representation of an individual domino tile (bone).
 */
@Serializable
data class DominoTile(
    val high: Int,
    val low: Int
) {
    val isDouble: Boolean get() = high == low
    val totalPips: Int get() = high + low

    fun hasPip(pip: Int): Boolean = high == pip || low == pip

    fun otherEnd(pip: Int): Int {
        return when (pip) {
            high -> low
            low -> high
            else -> throw IllegalArgumentException("Tile $this does not have pip $pip")
        }
    }

    override fun toString(): String = "[$high|$low]"
}

enum class BoardSide {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

/**
 * Nivel del rival de una sede. Determina la estrategia con la que juega la IA y, por tanto,
 * la dificultad real de la partida: el resto de la sede (modo, objetivo, apuesta) no cambia
 * lo dificil que es ganar.
 */
enum class Difficulty {
    /**
     * Comportamiento historico del juego, sin heuristica. Es el valor por defecto del motor
     * para no alterar el comportamiento de los tests y benchmarks ya existentes, que miden
     * contra esta referencia. El juego real nunca lo usa: cada sede asigna un nivel.
     */
    LEGACY,

    /** Rival de iniciacion: juega sin criterio, a eleccion aleatoria entre sus jugadas. */
    RELAXED,

    /** Rival competente: heuristica de pips y tranca. */
    STANDARD,

    /** Rival duro: heuristica afinada al maximo y castiga los errores. */
    TOUGH
}

/**
 * Una ficha ya colocada en la cadena del tablero, con su posición y orientación en el
 * espacio virtual de 240x320 que usa el renderizado de la mesa.
 */
@Serializable
data class BoardPlacedTile(
    val tile: DominoTile,
    val pipLeft: Int, // Top half (if vertical) or Left half (if horizontal)
    val pipRight: Int, // Bottom half (if vertical) or Right half (if horizontal)
    val isDouble: Boolean = tile.isDouble,
    val isFirstTile: Boolean = false,
    val virtX: Int = 105,
    val virtY: Int = 119,
    val isVertical: Boolean = false,
    val branchSide: BoardSide? = null,
    val branchStepIndex: Int = 0,
    /**
     * Identificador unico de esta colocacion, asignado por el motor al poner la ficha.
     *
     * Sirve para que la interfaz reconozca que una ficha es nueva y anime solo esa. El indice
     * en la lista no vale: al jugar por la izquierda la cadena se reordena y los indices de
     * las fichas ya colocadas cambian, lo que reiniciaria sus animaciones.
     */
    val placementId: Int = 0,
    val connectedPip: Int? = null,
    val openEndPip: Int? = null
)

/**
 * Las 8 modalidades de juego incluidas en el juego.
 */
enum class GameMode(
    val title: String,
    val summary: String,
    val isPointGame: Boolean = false,
    val allowsDraw: Boolean = true
) {
    DRAW(
        title = "Draw Domino",
        summary = "Dominó tradicional con robo del pozo. Quien se queda sin fichas primero gana."
    ),
    BLOCK(
        title = "Block Domino",
        summary = "Bloqueo puro sin pozo. Si no tienes ficha debes pasar el turno.",
        allowsDraw = false
    ),
    ALL_FIVES(
        title = "All Fives",
        summary = "Suma puntos inmediatamente si los extremos abiertos suman un múltiplo de 5 (los dobles abiertos cuentan doble).",
        isPointGame = true
    ),
    ALL_THREES(
        title = "All Threes",
        summary = "Suma puntos inmediatamente si la suma de extremos abiertos es múltiplo de 3.",
        isPointGame = true
    ),
    FIVES_AND_THREES(
        title = "Fives & Threes",
        summary = "Suma puntos para múltiplos de 5 y 3. ¡Puntuación doble si es múltiplo de ambos (15)!",
        isPointGame = true
    ),
    MATADOR(
        title = "Matador",
        summary = "Debes conectar fichas que sumen 7 con el extremo, o usar los 4 comodines Matadores.",
        allowsDraw = false
    ),
    DRAW_SPECIAL(
        title = "Draw Special",
        summary = "Cada jugador juega en su extremo asignado o en el del jugador bloqueado."
    ),
    BLOCK_HUNGARIAN(
        title = "Block Hungarian",
        summary = "¡Puedes encadenar y jugar múltiples fichas seguidas en un mismo turno!",
        allowsDraw = false
    )
}

/**
 * Player definition with personality and team.
 */
data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val personality: Personality = Personality.NICE,
    val hand: List<DominoTile> = emptyList(),
    val score: Int = 0,
    val team: Int = 0 // 0 for player/partner, 1 for rivals
)

enum class Personality {
    NICE,
    CONFIDENT,
    AGGRESSIVE
}

/**
 * Tournament venue definition (7 World Tour cities).
 */
data class CityVenue(
    val id: String,
    val name: String,
    val region: String,
    val targetScore: Int,
    val defaultMode: GameMode,
    /** Nivel del rival de esta sede. Escala a lo largo de la gira. */
    val difficulty: Difficulty = Difficulty.STANDARD,
    val trophyTitle: String,
    val rivalName: String,
    val rivalIntro: String,
    val rivalDefeatLine: String,
    val postcardResId: Int = 0,
    val musicTitle: String = "",
    val cityLore: String = "",
    val isUnlocked: Boolean = true,
    val isCompleted: Boolean = false
)

/**
 * Trofeo/recuerdo que se gana al completar una sede.
 *
 * No se vende ni se convierte en dinero: el objetivo del juego es completar la gira y el
 * trofeo es la constancia de haberlo hecho.
 */
data class LuxuryItem(
    val id: String,
    val title: String,
    val cityName: String
)

/**
 * Bonificación clásica de cierre por dominó.
 */
enum class DominoBonusType {
    NONE,
    DOUBLE,
    CAPICUA,
    DOUBLE_CAPICUA
}
