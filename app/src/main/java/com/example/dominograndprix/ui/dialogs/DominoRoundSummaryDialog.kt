package com.example.dominograndprix.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.DominoBonusType
import com.example.dominograndprix.model.Player
import com.example.dominograndprix.ui.components.DominoTileView
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Diálogo detallado de tanteo y resumen de ronda / partida copiado fielmente de tour-domino-main.
 *
 * Muestra:
 * 1. Encabezado con ícono y título de victoria/derrota.
 * 2. Motivo del cierre (¡Dominó! o Tranca con menor conteo).
 * 3. Fichas que le quedaron a cada jugador en mano con representación gráfica y puntos.
 * 4. Desglose exacto de puntos sumados (suma de rivales, bonificaciones, total).
 * 5. Marcador acumulado hacia la meta.
 * 6. Botones de acción.
 */
@Composable
fun DominoRoundSummaryDialog(
    engine: DominoEngine,
    targetScore: Int,
    humanWon: Boolean,
    onNextRound: () -> Unit,
    onExitMatch: () -> Unit
) {
    val winnerPlayer = engine.winnerPlayerId?.let { engine.players.getOrNull(it) }
        ?: engine.players.firstOrNull { it.team == engine.winningTeam }
    val winnerName = winnerPlayer?.name ?: (if (humanWon) "Tú" else "Rival")
    val bonus = engine.lastRoundBonusType
    val isMatchOver = engine.isMatchOver
    val isBlocked = engine.lastRoundWasBlocked

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkNavy.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ENCABEZADO CON ÍCONO Y TÍTULO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.15f))
                            .border(1.5.dp, GoldAccent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMatchOver) "🏆" else if (isBlocked) "🛡️" else "✨",
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isMatchOver) {
                            if (humanWon) "¡VICTORIA EN EL RETO!" else "DERROTA EN EL RETO"
                        } else {
                            if (humanWon) "¡MANO GANADA!" else "MANO PERDIDA"
                        },
                        color = if (humanWon) GoldAccent else Color(0xFFFF6B6B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (!isBlocked) {
                            "$winnerName cerró la mano haciendo ¡Dominó!"
                        } else {
                            "Mesa cerrada por Tranca · Gana $winnerName por menor conteo"
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    if (bonus != DominoBonusType.NONE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldAccent.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = when (bonus) {
                                    DominoBonusType.DOUBLE_CAPICUA -> "🌟 ¡¡DOBLE CAPICÚA!! (×3) 🌟"
                                    DominoBonusType.CAPICUA -> "✨ ¡¡CAPICÚA!! (×2) ✨"
                                    DominoBonusType.DOUBLE -> "🎯 ¡DOMINÓ CON DOBLE! 🎯"
                                    DominoBonusType.NONE -> ""
                                },
                                color = GoldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. CONTENIDO DESPLAZABLE: FICHAS EN MANO + DESGLOSE + MARCADOR ACUMULADO
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SECCIÓN A: Fichas en Mano de cada jugador
                    item {
                        Text(
                            text = "FICHAS EN MANO AL CERRARSE LA RONDA",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(engine.players) { player ->
                        val isPlayerWinner = (player.id == engine.winnerPlayerId) ||
                            (engine.isTeamGame && player.team == engine.winningTeam && player.hand.isEmpty())
                        val pipsSum = player.hand.sumOf { it.totalPips }
                        val countsTowardWinner = !isPlayerWinner && (if (engine.isTeamGame) player.team != engine.winningTeam else true)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPlayerWinner) Color(0xFF064E3B).copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlayerWinner) Color(0xFF34D399).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = player.name,
                                            color = if (player.id == 0) Color(0xFFA7F3D0) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        if (engine.isTeamGame) {
                                            Text(
                                                text = if (player.team == 0) "(Eq. Tú)" else "(Eq. Rival)",
                                                color = Color.LightGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (player.hand.isEmpty()) "¡Sin fichas! (0 pts)"
                                        else if (countsTowardWinner) "Suma al ganador: +$pipsSum pts"
                                        else "$pipsSum pts",
                                        color = if (player.hand.isEmpty()) Color(0xFF34D399)
                                        else if (countsTowardWinner) GoldAccent
                                        else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                if (player.hand.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        player.hand.forEach { tile ->
                                            DominoTileView(
                                                tile = tile,
                                                width = 18.dp,
                                                height = 32.dp,
                                                isPlayable = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN B: Desglose de Puntos
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "DESGLOSE DE PUNTOS SUMADOS",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val opponentPips = engine.players
                                    .filter { it.team != engine.winningTeam }
                                    .sumOf { it.hand.sumOf { t -> t.totalPips } }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Suma de fichas rivales:", color = Color.LightGray, fontSize = 11.sp)
                                    Text("$opponentPips pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                if (bonus != DominoBonusType.NONE) {
                                    val multiplier = when (bonus) {
                                        DominoBonusType.DOUBLE_CAPICUA -> 3
                                        DominoBonusType.CAPICUA -> 2
                                        else -> 1
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Bonificación $bonus:", color = GoldAccent, fontSize = 11.sp)
                                        Text("×$multiplier", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                val totalAwarded = engine.lastPointsEarned.coerceAtLeast(opponentPips)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total sumado a $winnerName:", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("+$totalAwarded pts", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // SECCIÓN C: Marcador Acumulado
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "MARCADOR ACUMULADO (OBJETIVO: $targetScore PTS)",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (engine.isTeamGame) {
                                    val scoreTeam0 = engine.players.filter { it.team == 0 }.sumOf { it.score }
                                    val scoreTeam1 = engine.players.filter { it.team == 1 }.sumOf { it.score }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Equipo Tú:", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("$scoreTeam0 / $targetScore pts", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Equipo Rival:", color = Color(0xFFFECACA), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("$scoreTeam1 / $targetScore pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                } else {
                                    engine.players.forEach { p ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = p.name,
                                                color = if (p.id == 0) Color(0xFFA7F3D0) else Color.White,
                                                fontWeight = if (p.id == 0) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${p.score} / $targetScore pts",
                                                color = if (p.id == 0) GoldAccent else Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. BOTONES DE ACCIÓN
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onNextRound,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isMatchOver) {
                                if (humanWon) "SIGUIENTE RETO ➔" else "REINTENTAR RETO"
                            } else {
                                "SIGUIENTE RONDA ➔"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = onExitMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Text(
                            text = "SALIR AL MENÚ",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
