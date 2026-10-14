package com.example.dominograndprix.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.R
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton
import com.example.dominograndprix.ui.components.CreditsButton
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

@Composable
fun MainTitleScreen(
    onStartWorldTour: () -> Unit = {},
    onQuickPlay: () -> Unit = {},
    onLocalMatch: () -> Unit = {},
    onShowRules: () -> Unit = {},
    onShowTrophies: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Cover image
        Image(
            painter = painterResource(id = R.drawable.bg_cover),
            contentDescription = "Domino Imperial Cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Gradient overlay at top and bottom so branding and buttons are crisp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Top Header with New Brand Title, 3D Icon Badge, Language Selector & Audio Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_icon),
                    contentDescription = "Domino Imperial Icon",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, GoldAccent, RoundedCornerShape(12.dp))
                )
                Column {
                    Text(
                        text = "DOMINO IMPERIAL",
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Text(
                        text = DominoStrings.editionSubtitle(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Selector Button (ES, EN, FR, IT, PT, DE)
                LanguageSelectorButton()

                // Audio Credits & Licenses Dialog Button
                CreditsButton()

                // Audio Toggle Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavy.copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                ) {
                    Text(
                        text = if (DominoSoundManager.isMuted) DominoStrings.mutedLabel() else DominoStrings.musicOn(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Menu Cards overlaid gracefully at bottom half
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // World Tour Button
            MenuOptionCard(
                title = DominoStrings.worldTourBtnTitle(),
                subtitle = DominoStrings.worldTourBtnSub(),
                containerColor = GoldAccent,
                contentColor = DarkNavy,
                onClick = onStartWorldTour
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Play Button (Direct to Havana)
            MenuOptionCard(
                title = DominoStrings.quickPlayBtnTitle(),
                subtitle = DominoStrings.quickPlayBtnSub(),
                containerColor = Color(0xFF1B6B3E),
                contentColor = Color.White,
                onClick = onQuickPlay
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Partida local (pass-and-play): dos personas, mismo dispositivo
            MenuOptionCard(
                title = DominoStrings.localMatchBtnTitle(),
                subtitle = DominoStrings.localMatchBtnSub(),
                containerColor = Color(0xFF2D3E5E),
                contentColor = Color.White,
                onClick = onLocalMatch
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rules Guide
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2836).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(onClick = onShowRules)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            DominoStrings.eightModesBtn(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Trophy Room
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2836).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(onClick = onShowTrophies)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            DominoStrings.trophyRoomBtn(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = DominoStrings.titleFooter(),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MenuOptionCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(text = title, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = contentColor.copy(alpha = 0.88f), fontSize = 11.sp)
        }
    }
}
