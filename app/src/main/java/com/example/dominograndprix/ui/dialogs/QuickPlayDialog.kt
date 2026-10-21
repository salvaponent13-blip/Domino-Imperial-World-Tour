package com.example.dominograndprix.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode

private val GoldAccent = Color(0xFFFFD700)
private val DarkNavy = Color(0xFF131924)
private val CardBg = Color(0xFF1C2534)

data class QuickPlayConfig(
    val gameMode: GameMode,
    val targetScore: Int,
    val playerCount: Int,
    val isTeamGame: Boolean,
    val difficulty: Difficulty,
    /** Cuantos asientos ocupa una persona delante del mismo movil (1 o 2). */
    val humanPlayerCount: Int = 1
)

@Composable
fun QuickPlayDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    onStartMatch: (QuickPlayConfig) -> Unit
) {
    if (!isOpen) return

    var selectedMode by remember { mutableStateOf(GameMode.ALL_FIVES) }
    var selectedScore by remember { mutableIntStateOf(50) }
    var selectedPlayerCount by remember { mutableIntStateOf(4) }
    var isTeamGame by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.STANDARD) }
    var selectedHumanCount by remember { mutableIntStateOf(1) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkNavy,
            border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.55f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera con título y botón de cierre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "CONFIGURAR PARTIDA RÁPIDA",
                            color = GoldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                DominoSoundManager.playTileTap()
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. MODALIDADES DE JUEGO (8 MODOS)
                Text(
                    text = "1. MODALIDAD DE JUEGO",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val modes = GameMode.values()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in modes.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mode1 = modes[i]
                            val isSel1 = (mode1 == selectedMode)
                            ModeCard(
                                mode = mode1,
                                isSelected = isSel1,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    DominoSoundManager.playTileTap()
                                    selectedMode = mode1
                                }
                            )

                            if (i + 1 < modes.size) {
                                val mode2 = modes[i + 1]
                                val isSel2 = (mode2 == selectedMode)
                                ModeCard(
                                    mode = mode2,
                                    isSelected = isSel2,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        DominoSoundManager.playTileTap()
                                        selectedMode = mode2
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. OBJETIVO DE PUNTOS
                Text(
                    text = "2. OBJETIVO DE PUNTOS",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 50, 100, 150).forEach { score ->
                        val isSel = (score == selectedScore)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) GoldAccent else CardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) GoldAccent else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    DominoSoundManager.playTileTap()
                                    selectedScore = score
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "$score pts",
                                    color = if (isSel) DarkNavy else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. JUGADORES Y FORMATO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Jugadores
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "3. JUGADORES",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4).forEach { count ->
                                val isSel = (count == selectedPlayerCount)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) GoldAccent else CardBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) GoldAccent else Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            DominoSoundManager.playTileTap()
                                            selectedPlayerCount = count
                                            if (count == 2) isTeamGame = false
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "$count Jugadores",
                                            color = if (isSel) DarkNavy else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Formato (Individual vs Parejas)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "4. FORMATO",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Individual
                            val isIndiv = !isTeamGame
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isIndiv) GoldAccent else CardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isIndiv) GoldAccent else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        DominoSoundManager.playTileTap()
                                        isTeamGame = false
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Individual",
                                        color = if (isIndiv) DarkNavy else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Parejas
                            val isTeams = isTeamGame
                            val canDoTeams = (selectedPlayerCount == 4)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTeams && canDoTeams) GoldAccent else CardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isTeams && canDoTeams) GoldAccent else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = canDoTeams) {
                                        DominoSoundManager.playTileTap()
                                        isTeamGame = true
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Parejas (2v2)",
                                        color = if (!canDoTeams) Color.White.copy(alpha = 0.3f) else if (isTeams) DarkNavy else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. DIFICULTAD DE LA IA
                Text(
                    text = "5. DIFICULTAD DE LA IA",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Difficulty.RELAXED to "Fácil",
                        Difficulty.STANDARD to "Estándar",
                        Difficulty.TOUGH to "Difícil"
                    ).forEach { (diff, label) ->
                        val isSel = (diff == selectedDifficulty)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) GoldAccent else CardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) GoldAccent else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    DominoSoundManager.playTileTap()
                                    selectedDifficulty = diff
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) DarkNavy else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. MODO: un jugador contra la IA o dos personas pasandose el movil
                Text(
                    text = DominoStrings.quickPlayModeSection(),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1 to DominoStrings.quickPlayOneHuman(),
                        2 to DominoStrings.quickPlayTwoHumans()
                    ).forEach { (humans, label) ->
                        val isSel = (humans == selectedHumanCount)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) GoldAccent else CardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) GoldAccent else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    DominoSoundManager.playTileTap()
                                    selectedHumanCount = humans
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) DarkNavy else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // BOTÓN COMENZAR PARTIDA
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GoldAccent,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable {
                            DominoSoundManager.playTileClack(isDouble = true)
                            onStartMatch(
                                QuickPlayConfig(
                                    gameMode = selectedMode,
                                    targetScore = selectedScore,
                                    playerCount = selectedPlayerCount,
                                    isTeamGame = isTeamGame,
                                    difficulty = selectedDifficulty,
                                    humanPlayerCount = selectedHumanCount
                                )
                            )
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GoldAccent, Color(0xFFFFB300))
                                )
                            )
                    ) {
                        Text(
                            text = "▶ COMENZAR PARTIDA AHORA",
                            color = DarkNavy,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GoldAccent.copy(alpha = 0.18f) else CardBg,
        border = BorderStroke(
            1.dp,
            if (isSelected) GoldAccent else Color.White.copy(alpha = 0.10f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = mode.title,
                color = if (isSelected) GoldAccent else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mode.summary,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
