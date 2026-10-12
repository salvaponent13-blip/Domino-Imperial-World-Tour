package com.example.dominograndprix.ui.vault

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
import com.example.dominograndprix.model.LuxuryItem
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

@Composable
fun TrophyRoomScreen(
    items: List<LuxuryItem>,
    playerCash: Int = 1500,
    onSellItem: (LuxuryItem) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141E30), Color(0xFF243B55))
                )
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("💎 SALA DE TROFEOS", color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("PREMIOS Y OBJETOS DE LUJO", color = Color.LightGray, fontSize = 11.sp)
                }
                Text("DINERO: $$playerCash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    TrophyCard(item = item, onSell = { onSellItem(item) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("VOLVER AL MENÚ", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TrophyCard(
    item: LuxuryItem,
    onSell: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E2A38),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "Obtenido en: ${item.cityName}", color = Color.LightGray, fontSize = 11.sp)
                Text(text = "Valor de mercado: $${item.value}", color = Color.White, fontSize = 12.sp)
            }

            if (!item.isSold) {
                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("EMPEÑAR (+$${item.value})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("VENDIDO", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
