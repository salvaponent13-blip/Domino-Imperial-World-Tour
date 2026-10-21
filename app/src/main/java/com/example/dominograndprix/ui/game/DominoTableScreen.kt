package com.example.dominograndprix.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.audio.DominoHaptics
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.data.MatchSnapshot
import com.example.dominograndprix.data.MatchSnapshotStore
import com.example.dominograndprix.engine.DominoBoardLayout
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.DominoBonusType
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Player
import com.example.dominograndprix.ui.components.AnimatedPlacedTile
import com.example.dominograndprix.ui.components.DominoBackTileView
import com.example.dominograndprix.ui.components.DominoTileView
import com.example.dominograndprix.ui.components.tileHeight
import com.example.dominograndprix.ui.components.tileWidth
import com.example.dominograndprix.ui.dialogs.DominoPipCounterDialog
import com.example.dominograndprix.ui.dialogs.DominoRoundSummaryDialog
import kotlinx.coroutines.delay

val TableFeltGreen = Color(0xFF0F4727)
val TableWoodFrame = Color(0xFF4A2511)
val GoldAccent = Color(0xFFFFD700)
val DarkNavy = Color(0xFF131924)

enum class CameraViewMode(val label: String, val rotationX: Float, val scale: Float) {
    CLASSIC_3D("3D", 15f, 1f),
    DEEP_3D("3D+", 26f, 0.96f),
    TOP_2D("2D", 0f, 1f)
}

enum class AmbientLightMode(
    val label: String,
    val feltCoreColor: Color,
    val feltMidColor: Color,
    val feltEdgeColor: Color,
    val spotlightColor: Color
) {
    WARM_CLUB(
        label = "Club",
        feltCoreColor = Color(0xFF1E6E3E),
        feltMidColor = Color(0xFF0F4727),
        feltEdgeColor = Color(0xFF051A0D),
        spotlightColor = Color(0x38FFF4D6)
    ),
    TOURNAMENT_PRO(
        label = "Torneo",
        feltCoreColor = Color(0xFF1B7A42),
        feltMidColor = Color(0xFF0F5A2F),
        feltEdgeColor = Color(0xFF062915),
        spotlightColor = Color(0x30F0F9FF)
    ),
    LOUNGE_DRAMATIC(
        label = "Lounge",
        feltCoreColor = Color(0xFF14532D),
        feltMidColor = Color(0xFF09331B),
        feltEdgeColor = Color(0xFF020D07),
        spotlightColor = Color(0x50FFC478)
    )
}

