package com.example.dominograndprix.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Text(
                text = "📖 GUÍA DE LAS 8 MODALIDADES",
                color = GoldAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                Text("ENTENDIDO, VOLVER AL MENÚ", fontWeight = FontWeight.Bold)
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
            Text(text = mode.summary, color = Color.White, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))
            val detail = when (mode) {
                GameMode.MATADOR -> "Comodines Matadores: [6|1], [5|2], [4|3] y el doble blanco [0|0]. Si juegas [0|0], ¡sólo otro Matador puede unirse a él!"
                GameMode.BLOCK_HUNGARIAN -> "¡Juega todas las fichas válidas que puedas en una sola tirada sin límite hasta que no tengas más movimientos!"
                GameMode.FIVES_AND_THREES -> "Sumar múltiplos de 5 otorga puntos; múltiplos de 3 otorga puntos; ¡si sumas 15 (ambos), la puntuación es doble!"
                GameMode.ALL_FIVES -> "Los puntos se suman al marcador en cada turno si la suma de los extremos abiertos es 5, 10, 15, 20..."
                GameMode.ALL_THREES -> "Puntuas inmediatamente si la suma de extremos abiertos es 3, 6, 9, 12, 15, 18..."
                GameMode.DRAW_SPECIAL -> "Cada jugador sólo tira en su propio extremo de mesa o en el del jugador que se vio obligado a pasar."
                GameMode.BLOCK -> "Sin pozo. Si no tienes ficha, debes pasar. Ganar por cierre otorga los puntos de mano de los rivales."
                GameMode.DRAW -> "El dominó clásico más extendido del mundo. Roba del pozo si no puedes jugar."
            }
            Text(text = "Estrategia clave: $detail", color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
