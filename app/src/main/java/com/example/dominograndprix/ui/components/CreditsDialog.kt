package com.example.dominograndprix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

data class AudioCreditItem(
    val title: String,
    val attribution: String,
    val usageNotes: String
)

private val KEVIN_MACLEOD_CREDITS = listOf(
    AudioCreditItem(
        title = "Backbay Lounge",
        attribution = "Backbay Lounge Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Menú Principal • Los Ángeles"
    ),
    AudioCreditItem(
        title = "Modern Jazz Samba",
        attribution = "Modern Jazz Samba Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Circuito Mundial (Mapa)"
    ),
    AudioCreditItem(
        title = "No Frills Salsa",
        attribution = "No Frills Salsa Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "La Habana • Miami"
    ),
    AudioCreditItem(
        title = "No Frills Cumbia",
        attribution = "No Frills Cumbia Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Ciudad de México"
    ),
    AudioCreditItem(
        title = "Beachfront Celebration",
        attribution = "Beachfront Celebration Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "San Juan (Puerto Rico)"
    ),
    AudioCreditItem(
        title = "The Entertainer",
        attribution = "The Entertainer Kevin MacLeod (incompetech.com)\nComposed by Scott Joplin (1902, Public Domain)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Nueva Orleans"
    ),
    AudioCreditItem(
        title = "Hot Swing",
        attribution = "Hot Swing Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Las Vegas • Bronx"
    ),
    AudioCreditItem(
        title = "Fox Tale Waltz Part 1",
        attribution = "Fox Tale Waltz Part 1 Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Montecarlo"
    ),
    AudioCreditItem(
        title = "Eastminster",
        attribution = "Eastminster Kevin MacLeod (incompetech.com)\nLicensed under Creative Commons: By Attribution 4.0 License",
        usageNotes = "Tokio"
    )
)

@Composable
fun CreditsButton(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkNavy.copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
        modifier = modifier.clickable {
            DominoSoundManager.playTileTap()
            showDialog = true
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Text(
                text = "⚖️ ${DominoStrings.creditsBtnLabel()}",
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }

    if (showDialog) {
        CreditsDialog(onDismiss = { showDialog = false })
    }
}

@Composable
fun CreditsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F1A26),
            border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DominoStrings.creditsDialogTitle(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 1: Music (CC-BY 4.0)
                    item {
                        Text(
                            text = DominoStrings.musicCreditsSection(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(KEVIN_MACLEOD_CREDITS) { credit ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF162332),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = credit.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = credit.usageNotes,
                                        color = GoldAccent.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = credit.attribution,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Section 2: Procedural Music & Sound FX
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = DominoStrings.proceduralMusicSection(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF162332),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = DominoStrings.proceduralMusicBody(),
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(
                            text = DominoStrings.closeLabel(),
                            color = DarkNavy,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
