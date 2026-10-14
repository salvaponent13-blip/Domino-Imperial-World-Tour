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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Player
import com.example.dominograndprix.ui.components.AnimatedPlacedTile
import com.example.dominograndprix.ui.components.DominoBackTileView
import com.example.dominograndprix.ui.components.DominoTileView
import com.example.dominograndprix.ui.components.tileHeight
import com.example.dominograndprix.ui.components.tileWidth
import kotlinx.coroutines.delay

val TableFeltGreen = Color(0xFF0F4727)
val TableWoodFrame = Color(0xFF4A2511)
val GoldAccent = Color(0xFFFFD700)
val DarkNavy = Color(0xFF131924)

@Composable
fun DominoTableScreen(
    gameMode: GameMode = GameMode.DRAW,
    cityName: String = "La Habana",
    venueId: String? = "cuba",
    betAmount: Int = 500,
    targetScore: Int = 100,
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
    onMatchFinished: (won: Boolean, pointsWon: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    // Semilla de la partida: la del desafio diario si la hay, y si no una del reloj. Viaja con
    // el guardado, de modo que el reparto de la siguiente ronda es coherente al reanudar.
    val engine = remember {
        mutableStateOf(
            if (resumeFrom != null) {
                DominoEngine.resume(resumeFrom)
            } else {
                DominoEngine.newMatch(
                    gameMode = gameMode,
                    targetScore = targetScore,
                    difficulty = difficulty,
                    seed = dailySeed ?: System.currentTimeMillis()
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

    // Camara vertical en unidades virtuales. Se separa el arrastre manual del encuadre
    // automatico: el arrastre debe responder al dedo al instante, mientras que el reencuadre
    // al crecer la cadena se anima para que no de un salto.
    var cameraDragVirtY by remember { mutableFloatStateOf(0f) }
    var cameraAutoTargetVirtY by remember { mutableFloatStateOf(0f) }
    val cameraAutoVirtY by animateFloatAsState(
        targetValue = cameraAutoTargetVirtY,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "cameraAuto"
    )
    val verticalCameraVirtY = cameraAutoVirtY + cameraDragVirtY

    // Show round dialog and play victory/defeat jingle when round finishes
    LaunchedEffect(engine.value.isRoundOver, gameStateVersion) {
        if (engine.value.isRoundOver) {
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
    //
    // El desafio diario NO se guarda a proposito: si se pudiera reanudar, se podria reintentar
    // el mismo reto tantas veces como se quisiera y dejaria de ser un reto. Al salir de el se
    // pierde el intento, que es la regla del modo.
    LaunchedEffect(engine.value.isRoundOver, engine.value.isMatchOver, gameStateVersion) {
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

    // Auto-adjust vertical camera (like b.e(int, int) in b.java lines 4316-4333)
    LaunchedEffect(engine.value.boardChain.size, gameStateVersion) {
        if (engine.value.boardChain.isEmpty()) {
            cameraDragVirtY = 0f
            cameraAutoTargetVirtY = 0f
        } else {
            val minY = engine.value.boardChain.minOf { it.virtY }
            val maxY = engine.value.boardChain.maxOf { it.virtY + if (it.isVertical) 31 else 17 }
            val centerChainY = (minY + maxY) / 2f
            cameraAutoTargetVirtY = (119f - centerChainY).coerceIn(-90f, 90f)
        }
    }

    // Auto-advance AI turns with physical domino sound effects
    LaunchedEffect(engine.value.currentPlayerIndex, engine.value.isRoundOver, gameStateVersion) {
        val current = engine.value.players[engine.value.currentPlayerIndex]
        if (!current.isHuman && !engine.value.isRoundOver) {
            isAIThinking = true
            delay(650)
            val prevBoardSize = engine.value.boardChain.size
            engine.value.executeAIMove()
            if (engine.value.boardChain.size > prevBoardSize) {
                val lastPlaced = engine.value.boardChain.lastOrNull()
                DominoSoundManager.playTileClack(isDouble = lastPlaced?.isDouble == true)
            } else {
                DominoSoundManager.playPassKnock()
            }
            logMessage = engine.value.roundLog
            isAIThinking = false
            gameStateVersion++
        }
    }

    val isMyTurn = engine.value.isCurrentPlayerHuman && !engine.value.isRoundOver && !isPrivacyCurtainUp
    // Húngaro: tras tirar una ficha válida el turno NO avanza y el jugador puede encadenar.
    val isChaining = isMyTurn && engine.value.canContinueHungarianTurn
    val actingSeat = engine.value.currentPlayerIndex
    val human = engine.value.players[actingSeat]
    val playableTiles = remember(
        engine.value.boardChain.size,
        human.hand.size,
        engine.value.currentPlayerIndex,
        engine.value.canContinueHungarianTurn,
        gameStateVersion
    ) {
        if (isMyTurn) engine.value.getPlayableTiles(actingSeat) else emptyList()
    }

    val selectedTileSides = if (selectedTile != null) {
        engine.value.getPlayableSides(actingSeat, selectedTile!!)
    } else {
        emptySet()
    }
    val canPlaySelectedLeft = selectedTileSides.contains(BoardSide.LEFT)
    val canPlaySelectedRight = selectedTileSides.contains(BoardSide.RIGHT)

    // Draw Special: tu rama asignada y si el rival que la otra posee la tiene abierta.
    val isBranchGame = gameMode == GameMode.DRAW_SPECIAL
    val humanBranch = engine.value.branchForPlayer(actingSeat) ?: BoardSide.LEFT
    val otherBranch = if (humanBranch == BoardSide.LEFT) BoardSide.RIGHT else BoardSide.LEFT
    val otherBranchLocked = isBranchGame && engine.value.isBranchRestrictedFor(actingSeat, otherBranch)

    // Must player draw from face-down boneyard?
    val mustDrawFromBoneyard = isMyTurn && !isChaining && playableTiles.isEmpty() &&
            gameMode.allowsDraw && engine.value.boneyard.isNotEmpty()

    // Auto-Pass when player has no playable tiles and cannot draw
    LaunchedEffect(isMyTurn, isChaining, playableTiles.size, engine.value.boneyard.size, gameStateVersion) {
        if (isMyTurn && !isChaining && playableTiles.isEmpty() &&
            (!gameMode.allowsDraw || engine.value.boneyard.isEmpty())
        ) {
            logMessage = DominoStrings.autoPassMsg()
            delay(950)
            DominoSoundManager.playPassKnock()
            engine.value.passTurn(actingSeat)
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
            // 1. COMPACT TOP HUD + TOP PARTNER + AUDIO TOGGLE
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkNavy.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
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
                            Text(cityName.uppercase(), color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(DominoStrings.ruleAndTarget(gameMode.title, targetScore), color = Color.LightGray, fontSize = 9.sp)
                        }
                    }

                    // Partner & Rivals compact strip + Audio Mute Toggle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerBadge(engine.value.players[1], engine.value.currentPlayerIndex == 1)
                        PlayerBadge(engine.value.players[2], engine.value.currentPlayerIndex == 2)
                        PlayerBadge(engine.value.players[3], engine.value.currentPlayerIndex == 3)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                        ) {
                            Text(
                                text = if (DominoSoundManager.isMuted) "🔇" else "🔊",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 2. CENTRAL 2D DOMINO FEVER TABLE (Exact 240x320 virtual layout template!)
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
                                Color(0xFF1E6E3E),
                                TableFeltGreen,
                                Color(0xFF082614)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            cameraDragVirtY = (cameraDragVirtY + dragAmount.y / 4f).coerceIn(-120f, 120f)
                        }
                    }
            ) {
                // Virtual coordinate scaling:
                // La cadena ocupa virtX en [45 .. 196] (ancho 151, centro 120.5)
                // y virtY alrededor de 119 (span aproximado [15 .. 235], alto 220).
                val unitDp = (maxWidth / 168f).coerceAtMost(maxHeight / 228f)
                val originVirtX = 36f
                val originVirtY = 12f

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
                    // Renderizar cada ficha colocada en su coordenada virtual (virtX, virtY).
                    // La clave es placementId: da identidad estable a cada ficha aunque la
                    // cadena se reordene al jugar por la izquierda, de modo que la animacion
                    // de asentamiento corre solo para la ficha recien puesta.
                    engine.value.boardChain.forEach { placed ->
                        key(placed.placementId) {
                            val tileW = tileWidth(unitDp, placed.isVertical)
                            val tileH = tileHeight(unitDp, placed.isVertical)
                            val xDp = unitDp * (placed.virtX - originVirtX)
                            val yDp = unitDp * (placed.virtY - originVirtY + verticalCameraVirtY)

                            AnimatedPlacedTile(
                                tile = placed.tile,
                                pip1 = placed.pipLeft,
                                pip2 = placed.pipRight,
                                isHorizontal = !placed.isVertical,
                                width = tileW,
                                height = tileH,
                                placementId = placed.placementId,
                                modifier = Modifier.offset(x = xDp, y = yDp)
                            )
                        }
                    }

                    // If player selected a tile that matches BOTH ends, show glowing target slots right on the chain ends!
                    if (isMyTurn && selectedTile != null && canPlaySelectedLeft && canPlaySelectedRight) {
                        val leftSlot = engine.value.getNextSlotForSide(BoardSide.LEFT)
                        val leftW = unitDp * (if (leftSlot.isVertical) 17f else 31f)
                        val leftH = unitDp * (if (leftSlot.isVertical) 31f else 17f)
                        val leftXDp = unitDp * (leftSlot.virtX - originVirtX)
                        val leftYDp = unitDp * (leftSlot.virtY - originVirtY + verticalCameraVirtY)

                        Box(
                            modifier = Modifier
                                .offset(x = leftXDp, y = leftYDp)
                                .size(width = leftW, height = leftH)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldAccent.copy(alpha = 0.45f))
                                .border(2.dp, GoldAccent, RoundedCornerShape(4.dp))
                                .clickable {
                                    val t = selectedTile!!
                                    DominoSoundManager.playTileClack(isDouble = t.isDouble)
                                    DominoHaptics.tilePlaced(hapticView)
                                    engine.value.playTileLeft(actingSeat, t)
                                    selectedTile = null
                                    logMessage = engine.value.roundLog
                                    gameStateVersion++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${engine.value.leftOpenPip}", color = DarkNavy, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        val rightSlot = engine.value.getNextSlotForSide(BoardSide.RIGHT)
                        val rightW = unitDp * (if (rightSlot.isVertical) 17f else 31f)
                        val rightH = unitDp * (if (rightSlot.isVertical) 31f else 17f)
                        val rightXDp = unitDp * (rightSlot.virtX - originVirtX)
                        val rightYDp = unitDp * (rightSlot.virtY - originVirtY + verticalCameraVirtY)

                        Box(
                            modifier = Modifier
                                .offset(x = rightXDp, y = rightYDp)
                                .size(width = rightW, height = rightH)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldAccent.copy(alpha = 0.45f))
                                .border(2.dp, GoldAccent, RoundedCornerShape(4.dp))
                                .clickable {
                                    val t = selectedTile!!
                                    DominoSoundManager.playTileClack(isDouble = t.isDouble)
                                    DominoHaptics.tilePlaced(hapticView)
                                    engine.value.playTileRight(actingSeat, t)
                                    selectedTile = null
                                    logMessage = engine.value.roundLog
                                    gameStateVersion++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${engine.value.rightOpenPip}", color = DarkNavy, fontWeight = FontWeight.Black, fontSize = 11.sp)
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
                            Text(
                                text = DominoStrings.openEndsBadge(engine.value.leftOpenPip, engine.value.rightOpenPip),
                                color = Color.White,
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
                                DominoStrings.branchAssignment(humanBranch == BoardSide.LEFT)
                            },
                            color = if (otherBranchLocked) Color(0xFFFFCDD2) else GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        human.hand.forEach { tile ->
                            val isPlayable = playableTiles.contains(tile)
                            val isSelected = selectedTile == tile

                            DominoTileView(
                                tile = tile,
                                isSelected = isSelected,
                                isPlayable = isMyTurn && isPlayable,
                                dimUnplayable = isMyTurn,
                                isHorizontal = false,
                                width = 38.dp,
                                height = 70.dp,
                                onClick = {
                                    if (!isMyTurn || !isPlayable) return@DominoTileView
                                    // Consultar los lados legales en el momento del clic: esto
                                    // respeta tanto Húngaro (encadenar) como las ramas de Draw Special.
                                    val sides = engine.value.getPlayableSides(actingSeat, tile)
                                    if (sides.isEmpty()) return@DominoTileView

                                    if (sides.size == 1) {
                                        val side = sides.first()
                                        DominoSoundManager.playTileClack(isDouble = tile.isDouble)
                                        DominoHaptics.tilePlaced(hapticView)
                                        if (side == BoardSide.LEFT) {
                                            engine.value.playTileLeft(actingSeat, tile)
                                        } else {
                                            engine.value.playTileRight(actingSeat, tile)
                                        }
                                        logMessage = engine.value.roundLog
                                        selectedTile = null
                                        gameStateVersion++
                                    } else if (engine.value.leftOpenPip == engine.value.rightOpenPip) {
                                        // Ambos extremos valen lo mismo: no hay elección real
                                        DominoSoundManager.playTileClack(isDouble = tile.isDouble)
                                        DominoHaptics.tilePlaced(hapticView)
                                        engine.value.playTileLeft(actingSeat, tile)
                                        logMessage = engine.value.roundLog
                                        selectedTile = null
                                        gameStateVersion++
                                    } else {
                                        DominoSoundManager.playTileTap()
                                        selectedTile = if (isSelected) null else tile
                                    }
                                }
                            )
                        }
                    }

                    // Choice buttons only when a selected tile matches two DIFFERENT open ends
                    if (isMyTurn && selectedTile != null && canPlaySelectedLeft && canPlaySelectedRight) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val t = selectedTile!!
                                    DominoSoundManager.playTileClack(isDouble = t.isDouble)
                                    DominoHaptics.tilePlaced(hapticView)
                                    engine.value.playTileLeft(actingSeat, t)
                                    selectedTile = null
                                    logMessage = engine.value.roundLog
                                    gameStateVersion++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(DominoStrings.endChoiceBtn(engine.value.leftOpenPip, true), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val t = selectedTile!!
                                    DominoSoundManager.playTileClack(isDouble = t.isDouble)
                                    DominoHaptics.tilePlaced(hapticView)
                                    engine.value.playTileRight(actingSeat, t)
                                    selectedTile = null
                                    logMessage = engine.value.roundLog
                                    gameStateVersion++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(DominoStrings.endChoiceBtn(engine.value.rightOpenPip, false), fontWeight = FontWeight.Bold, fontSize = 11.sp)
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

        // 5. ROUND FINISHED DIALOG
        if (showRoundDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = DominoStrings.roundDialogTitle(engine.value.winningTeam == 0),
                        color = if (engine.value.winningTeam == 0) GoldAccent else Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(engine.value.roundLog, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val scoreTeamA = engine.value.players.filter { it.team == 0 }.sumOf { it.score }
                        val scoreTeamB = engine.value.players.filter { it.team == 1 }.sumOf { it.score }
                        Text(DominoStrings.teamScoreLine(scoreTeamA, targetScore))
                        Text(DominoStrings.rivalScoreLine(scoreTeamB))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRoundDialog = false
                            if (engine.value.isMatchOver) {
                                onMatchFinished(engine.value.winningTeam == 0, betAmount * 2)
                            } else {
                                DominoSoundManager.playVenueMusic(venueId)
                                engine.value.startNewRound()
                                selectedTile = null
                                logMessage = engine.value.roundLog
                                gameStateVersion++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy)
                    ) {
                        Text(
                            DominoStrings.nextRoundOrFinishBtn(engine.value.isMatchOver),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRoundDialog = false
                            onBack()
                        }
                    ) {
                        Text(DominoStrings.exitToMenuBtn())
                    }
                }
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
fun PlayerBadge(player: Player, isCurrentTurn: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isCurrentTurn) GoldAccent.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentTurn) GoldAccent else Color.White.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${player.name}: ${player.hand.size}",
                color = if (isCurrentTurn) GoldAccent else Color.White,
                fontSize = 10.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
