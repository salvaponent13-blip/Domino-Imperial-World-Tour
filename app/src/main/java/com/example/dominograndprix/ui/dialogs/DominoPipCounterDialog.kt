package com.example.dominograndprix.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Contador de fichas por numero (0 al 6).
 *
 * En un domino de 28 fichas cada numero aparece en 7 fichas. Se muestra cuantas estan en la
 * mesa, cuantas en la mano del jugador y cuantas quedan ocultas (rivales + pozo). El pozo se
 * cuenta aparte porque en los modos con robo es informacion publica y cambia la decision.
 */
@Composable
fun DominoPipCounterDialog(
    engine: DominoEngine,
    humanSeat: Int = 0,
    onDismiss: () -> Unit
) {
    val humanHand = engine.players.getOrNull(humanSeat)?.hand ?: emptyList()
    val boneyardTiles = engine.boneyard

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkNavy.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF064E3B))
                                .border(1.dp, Color(0xFF34D399), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Column {
                            Text(
                                DominoStrings.pipCounterTitle(),
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(DominoStrings.pipCounterSubtitle(), color = Color.LightGray, fontSize = 9.sp)
                        }
                    }

                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                        Text("✕", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(DominoStrings.colNumber(), color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                    Text(DominoStrings.colOnBoard(), color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(DominoStrings.colInHand(), color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(DominoStrings.colRemaining(), color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                for (pip in 0..6) {
                    val playedOnBoard = engine.boardChain.count { it.tile.high == pip || it.tile.low == pip }
                    val inHand = humanHand.count { it.high == pip || it.low == pip }
                    val inBoneyard = boneyardTiles.count { it.high == pip || it.low == pip }
                    val hidden = (7 - playedOnBoard - inHand).coerceAtLeast(0)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (hidden == 0) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (hidden == 0) Color.White.copy(alpha = 0.1f) else GoldAccent.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = "$pip",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                if (hidden == 0) {
                                    Text(
                                        DominoStrings.exhaustedLabel(),
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text("$playedOnBoard / 7", color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text("$inHand", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text(
                                text = if (inBoneyard > 0) "$hidden ($inBoneyard)" else "$hidden",
                                color = if (hidden == 0) Color.Gray else if (hidden <= 2) Color(0xFFF87171) else GoldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(DominoStrings.closeLabel(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
