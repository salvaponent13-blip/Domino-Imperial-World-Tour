package com.example.dominograndprix.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dominograndprix.audio.DominoHaptics
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.billing.BillingManager
import com.example.dominograndprix.data.SettingsStore
import com.example.dominograndprix.i18n.TutorialStrings
import com.example.dominograndprix.billing.BillingNotice
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton

/**
 * Ajustes del jugador: sonido, vibracion, idioma, version PRO y enlaces de comunidad.
 *
 * Las preferencias se guardan en [com.example.dominograndprix.data.SettingsStore] y sobreviven al
 * cierre de la app.
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hapticsEnabled by remember { mutableStateOf(DominoHaptics.enabled) }
    // Aviso tras pulsar "Ver tutorial": el tutorial no se lanza desde aquí, se reactiva para la
    // próxima partida (así se ve sobre la mesa, que es donde tiene sentido).
    var tutorialReset by remember { mutableStateOf(false) }

    // Limpiar avisos de compra antiguos al cerrar el diálogo de ajustes.
    DisposableEffect(Unit) {
        onDispose {
            BillingManager.consumeNotice()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF131924).copy(alpha = 0.97f),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DominoStrings.settingsTitle(),
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingRow(
                    label = DominoStrings.settingsSound(),
                    value = if (DominoSoundManager.isMuted) DominoStrings.settingsOff() else DominoStrings.settingsOn(),
                    enabled = !DominoSoundManager.isMuted
                ) {
                    DominoSoundManager.toggleMute()
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingRow(
                    label = DominoStrings.settingsHaptics(),
                    value = if (hapticsEnabled) DominoStrings.settingsOn() else DominoStrings.settingsOff(),
                    enabled = hapticsEnabled
                ) {
                    hapticsEnabled = !hapticsEnabled
                    DominoHaptics.setEnabled(context, hapticsEnabled)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DominoStrings.settingsLanguage(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    LanguageSelectorButton()
                }

                // --- SECCIÓN QUITAR PUBLICIDAD (PRO) ---
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                val isPro by BillingManager.pro.collectAsState()
                val price by BillingManager.price.collectAsState()
                val busy by BillingManager.busy.collectAsState()
                val notice by BillingManager.notice.collectAsState()

                if (isPro) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = DominoStrings.proActiveBadge(),
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    val activity = context as? Activity
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { activity?.let { BillingManager.buy(it) } },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color(0xFF131924)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = DominoStrings.removeAdsBtn(price),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = { BillingManager.restore() },
                            enabled = !busy
                        ) {
                            Text(
                                text = DominoStrings.restorePurchasesBtn(),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                notice?.let { n ->
                    val noticeText = when (n) {
                        BillingNotice.PURCHASED -> DominoStrings.billingPurchasedNotice()
                        BillingNotice.RESTORED -> DominoStrings.billingRestoredNotice()
                        BillingNotice.CANCELED -> DominoStrings.billingCanceledNotice()
                        BillingNotice.NOT_READY -> DominoStrings.billingNotReadyNotice()
                        BillingNotice.NOTHING -> DominoStrings.billingNothingNotice()
                        BillingNotice.FAILED -> DominoStrings.billingFailedNotice()
                    }
                    Text(
                        text = noticeText,
                        color = if (n == BillingNotice.PURCHASED || n == BillingNotice.RESTORED) Color(0xFFA7F3D0) else Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                // --- VALORAR Y COMPARTIR APP ---
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DominoSoundManager.playTileTap()
                            val pkg = context.packageName
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = DominoStrings.rateAppBtn(),
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            DominoSoundManager.playTileTap()
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, DominoStrings.shareAppMessage())
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, null)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = DominoStrings.shareAppBtn(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- VOLVER A VER EL TUTORIAL ---
                TextButton(
                    onClick = {
                        DominoSoundManager.playTileTap()
                        SettingsStore.resetTutorial(context)
                        tutorialReset = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = DominoStrings.replayTutorialBtn(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (tutorialReset) {
                    Text(
                        text = TutorialStrings.replayHint(),
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color(0xFF131924)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(DominoStrings.settingsClose(), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (enabled) Color(0xFF064E3B) else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(
                    1.dp,
                    if (enabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = value,
                    color = if (enabled) Color(0xFFA7F3D0) else Color.LightGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

