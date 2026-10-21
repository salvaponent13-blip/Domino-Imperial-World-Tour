package com.example.dominograndprix.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.DominoBonusType
import com.example.dominograndprix.ui.components.DominoTileView
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Resumen de ronda / partida con el tanteo **real** del motor.
 *
 * Lo que se muestra sale de lo que el motor ya calculo, no de una reconstruccion paralela:
 * - [DominoEngine.lastRoundPointsAwarded] es lo que el cierre sumo al ganador.
 * - [DominoEngine.roundPointsByPlayer] son los puntos de mesa de los modos que puntuan extremos.
 * - Las bonificaciones (capicua/doble) se anuncian como etiqueta, sin inventar multiplicadores:
 *   el motor no aplica ninguno.
 */
@Composable
fun DominoRoundSummaryDialog(
    engine: DominoEngine,
    humanWon: Boolean,
    isFreePlay: Boolean = false,
    onNextRound: () -> Unit,
    onExitMatch: () -> Unit
) {
    val target = engine.targetScore
    val winnerPlayer = engine.winnerPlayerId?.let { engine.players.getOrNull(it) }
        ?: engine.players.firstOrNull { it.team == engine.winningTeam }
    val winnerName = winnerPlayer?.name ?: (if (humanWon) DominoStrings.hudYou() else DominoStrings.hudRival())
    val bonus = engine.lastRoundBonusType
    val isMatchOver = engine.isMatchOver
    val isBlocked = engine.lastRoundWasBlocked
    // Cerrar por dominó = no fue tranca y la mano del ganador quedo vacia. Si alcanzo la meta
    // jugando, su mano conserva fichas y no debe narrarse como dominó.
    val closedByDomino = !isBlocked && (winnerPlayer?.hand?.isEmpty() == true)
    val reachedTarget = !isBlocked && !closedByDomino
    val roundPoints = engine.lastRoundPointsAwarded
    val pointMode = engine.gameMode.isPointGame

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
                // 1. Encabezado
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
                            text = when {
                                isMatchOver && humanWon -> "🏆"
                                isMatchOver -> "💀"
                                isBlocked -> "🛡️"
                                else -> "✨"
                            },
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isMatchOver) {
                            if (humanWon) DominoStrings.summaryChallengeWon() else DominoStrings.summaryChallengeLost()
                        } else {
                            if (humanWon) DominoStrings.summaryHandWon() else DominoStrings.summaryHandLost()
                        },
                        color = if (humanWon) GoldAccent else Color(0xFFFF6B6B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = when {
                            isBlocked -> DominoStrings.summaryBlocked(winnerName)
                            reachedTarget -> DominoStrings.summaryReachedTarget(winnerName, target)
                            else -> DominoStrings.summaryClosedDomino(winnerName)
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
                                    DominoBonusType.DOUBLE_CAPICUA -> DominoStrings.bonusDoubleCapicua()
                                    DominoBonusType.CAPICUA -> DominoStrings.bonusCapicua()
                                    DominoBonusType.DOUBLE -> DominoStrings.bonusDominoWithDouble()
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

                // 2. Contenido desplazable
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = DominoStrings.summaryHandsTitle(),
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(engine.players, key = { it.id }) { player ->
                        val isPlayerWinner = (player.id == engine.winnerPlayerId) ||
                            (engine.isTeamGame && player.team == engine.winningTeam && player.hand.isEmpty())
                        val pipsSum = player.hand.sumOf { it.totalPips }
                        val countsTowardWinner = !isPlayerWinner &&
                            (if (engine.isTeamGame) player.team != engine.winningTeam else true)

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
                                                text = "(${if (player.team == 0) DominoStrings.hudYou() else DominoStrings.hudRival()})",
                                                color = Color.LightGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = when {
                                            player.hand.isEmpty() -> DominoStrings.summaryEmptyHand()
                                            countsTowardWinner -> DominoStrings.summaryCountsForWinner(pipsSum)
                                            else -> "$pipsSum pts"
                                        },
                                        color = when {
                                            player.hand.isEmpty() -> Color(0xFF34D399)
                                            countsTowardWinner -> GoldAccent
                                            else -> Color.LightGray
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                if (player.hand.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Scroll horizontal: con robo se pueden acumular 13 fichas y
                                    // antes se salian del dialogo.
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
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

                    // Seccion B: desglose real
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = DominoStrings.summaryBreakdownTitle(),
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (pointMode) {
                                    // En los modos de puntos lo que suma son las jugadas de mesa,
                                    // no los pips que quedan en las manos.
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(DominoStrings.summaryTablePoints(), color = Color.LightGray, fontSize = 11.sp)
                                        Text("$roundPoints pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    engine.roundPointsByPlayer
                                        .filterValues { it > 0 }
                                        .forEach { (seat, points) ->
                                            val name = engine.players.getOrNull(seat)?.name ?: "—"
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("· $name", color = Color.LightGray, fontSize = 10.sp)
                                                Text("+$points", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                } else {
                                    val opponentPips = engine.players
                                        .filter { it.team != engine.winningTeam }
                                        .sumOf { it.hand.sumOf { t -> t.totalPips } }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(DominoStrings.summaryRivalPips(), color = Color.LightGray, fontSize = 11.sp)
                                        Text("$opponentPips pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        DominoStrings.summaryTotalTo(winnerName),
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text("+$roundPoints pts", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Seccion C: marcador acumulado
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = DominoStrings.summaryStandings(target),
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (engine.isTeamGame) {
                                    val scoreTeam0 = engine.teamScore(0)
                                    val scoreTeam1 = engine.teamScore(1)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(DominoStrings.summaryTeamYou(), color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("$scoreTeam0 / $target pts", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(DominoStrings.summaryTeamRival(), color = Color(0xFFFECACA), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("$scoreTeam1 / $target pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                                text = "${p.score} / $target pts",
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

                // 3. Botones
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
                            text = when {
                                isMatchOver && !humanWon -> DominoStrings.summaryRetry()
                                isMatchOver && isFreePlay -> DominoStrings.summaryBackToMenu()
                                isMatchOver -> DominoStrings.summaryNextChallenge()
                                else -> DominoStrings.summaryNextRound()
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
                            text = DominoStrings.summaryExit(),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
