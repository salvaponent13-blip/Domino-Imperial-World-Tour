package com.example.dominograndprix.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

@Composable
fun RulesGuideScreen(
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1A24), Color(0xFF162534))
                )
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DominoStrings.rulesGuideTitle(),
                    color = GoldAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                LanguageSelectorButton()
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(GameMode.values()) { mode ->
                    RuleDetailCard(mode = mode)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(DominoStrings.backToMainMenu(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RuleDetailCard(mode: GameMode) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F2E3E),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = mode.title, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = DominoStrings.modeSummary(mode), color = Color.White, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = DominoStrings.modeStrategy(mode), color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