@Composable
fun DominoTableScreen(
    gameMode: GameMode = GameMode.DRAW,
    cityName: String = "La Habana",
    venueId: String? = "cuba",
    betAmount: Int = 500,
    targetScore: Int = 100,
    playerCount: Int = 4,
    isTeamGame: Boolean = true,
    /** Nivel del rival de la sede. Decide con que estrategia juegan los asientos automaticos. */
    difficulty: Difficulty = Difficulty.STANDARD,
    /**
     * Asientos que juegan personas. Por defecto solo el 0, que es el modo carrera. En
     * pass-and-play local se pasa el conjunto de asientos de las personas (1v1: `setOf(0, 2)`).
     */
    humanPlayerIndices: Set<Int> = setOf(0),
    /** Nombres de los asientos. Vacio = los del modo carrera. */
    playerNames: List<String> = emptyList(),
    /**
     * Semilla fija del desafio diario, o `null` en partidas normales.
     *
     * Cuando viene, el reparto lo determina ella y no el reloj: es lo que hace que el reto de
     * un dia sea exactamente la misma partida para todo el mundo.
     */
    dailySeed: Long? = null,
    /** Partida guardada que hay que reanudar, si la hay. */
    resumeFrom: MatchSnapshot? = null,
    /**
     * Fin de partida. Recibe el motor ya cerrado para que quien llama pueda construir su
     * informe (estadisticas, logros) con datos reales en lugar de suposiciones.
     */
    onMatchFinished: (won: Boolean, pointsWon: Int, engine: DominoEngine) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    // Semilla de la partida: la del desafio diario si la hay, y si no una del reloj. Viaja con
    // el guardado, de modo que el reparto de la siguiente ronda es coherente al reanudar.
    val engine = remember(gameMode, targetScore, playerCount, resumeFrom) {
        mutableStateOf(
            if (resumeFrom != null) {
                DominoEngine.resume(resumeFrom)
            } else {
                DominoEngine.newMatch(
                    gameMode = gameMode,
                    targetScore = targetScore,
                    difficulty = difficulty,
                    seed = dailySeed ?: System.currentTimeMillis(),
                    playerCount = playerCount,
                    isTeamGame = isTeamGame
                ).apply {
                    setHumanPlayerIndices(humanPlayerIndices)
                    if (playerNames.isNotEmpty()) setPlayerNames(playerNames)
                }
            }
        )
    }
    var gameStateVersion by remember { mutableIntStateOf(0) }
    var selectedTile by remember { mutableStateOf<DominoTile?>(null) }
    var logMessage by remember { mutableStateOf(engine.value.roundLog) }
    var isAIThinking by remember { mutableStateOf(false) }
    // Si se reanuda una partida, la ronda ya estaba cerrada: el dialogo debe salir de entrada.
    var showRoundDialog by remember { mutableStateOf(resumeFrom != null) }

    // Bocadillos de reacción animados para rivales
    var activeReactionSeat by remember { mutableStateOf<Int?>(null) }
    var activeReactionText by remember { mutableStateOf<String?>(null) }

    // Anuncio flotante de puntos o jugadas destacadas (estilo Google AI Studio)
    var turnAnnouncement by remember { mutableStateOf<String?>(null) }
    var gameSpeed by remember { mutableIntStateOf(1) } // 1x o 2x (fidelidad a tour-domino-main)
    var showTileCounter by remember { mutableStateOf(false) }

    LaunchedEffect(turnAnnouncement) {
        if (turnAnnouncement != null) {
            delay(2200L)
            turnAnnouncement = null
        }
    }

    LaunchedEffect(activeReactionText, activeReactionSeat) {
        if (activeReactionText != null) {
            delay(2400L)
            activeReactionText = null
            activeReactionSeat = null
        }
    }

    // Vista para la respuesta haptica: usarla respeta los ajustes de vibracion del sistema.
    val hapticView = LocalView.current

    // --- Cortina de privacidad del pass-and-play ---
    // En partidas con varias personas, el traspaso del dispositivo oculta la mano del
    // siguiente jugador hasta que confirma que le ha llegado. Sin esto, quien pasa el movil
    // acaba viendo las fichas del otro.
    val isLocalMatch = engine.value.humanPlayerIndices.size > 1
    var previousActingSeat by remember { mutableIntStateOf(-1) }
    var isPrivacyCurtainUp by remember { mutableStateOf(false) }

    LaunchedEffect(engine.value.currentPlayerIndex, engine.value.isRoundOver, engine.value.isMatchOver) {
        val seat = engine.value.currentPlayerIndex
        val shouldRaise = PrivacyCurtainRules.shouldRaiseCurtain(
            humanSeatCount = engine.value.humanPlayerIndices.size,
            previousActingSeat = previousActingSeat,
            previousSeatWasHuman = previousActingSeat >= 0 &&
                previousActingSeat in engine.value.humanPlayerIndices,
            currentSeat = seat,
            currentSeatIsHuman = engine.value.isCurrentPlayerHuman,
            isRoundOver = engine.value.isRoundOver,
            isMatchOver = engine.value.isMatchOver
        )
        if (shouldRaise) {
            isPrivacyCurtainUp = true
            selectedTile = null
        }
        previousActingSeat = seat
    }

    // Cámara 2D en unidades virtuales para arrastre táctil manual
    var cameraDragVirtX by remember { mutableFloatStateOf(0f) }
    var cameraDragVirtY by remember { mutableFloatStateOf(0f) }

    // Modos 3D y de iluminación ambiental interactivos (Google AI Studio)
    var cameraMode by remember { mutableStateOf(CameraViewMode.TOP_2D) }
    var ambientLightMode by remember { mutableStateOf(AmbientLightMode.WARM_CLUB) }

    val animRotationX by animateFloatAsState(
        targetValue = cameraMode.rotationX,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cameraRotationX"
    )
    val animCameraScale by animateFloatAsState(
        targetValue = cameraMode.scale,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cameraScale"
    )

    // Show round dialog and play victory/defeat jingle when round finishes
    LaunchedEffect(engine.value.isRoundOver, engine.value.isMatchOver) {
        if (engine.value.isRoundOver || engine.value.isMatchOver) {
            if (engine.value.winningTeam == 0) {
                DominoSoundManager.playVictoryJingle()
            } else {
                DominoSoundManager.playDefeatJingle()
            }
            // Cerrar la ronda merece una respuesta mas rotunda que colocar una ficha.
            DominoHaptics.dominoCalled(hapticView)
            showRoundDialog = true
        }
    }

    // Guardado de la partida al cerrar cada ronda (enfoque 80/20 del plan).
    LaunchedEffect(engine.value.isRoundOver, engine.value.isMatchOver) {
        if (dailySeed != null) return@LaunchedEffect
        val current = engine.value
        if (current.isRoundOver && !current.isMatchOver) {
            MatchSnapshotStore.save(
                context,
                current.toSnapshot(cityName = cityName, venueId = venueId, betAmount = betAmount)
            )
        } else if (current.isMatchOver) {
            // La partida ha terminado: no debe quedar un guardado que la resucite.
            MatchSnapshotStore.clear(context)
        }
    }

    // Auto-reset manual pan when round or board resets
    LaunchedEffect(engine.value.boardChain.size, gameStateVersion) {
        if (engine.value.boardChain.isEmpty()) {
            cameraDragVirtX = 0f
            cameraDragVirtY = 0f
        }
    }

    // Auto-advance AI turns with physical domino sound effects and animated reactions
    LaunchedEffect(engine.value.currentPlayerIndex, engine.value.turnNumber, engine.value.isRoundOver, engine.value.isMatchOver) {
        val current = engine.value.players[engine.value.currentPlayerIndex]
        if (!current.isHuman && !engine.value.isRoundOver && !engine.value.isMatchOver) {
            try {
                isAIThinking = true
                val aiDelay = if (gameSpeed == 2) 300L else 750L
                delay(aiDelay)
                val prevBoardSize = engine.value.boardChain.size
                val prevScore = current.score
                val prevHandSize = current.hand.size
                val aiSeat = engine.value.currentPlayerIndex

                engine.value.executeAIMove()

                val newBoardSize = engine.value.boardChain.size
                val updatedAI = engine.value.players[aiSeat]
                val newScore = updatedAI.score
                val newHandSize = updatedAI.hand.size

                if (newBoardSize > prevBoardSize) {
                    val lastPlaced = engine.value.boardChain.lastOrNull()
                    DominoSoundManager.playTileClack(isDouble = lastPlaced?.isDouble == true)
                    if (newHandSize == 0) {
                        activeReactionSeat = aiSeat
                        activeReactionText = "¡Dominó! 🎉"
                        turnAnnouncement = "¡${updatedAI.name} cerró la ronda! (Dominó)"
                    } else if (newScore > prevScore) {
                        activeReactionSeat = aiSeat
                        activeReactionText = "+${newScore - prevScore} pts ⭐"
                        turnAnnouncement = "+${newScore - prevScore} pts (${updatedAI.name})"
                    }
                } else {
                    DominoSoundManager.playPassKnock()
                    activeReactionSeat = aiSeat
                    if (newHandSize > prevHandSize) {
                        activeReactionText = "Al pozo... 🦴"
                    } else {
                        activeReactionText = "¡Paso! ❌"
                        val passedPips = listOfNotNull(engine.value.leftOpenPip, engine.value.rightOpenPip).distinct().joinToString(" y ")
                        if (passedPips.isNotEmpty()) {
                            turnAnnouncement = "${updatedAI.name} pasó al $passedPips"
                        }
                    }
                }
                logMessage = engine.value.roundLog
            } finally {
                isAIThinking = false
            }
            gameStateVersion++
        }
    }

    // Limpiar selección de ficha cuando el turno cambia
    LaunchedEffect(engine.value.currentPlayerIndex) {
        selectedTile = null
    }

    val isMyTurn = engine.value.isCurrentPlayerHuman && !engine.value.isRoundOver && !isPrivacyCurtainUp
    // Húngaro: tras tirar una ficha válida el turno NO avanza y el jugador puede encadenar.
    val isChaining = isMyTurn && engine.value.canContinueHungarianTurn

    // --- Asiento que ACTUA y asiento que MIRA: son cosas distintas ---
    //
    // `actingSeat` es quien tiene el turno (puede ser una persona o la maquina).
    // `viewingSeat` es de quien se muestra la mano abajo. Nunca son lo mismo cuando juega la
    // IA: si se pintara la mano del asiento activo, se estarian ensenando las fichas secretas
    // del rival y el jugador veria su propia mano cambiando sola en cada turno.
    val actingSeat = engine.value.currentPlayerIndex
    var lastHumanSeat by remember { mutableIntStateOf(0) }
    LaunchedEffect(actingSeat, isLocalMatch) {
        if (isLocalMatch && engine.value.isCurrentPlayerHuman && !engine.value.isRoundOver) {
            lastHumanSeat = actingSeat
        }
    }
    // La decision vive en HandViewRules, con sus propios tests: confundir el asiento que actua
    // con el que mira fue el defecto que llego a produccion.
    val viewingSeat = HandViewRules.handSeat(
        isLocalMatch = isLocalMatch,
        currentSeat = actingSeat,
        currentSeatIsHuman = engine.value.isCurrentPlayerHuman,
        lastHumanSeat = lastHumanSeat,
        humanSeats = engine.value.humanPlayerIndices
    )
    val viewer = engine.value.players[viewingSeat]
    val viewerHuman = viewingSeat in engine.value.humanPlayerIndices
    val playableTiles = remember(
        engine.value.boardChain.size,
        viewer.hand.size,
        engine.value.currentPlayerIndex,
        engine.value.canContinueHungarianTurn,
        gameStateVersion
    ) {
        if (isMyTurn) engine.value.getPlayableTiles(viewingSeat) else emptyList()
    }

    val selectedTileSides = if (selectedTile != null) {
        // La ficha seleccionada es de quien MIRA la mano, no de quien tiene el turno.
        engine.value.getPlayableSides(viewingSeat, selectedTile!!)
    } else {
        emptySet()
    }
    val canPlaySelectedLeft = selectedTileSides.contains(BoardSide.LEFT)
    val canPlaySelectedRight = selectedTileSides.contains(BoardSide.RIGHT)

    // Draw Special: tu rama asignada y si el rival que la otra posee la tiene abierta.
    val isBranchGame = gameMode == GameMode.DRAW_SPECIAL
    val viewerBranch = engine.value.branchForPlayer(viewingSeat) ?: BoardSide.LEFT
    val otherBranch = if (viewerBranch == BoardSide.LEFT) BoardSide.RIGHT else BoardSide.LEFT
    val otherBranchLocked = isBranchGame && engine.value.isBranchRestrictedFor(viewingSeat, otherBranch)

    // Must player draw from face-down boneyard?
    val mustDrawFromBoneyard = isMyTurn && !isChaining && playableTiles.isEmpty() &&
            gameMode.allowsDraw && engine.value.boneyard.isNotEmpty() && !engine.value.hasDrawnThisTurn

    // Función unificada para ejecutar jugada humana con sonido, haptics y anuncio de puntos
    val playMove: (DominoTile, BoardSide) -> Unit = { tileToPlay, sideToPlay ->
        DominoSoundManager.playTileClack(isDouble = tileToPlay.isDouble)
        DominoHaptics.tilePlaced(hapticView)
        engine.value.playTile(actingSeat, tileToPlay, sideToPlay)
        if (engine.value.lastPointsEarned > 0) {
            turnAnnouncement = "+${engine.value.lastPointsEarned} ¡Puntos! (Tú)"
        }
        selectedTile = null
        logMessage = engine.value.roundLog
        gameStateVersion++
    }

    // Lista de fichas en tablero + ranuras de colocación (fórmula exacta de tour-domino-main)
    val boardChain = engine.value.boardChain
    val allTilesWithGhosts = remember(boardChain.size, selectedTile, selectedTileSides.size, gameStateVersion) {
        val list = boardChain.toMutableList()
        if (isMyTurn && selectedTile != null && selectedTileSides.size > 1) {
            selectedTileSides.forEach { side ->
                val slot = engine.value.getNextSlotForSide(side)
                list.add(
                    BoardPlacedTile(
                        tile = selectedTile!!,
                        virtX = slot.virtX,
                        virtY = slot.virtY,
                        isVertical = slot.isVertical,
                        pipLeft = 0,
                        pipRight = 0,
                        branchSide = side
                    )
                )
            }
        }
        list
    }

    // Límites exactos del tablero con margen de seguridad de 1 celda (14 unidades)
    val boardBounds = remember(allTilesWithGhosts) {
        DominoBoardLayout.computeBounds(allTilesWithGhosts)
    }

    // Auto-Pass when player has no playable tiles and cannot draw (or already drew this turn)
    LaunchedEffect(isMyTurn, isChaining, playableTiles.size, engine.value.boneyard.size, engine.value.hasDrawnThisTurn, gameStateVersion) {
        if (isMyTurn && !isChaining && playableTiles.isEmpty() &&
            (!gameMode.allowsDraw || engine.value.boneyard.isEmpty() || engine.value.hasDrawnThisTurn)
        ) {
            val passedPips = listOfNotNull(engine.value.leftOpenPip, engine.value.rightOpenPip).distinct().joinToString(" y ")
            if (passedPips.isNotEmpty()) {
                turnAnnouncement = "Pasaste al $passedPips"
            }
            logMessage = DominoStrings.autoPassMsg()
            delay(950)
            DominoSoundManager.playPassKnock()
            engine.value.passTurn(viewingSeat)
            logMessage = engine.value.roundLog
            gameStateVersion++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0F0A))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. COMPACT TOP HUD + SCOREBOARD + AUDIO TOGGLE
            val teamScore = engine.value.players.filter { it.team == 0 }.sumOf { it.score }
            val rivalScore = engine.value.players.filter { it.team == 1 }.sumOf { it.score }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkNavy.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { onBack() }
                            ) {
                                Text(
                                    text = "⬅",
                                    color = GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        cityName.uppercase(),
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GoldAccent.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldAccent)
                                    ) {
                                        Text(
                                            text = gameMode.title.uppercase(),
                                            color = GoldAccent,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Meta: $targetScore pts" + (if (betAmount > 0) " | \$$betAmount" else ""),
                                    color = Color.LightGray,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Marcador de Equipos (Tú vs Rival)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team / Player Score (Emerald)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF064E3B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (isTeamGame) "TÚ:" else "TÚ:",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$teamScore",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Rival Score (Ruby)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF7F1D1D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (isTeamGame) "RIV:" else "RIV:",
                                        color = Color(0xFFFECACA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$rivalScore",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Fila 2: Tira de Rivales + Controles (Velocidad, Contar, Cámara 3D, Iluminación, Mute)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rivals & Partner compact strip con mini-atriles y fallas
                        if (engine.value.players.size > 1) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (seat in 1 until engine.value.players.size) {
                                    val isPartner = isTeamGame && seat == 2
                                    val passed = engine.value.playerPassedPips[seat] ?: emptySet()
                                    val reaction = if (activeReactionSeat == seat) activeReactionText else null
                                    PlayerBadge(
                                        player = engine.value.players[seat],
                                        isCurrentTurn = engine.value.currentPlayerIndex == seat,
                                        isPartner = isPartner,
                                        passedPips = passed,
                                        reactionText = reaction,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Botones de Acción (Velocidad, Contar, Cámara 3D, Iluminación y Mute)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selector de Velocidad IA (1x / 2x)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (gameSpeed == 2) GoldAccent.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (gameSpeed == 2) GoldAccent else Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    DominoSoundManager.playTileTap()
                                    gameSpeed = if (gameSpeed == 1) 2 else 1
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("⏩", fontSize = 8.sp)
                                    Text(
                                        text = "${gameSpeed}x",
                                        color = if (gameSpeed == 2) GoldAccent else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Botón Contar Fichas
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (showTileCounter) Color(0xFF064E3B) else Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (showTileCounter) Color(0xFF34D399) else Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    DominoSoundManager.playTileTap()
                                    showTileCounter = !showTileCounter
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("#", color = Color(0xFFA7F3D0), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        text = "Contar",
                                        color = if (showTileCounter) Color(0xFFA7F3D0) else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 3D Camera Perspective Toggle (2D Cenital, 3D Clásico, 3D Profundo)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (cameraMode != CameraViewMode.TOP_2D) GoldAccent.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (cameraMode != CameraViewMode.TOP_2D) GoldAccent else Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    DominoSoundManager.playTileTap()
                                    cameraMode = when (cameraMode) {
                                        CameraViewMode.TOP_2D -> CameraViewMode.CLASSIC_3D
                                        CameraViewMode.CLASSIC_3D -> CameraViewMode.DEEP_3D
                                        CameraViewMode.DEEP_3D -> CameraViewMode.TOP_2D
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("🎥", fontSize = 8.sp)
                                    Text(
                                        text = cameraMode.label,
                                        color = if (cameraMode != CameraViewMode.TOP_2D) GoldAccent else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Ambient Lighting Toggle (Club Cálido, Torneo Pro, Lounge Dramático)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    DominoSoundManager.playTileTap()
                                    ambientLightMode = when (ambientLightMode) {
                                        AmbientLightMode.WARM_CLUB -> AmbientLightMode.TOURNAMENT_PRO
                                        AmbientLightMode.TOURNAMENT_PRO -> AmbientLightMode.LOUNGE_DRAMATIC
                                        AmbientLightMode.LOUNGE_DRAMATIC -> AmbientLightMode.WARM_CLUB
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("💡", fontSize = 8.sp)
                                    Text(
                                        text = ambientLightMode.label,
                                        color = GoldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Sound Mute Toggle
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                            ) {
                                Text(
                                    text = if (DominoSoundManager.isMuted) "🔇" else "🔊",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. CENTRAL DOMINO FEVER TABLE WITH 3D PERSPECTIVE & AMBIENT LIGHTING
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(3.dp, TableWoodFrame, RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientLightMode.feltCoreColor,
                                ambientLightMode.feltMidColor,
                                ambientLightMode.feltEdgeColor
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scale = 2.0f
                            cameraDragVirtX = (cameraDragVirtX + dragAmount.x / scale).coerceIn(-120f, 120f)
                            cameraDragVirtY = (cameraDragVirtY + dragAmount.y / scale).coerceIn(-120f, 120f)
                        }
                    }
            ) {
                // Dynamic Overhead Lamp Spotlight & Recessed Concave Pit Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ambientLightMode.spotlightColor,
                                    ambientLightMode.spotlightColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(maxWidth.value * 0.5f, maxHeight.value * 0.40f),
                                radius = (maxWidth.value.coerceAtLeast(maxHeight.value) * 0.70f)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.60f)
                                )
                            )
                        )
                )

                // Pinstripe dorado interior del tapete de torneo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .border(1.dp, GoldAccent.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                )

                // Remaches esquineros de latón dorado (Brass Corner Studs estilo casino)
                val brassStudGradient = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFF59E0B), Color(0xFF78350F))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 6.dp, y = 6.dp)
                        .clip(CircleShape)
                        .background(brassStudGradient)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.7f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .clip(CircleShape)
                        .background(brassStudGradient)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.7f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = 6.dp, y = (-6).dp)
                        .clip(CircleShape)
                        .background(brassStudGradient)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.7f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .clip(CircleShape)
                        .background(brassStudGradient)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.7f), CircleShape)
                )
                // Cuadrícula y escala exacta calculada directamente de tour-domino-main
                val availW = (maxWidth.value - 24f).coerceAtLeast(120f)
                val availH = (maxHeight.value - 24f).coerceAtLeast(120f)
                val rawCellW = availW / boardBounds.totalCols
                val rawCellH = availH / boardBounds.totalRows
                val targetCellDp = kotlin.math.min(rawCellW, rawCellH).coerceIn(6f, 23f).dp

                val animCellDp by animateDpAsState(
                    targetValue = targetCellDp,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    label = "animCellDp"
                )

                val gridW = animCellDp * boardBounds.totalCols
                val gridH = animCellDp * boardBounds.totalRows
                val gridOriginX = (maxWidth - gridW) / 2f
                val gridOriginY = (maxHeight - gridH) / 2f

                // 3D Tilted Board Surface (Superficie con perspectiva 3D realista)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationX = animRotationX
                            scaleX = animCameraScale
                            scaleY = animCameraScale
                            cameraDistance = 14f * density
                        }
                ) {
                    // Empty board message
                    if (engine.value.boardChain.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DominoStrings.emptyTableTitle(),
                                color = Color.White.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DominoStrings.emptyTableSub(isMyTurn),
                                color = GoldAccent,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Renderizar cada ficha usando la fórmula exacta de tour-domino-main
                        engine.value.boardChain.forEach { placed ->
                            key(placed.placementId) {
                                val isVisualVertical = placed.isVertical
                                val halfW = if (isVisualVertical) 14 else 28
                                val halfH = if (isVisualVertical) 28 else 14
                                val colSpan = if (isVisualVertical) 2f else 4f
                                val rowSpan = if (isVisualVertical) 4f else 2f
                                val colStart = (placed.virtX - halfW - boardBounds.minX) / 14f
                                val rowStart = (placed.virtY - halfH - boardBounds.minY) / 14f

                                val tileW = animCellDp * colSpan
                                val tileH = animCellDp * rowSpan
                                val xDp = gridOriginX + (animCellDp * colStart) + cameraDragVirtX.dp
                                val yDp = gridOriginY + (animCellDp * rowStart) + cameraDragVirtY.dp

                                AnimatedPlacedTile(
                                    tile = placed.tile,
                                    pip1 = placed.pipLeft,
                                    pip2 = placed.pipRight,
                                    isHorizontal = !isVisualVertical,
                                    width = tileW,
                                    height = tileH,
                                    placementId = placed.placementId,
                                    modifier = Modifier.offset(x = xDp, y = yDp)
                                )
                            }
                        }

                        // Si el jugador seleccionó una ficha jugable por múltiples extremos, mostrar ranuras brillantes
                        if (isMyTurn && selectedTile != null && selectedTileSides.size > 1) {
                            selectedTileSides.forEach { side ->
                                val slot = engine.value.getNextSlotForSide(side)
                                val isVisualVert = slot.isVertical
                                val halfW = if (isVisualVert) 14 else 28
                                val halfH = if (isVisualVert) 28 else 14
                                val colSpan = if (isVisualVert) 2f else 4f
                                val rowSpan = if (isVisualVert) 4f else 2f
                                val colStart = (slot.virtX - halfW - boardBounds.minX) / 14f
                                val rowStart = (slot.virtY - halfH - boardBounds.minY) / 14f

                                val slotW = animCellDp * colSpan
                                val slotH = animCellDp * rowSpan
                                val slotXDp = gridOriginX + (animCellDp * colStart) + cameraDragVirtX.dp
                                val slotYDp = gridOriginY + (animCellDp * rowStart) + cameraDragVirtY.dp

                                val targetPip = when (side) {
                                    BoardSide.LEFT -> engine.value.leftOpenPip
                                    BoardSide.RIGHT -> engine.value.rightOpenPip
                                    BoardSide.UP -> engine.value.upOpenPip ?: engine.value.spinnerTile?.high
                                    BoardSide.DOWN -> engine.value.downOpenPip ?: engine.value.spinnerTile?.high
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = slotXDp, y = slotYDp)
                                        .size(width = slotW, height = slotH)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldAccent.copy(alpha = 0.55f))
                                        .border(2.dp, GoldAccent, RoundedCornerShape(4.dp))
                                        .clickable {
                                            playMove(selectedTile!!, side)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${targetPip ?: ""}", color = DarkNavy, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Top-left Boneyard counter badge & Top-right Open Ends badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (gameMode.allowsDraw) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = DominoStrings.boneyardCount(engine.value.boneyard.size),
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (engine.value.boardChain.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            val evalText = engine.value.getOpenEndsEvaluationText()
                            Text(
                                text = if (evalText.isNotEmpty()) evalText else DominoStrings.openEndsBadge(
                                    engine.value.leftOpenPip,
                                    engine.value.rightOpenPip,
                                    engine.value.upOpenPip,
                                    engine.value.downOpenPip
                                ),
                                color = if (evalText.contains("⭐")) GoldAccent else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Draw Special: recordatorio de la rama asignada y de si el rival la tiene abierta
                if (isBranchGame) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkNavy.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (otherBranchLocked) Color(0xFFFF8A80) else GoldAccent
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 34.dp)
                    ) {
                        Text(
                            text = if (otherBranchLocked) {
                                DominoStrings.branchRestricted(otherBranch == BoardSide.LEFT)
                            } else {
                                DominoStrings.branchAssignment(viewerBranch == BoardSide.LEFT)
                            },
                            color = if (otherBranchLocked) Color(0xFFFFCDD2) else GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bocadillo animado de reacción de rival en la mesa
                if (activeReactionText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavy.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isBranchGame) 62.dp else 34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val seat = activeReactionSeat
                            val pName = if (seat != null && seat in engine.value.players.indices) engine.value.players[seat].name else "Rival"
                            Text(
                                text = "$pName:",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeReactionText ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // LIVE TURN ANNOUNCEMENT POPUP (Pill animado de puntos o avisos de turno)
                if (turnAnnouncement != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldAccent,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = turnAnnouncement ?: "",
                            color = DarkNavy,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // FACE-DOWN BONEYARD SELECTION OVERLAY (When player has no valid moves and must pick a face-down tile by luck!)
                if (mustDrawFromBoneyard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkNavy,
                            border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = DominoStrings.noPlayableTileTitle(),
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DominoStrings.pickFromBoneyardSub(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Face-down tiles grid to pick by luck
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    engine.value.boneyard.forEachIndexed { idx, _ ->
                                        DominoBackTileView(
                                            width = 34.dp,
                                            height = 62.dp,
                                            highlightBorder = true,
                                            onClick = {
                                                DominoSoundManager.playDrawTile()
                                                val drawn = engine.value.drawTile(actingSeat, idx)
                                                if (drawn != null) {
                                                    logMessage = DominoStrings.drewTileMsg(drawn.toString())
                                                    gameStateVersion++
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom status pill inside table
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isAIThinking) DominoStrings.rivalPlayingMsg() else logMessage,
                        color = if (isAIThinking) GoldAccent else Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            // 3. BOTTOM PLAYER HAND BAR (Authentic 240x320 bottom rack, 1-touch play)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavy.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isMyTurn) Color.Green else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isChaining) {
                                    DominoStrings.hungarianChainPrompt()
                                } else if (isMyTurn) {
                                    if (selectedTile != null) DominoStrings.chooseEndPrompt(engine.value.leftOpenPip, engine.value.rightOpenPip)
                                    else DominoStrings.yourTurnPrompt()
                                } else DominoStrings.turnOfPlayer(engine.value.players[engine.value.currentPlayerIndex].name),
                                color = if (isMyTurn) GoldAccent else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        val teamScore = engine.value.players.filter { it.team == 0 }.sumOf { it.score }
                        val rivalScore = engine.value.players.filter { it.team == 1 }.sumOf { it.score }
                        ScoreBoard(teamScore = teamScore, rivalScore = rivalScore)
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Player's Dominoes in a compact, clean row (all 7 fit on screen without scrolling!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewer.hand.forEach { tile ->
                            val isPlayable = playableTiles.contains(tile)
                            val isSelected = selectedTile == tile

                            DominoTileView(
                                tile = tile,
                                isSelected = isSelected,
                                isPlayable = isMyTurn && isPlayable,
                                // El apagado solo tiene sentido para fichas de una persona que
                                // puede actuar. En una mano de la maquina no se apaga nada.
                                dimUnplayable = isMyTurn && viewerHuman,
                                isHorizontal = false,
                                width = 38.dp,
                                height = 70.dp,
                                onClick = {
                                    if (!isMyTurn || !isPlayable) return@DominoTileView
                                    // Consultar los lados legales en el momento del clic: esto
                                    // respeta tanto Húngaro (encadenar) como las ramas de Draw Special.
                                    val sides = engine.value.getPlayableSides(viewingSeat, tile)
                                    if (sides.isEmpty()) return@DominoTileView

                                    if (sides.size == 1) {
                                        playMove(tile, sides.first())
                                    } else if (sides == setOf(BoardSide.LEFT, BoardSide.RIGHT) && engine.value.leftOpenPip == engine.value.rightOpenPip) {
                                        // Ambos extremos valen lo mismo: no hay elección real
                                        playMove(tile, BoardSide.LEFT)
                                    } else {
                                        DominoSoundManager.playTileTap()
                                        selectedTile = if (isSelected) null else tile
                                    }
                                }
                            )
                        }
                    }

                    // Botones de elección cuando la ficha seleccionada puede jugarse en varios extremos
                    if (isMyTurn && selectedTile != null && selectedTileSides.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "¿En qué extremo deseas colocar [${selectedTile!!.high}|${selectedTile!!.low}]?",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    selectedTileSides.forEach { side ->
                                        val pip = when (side) {
                                            BoardSide.LEFT -> engine.value.leftOpenPip
                                            BoardSide.RIGHT -> engine.value.rightOpenPip
                                            BoardSide.UP -> engine.value.upOpenPip ?: engine.value.spinnerTile?.high
                                            BoardSide.DOWN -> engine.value.downOpenPip ?: engine.value.spinnerTile?.high
                                        }
                                        val label = when (side) {
                                            BoardSide.LEFT -> "⬅ Izquierda ($pip)"
                                            BoardSide.RIGHT -> "Derecha ($pip) ➡"
                                            BoardSide.UP -> "⬆ Arriba ($pip)"
                                            BoardSide.DOWN -> "⬇ Abajo ($pip)"
                                        }
                                        Button(
                                            onClick = { playMove(selectedTile!!, side) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldAccent,
                                                contentColor = DarkNavy
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CORTINA DE PRIVACIDAD (pass-and-play)
        // Tapa TODO, incluida la mano, hasta que el nuevo jugador confirma que le ha llegado
        // el dispositivo. Es lo que impide que el jugador anterior vea sus fichas al pasarlo.
        if (isPrivacyCurtainUp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkNavy)
                    .pointerInput(Unit) { /* se traga cualquier toque: nada de espiar por debajo */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DominoStrings.passDeviceTitle(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = DominoStrings.passDeviceTo(engine.value.players[engine.value.currentPlayerIndex].name),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = DominoStrings.passDeviceHint(),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Button(
                        onClick = {
                            DominoSoundManager.playTileTap()
                            DominoHaptics.softTap(hapticView)
                            isPrivacyCurtainUp = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = DominoStrings.readyBtn(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 5. DETAILED ROUND FINISHED / MATCH OVER DIALOG (fiel a tour-domino-main)
        if (showRoundDialog) {
            val didPlayerWin = if (engine.value.isTeamGame) {
                engine.value.winningTeam == 0
            } else {
                engine.value.winnerPlayerId == 0
            }
            DominoRoundSummaryDialog(
                engine = engine.value,
                targetScore = targetScore,
                humanWon = didPlayerWin,
                onNextRound = {
                    showRoundDialog = false
                    if (engine.value.isMatchOver) {
                        onMatchFinished(didPlayerWin, betAmount * 2, engine.value)
                    } else {
                        DominoSoundManager.playVenueMusic(venueId)
                        engine.value.startNewRound(
                            openerOverride = engine.value.nextRoundOpener()
                        )
                        selectedTile = null
                        logMessage = engine.value.roundLog
                        gameStateVersion++
                    }
                },
                onExitMatch = {
                    showRoundDialog = false
                    onBack()
                }
            )
        }

        // 6. PIP COUNTER MODAL (fiel a tour-domino-main)
        if (showTileCounter) {
            DominoPipCounterDialog(
                engine = engine.value,
                humanSeat = 0,
                onDismiss = { showTileCounter = false }
            )
        }
    }
}

/**
 * Marcador de la partida, con un destello cuando alguien anota.
 *
 * Un cambio de digito pasa desapercibido en una mesa con fichas moviendose; el destello y el
 * latido hacen que el jugador se entere de que acaba de puntuar alguien, y de quien.
 */
@Composable
fun ScoreBoard(teamScore: Int, rivalScore: Int) {
    // Se guarda el valor anterior para saber si la puntuacion ha subido de verdad y quien lo
    // hizo: en la primera composicion no debe haber destello.
    var previousTeam by remember { mutableIntStateOf(teamScore) }
    var previousRival by remember { mutableIntStateOf(rivalScore) }

    val teamFlash = remember { Animatable(0f) }
    val rivalFlash = remember { Animatable(0f) }

    LaunchedEffect(teamScore) {
        if (teamScore > previousTeam) {
            teamFlash.animateTo(1f, tween(120))
            teamFlash.animateTo(0f, tween(420))
        }
        previousTeam = teamScore
    }
    LaunchedEffect(rivalScore) {
        if (rivalScore > previousRival) {
            rivalFlash.animateTo(1f, tween(120))
            rivalFlash.animateTo(0f, tween(420))
        }
        previousRival = rivalScore
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ScoreValue(teamScore, teamFlash.value)
        Text(
            text = " | ",
            color = GoldAccent.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        ScoreValue(rivalScore, rivalFlash.value)
    }
}

/** Un lado del marcador: escala y color reaccionan al destello. */
@Composable
private fun ScoreValue(score: Int, flash: Float) {
    val scale = 1f + 0.35f * flash
    val color = lerp(GoldAccent, Color.White, flash)
    Text(
        text = "$score",
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.scale(scale)
    )
}

@Composable
fun PlayerBadge(
    player: Player,
    isCurrentTurn: Boolean,
    isPartner: Boolean = false,
    passedPips: Set<Int> = emptySet(),
    reactionText: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrentTurn) {
            GoldAccent.copy(alpha = 0.28f)
        } else if (isPartner) {
            Color(0xFF064E3B).copy(alpha = 0.75f)
        } else {
            Color.Black.copy(alpha = 0.50f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentTurn) GoldAccent else if (isPartner) Color(0xFF34D399).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.18f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isPartner) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF34D399).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF34D399))
                ) {
                    Text(
                        text = "PAR",
                        color = Color(0xFFA7F3D0),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                    )
                }
            }

            Text(
                text = player.name,
                color = if (isCurrentTurn) GoldAccent else Color.White,
                fontSize = 9.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = "(${player.hand.size})",
                color = GoldAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )

            // Mini-atril 3D de fichas de pie (representación visual de fichas ocultas de espaldas)
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val count = minOf(7, player.hand.size)
                for (i in 0 until count) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.5.dp, height = 8.dp)
                            .clip(RoundedCornerShape(0.8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFBF8E6), Color(0xFFC7BBA5))
                                )
                            )
                            .border(0.3.dp, Color.Black.copy(alpha = 0.45f), RoundedCornerShape(0.8.dp))
                    )
                }
            }

            // Indicador de Fallas ("Pasó al X") en chips rojos
            if (passedPips.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    modifier = Modifier.padding(start = 1.dp)
                ) {
                    Text(
                        text = "Pasó:",
                        color = Color(0xFFFECACA),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    passedPips.sorted().forEach { pip ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFF7F1D1D),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF87171))
                        ) {
                            Text(
                                text = "$pip",
                                color = Color.White,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.2.dp)
                            )
                        }
                    }
                }
            }

            if (reactionText != null) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = GoldAccent
                ) {
                    Text(
                        text = reactionText,
                        color = DarkNavy,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
