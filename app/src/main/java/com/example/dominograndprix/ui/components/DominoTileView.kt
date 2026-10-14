package com.example.dominograndprix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.DominoTile

val SelectedGlow = Color(0xFFFFD700)

/**
 * Photorealistic 3D Fournier Chamelo (F06562) Domino Tile rendered via multi-layer Canvas shaders:
 * 1. Multi-step soft ambient drop shadow on the table felt
 * 2. 3D extruded bicapa side walls (lower glossy ebony phenolic layer + upper shaded ivory wall)
 * 3. Outer 3D chamfered bevel gradient (bright top-left light edge -> warm shadowed bottom-right slope)
 * 4. Raised polished acrylic ivory plateau with diagonal lighting gradient & top-left specular sheen
 * 5. Carved 3D V-groove center divider (dark channel + upper shadow + lower specular lip)
 * 6. 3D spherical metallic brass spinner pin (clavo central dorado) with cast shadow and radial highlight
 * 7. 3D concave drilled pips with top-left cavity shadow, bottom-right illuminated rim, radial enamel pit, and glossy glint
 */
@Composable
fun DominoTileView(
    tile: DominoTile,
    modifier: Modifier = Modifier,
    pip1: Int = tile.high,
    pip2: Int = tile.low,
    isSelected: Boolean = false,
    isPlayable: Boolean = true,
    isHorizontal: Boolean = false,
    dimUnplayable: Boolean = false,
    width: Dp = if (isHorizontal) 62.dp else 34.dp,
    height: Dp = if (isHorizontal) 34.dp else 62.dp,
    onClick: (() -> Unit)? = null
) {
    // Semantica para lectores de pantalla. La ficha se dibuja sobre un Canvas, que para
    // TalkBack es un rectangulo mudo: sin esto, la mano del jugador es inaccesible.
    //
    // - contentDescription: que ficha es, con los numeros deletreados ("Ficha seis tres").
    // - stateDescription: si se puede jugar o no, para no obligar a deducirlo del color.
    // - onClickLabel: que pasara al pulsarla, que es lo que anuncia el lector.
    val semanticsMod = Modifier.semantics {
        contentDescription = DominoStrings.tileDescription(tile.high, tile.low)
        stateDescription = if (isPlayable) {
            DominoStrings.playableLabel()
        } else {
            DominoStrings.unplayableLabel()
        }
        if (isSelected) {
            selected = true
        }
    }

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            enabled = isPlayable,
            onClickLabel = if (isPlayable) DominoStrings.tileClickAction() else null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .then(semanticsMod)
            .then(clickMod)
    ) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cornerRadiusPx = minDim * 0.16f
        val corner = CornerRadius(cornerRadiusPx, cornerRadiusPx)

        // Reserve space on right/bottom for 3D extrusion + drop shadow
        val extrudeX = (minDim * 0.075f).coerceAtLeast(1.8f)
        val extrudeY = (minDim * 0.095f).coerceAtLeast(2.2f)
        val bodyW = w - extrudeX
        val bodyH = h - extrudeY

        // 1. Soft multi-layer ambient drop shadow on the felt
        drawRoundRect(
            color = Color(0x55000000),
            topLeft = Offset(extrudeX * 1.35f, extrudeY * 1.45f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color(0x88000000),
            topLeft = Offset(extrudeX * 1.05f, extrudeY * 1.15f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 2. 3D Extruded Bicapa Side Wall: Lower Ebony Layer (Fournier Chamelo black phenolic back)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2B2927), Color(0xFF0A0908)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            topLeft = Offset(extrudeX * 0.82f, extrudeY * 0.85f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 3. 3D Extruded Side Wall: Upper Shaded Ivory Layer (half-thickness step)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFC9BEA3), Color(0xFF8C7E62)),
                start = Offset(0f, 0f),
                end = Offset(bodyW, bodyH)
            ),
            topLeft = Offset(extrudeX * 0.42f, extrudeY * 0.44f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 4. Outer 3D Chamfered Bevel Slope (Top-Left lit white -> Bottom-Right warm shadow)
        //
        // La version apagada conserva el modelado (luz arriba-izquierda, sombra abajo-derecha)
        // aunque este atenuada. Antes usaba dos grises casi iguales (contraste 1,06): la ficha
        // se leia como una mancha plana, y eso es lo que el jugador describio como "borrosa".
        // Una ficha desenfocada y una sin relieve se perciben igual.
        val bevelBrush = if (dimUnplayable && !isPlayable) {
            Brush.linearGradient(
                colors = listOf(Color(0xFFC8C2B4), Color(0xFF8A8272)),
                start = Offset(0f, 0f),
                end = Offset(bodyW, bodyH)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF3ECD8),
                    Color(0xFFC2B598),
                    Color(0xFF9E8F70)
                ),
                start = Offset(0f, 0f),
                end = Offset(bodyW, bodyH)
            )
        }
        drawRoundRect(
            brush = bevelBrush,
            topLeft = Offset.Zero,
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 5. Raised Inner Acrylic Ivory Plateau (gives the cushioned 3D bevel edge)
        val bevelInset = (minDim * 0.065f).coerceAtLeast(1.4f)
        val innerCorner = CornerRadius(
            (cornerRadiusPx - bevelInset * 0.6f).coerceAtLeast(2f),
            (cornerRadiusPx - bevelInset * 0.6f).coerceAtLeast(2f)
        )
        val faceBrush = if (dimUnplayable && !isPlayable) {
            Brush.linearGradient(
                colors = listOf(Color(0xFFD6D0C0), Color(0xFFB0A894)),
                start = Offset(bevelInset, bevelInset),
                end = Offset(bodyW - bevelInset, bodyH - bevelInset)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFEF9), // Bright warm ivory top-left
                    Color(0xFFF8F3E3), // Creamy center-left
                    Color(0xFFEDE4CC), // Warm ivory center-right
                    Color(0xFFDDD0B3)  // Shaded ivory bottom-right
                ),
                start = Offset(0f, 0f),
                end = Offset(bodyW, bodyH)
            )
        }
        drawRoundRect(
            brush = faceBrush,
            topLeft = Offset(bevelInset, bevelInset),
            size = Size(bodyW - bevelInset * 2f, bodyH - bevelInset * 2f),
            cornerRadius = innerCorner
        )

        // 6. Specular Acrylic Gloss Highlight along Top-Left Inner Ridge
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xEEFFFFFF),
                    Color(0x44FFFFFF),
                    Color(0x00FFFFFF),
                    Color(0x558A7B5E)
                ),
                start = Offset(0f, 0f),
                end = Offset(bodyW, bodyH)
            ),
            topLeft = Offset(bevelInset, bevelInset),
            size = Size(bodyW - bevelInset * 2f, bodyH - bevelInset * 2f),
            cornerRadius = innerCorner,
            style = Stroke(width = (minDim * 0.028f).coerceAtLeast(0.8f))
        )

        // 7. Crisp Outer Contour (or Gold Selection Halo)
        drawRoundRect(
            color = if (isSelected) SelectedGlow else Color(0xFF2D251C),
            topLeft = Offset.Zero,
            size = Size(bodyW, bodyH),
            cornerRadius = corner,
            style = Stroke(width = if (isSelected) 2.2.dp.toPx() else (minDim * 0.025f).coerceAtLeast(0.8f))
        )

        // 7b. Senal NO CROMATICA de jugabilidad.
        //
        // Hasta ahora lo unico que distinguia una ficha jugable de una que no era el color
        // (la ficha no jugable se pinta mas apagada), y eso deja fuera a quien no distingue
        // bien los tonos. Un contorno discontinuo es una senal de forma: se percibe aunque no
        // se distinga el color, y no tapa el dibujo de la ficha.
        if (dimUnplayable && !isPlayable && !isSelected) {
            val dash = (minDim * 0.12f).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = Color(0x99FFFFFF),
                topLeft = Offset.Zero,
                size = Size(bodyW, bodyH),
                cornerRadius = corner,
                style = Stroke(
                    width = (minDim * 0.045f).coerceAtLeast(1.4f),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(dash, dash * 0.7f)
                    )
                )
            )
        }

        // 8. 3D Engraved V-Groove Center Divider & Spherical Brass Spinner Pin
        val isVert = !isHorizontal
        val grooveThickness = (minDim * 0.038f).coerceAtLeast(1.1f)
        val centerPin: Offset

        if (isVert) {
            val midY = bodyH / 2f
            val linePad = bodyW * 0.11f
            // Upper bevel shadow of V-groove
            drawLine(
                color = Color(0xFF9C8E73),
                start = Offset(linePad, midY - grooveThickness * 0.65f),
                end = Offset(bodyW - linePad, midY - grooveThickness * 0.65f),
                strokeWidth = grooveThickness * 0.7f
            )
            // Lower specular highlight lip of V-groove
            drawLine(
                color = Color(0xFFFFFFFF),
                start = Offset(linePad, midY + grooveThickness * 0.75f),
                end = Offset(bodyW - linePad, midY + grooveThickness * 0.75f),
                strokeWidth = grooveThickness * 0.75f
            )
            // Deep center carved channel
            drawLine(
                color = Color(0xFF2E251B),
                start = Offset(linePad, midY),
                end = Offset(bodyW - linePad, midY),
                strokeWidth = grooveThickness
            )
            centerPin = Offset(bodyW / 2f, midY)
        } else {
            val midX = bodyW / 2f
            val linePad = bodyH * 0.11f
            // Left bevel shadow of V-groove
            drawLine(
                color = Color(0xFF9C8E73),
                start = Offset(midX - grooveThickness * 0.65f, linePad),
                end = Offset(midX - grooveThickness * 0.65f, bodyH - linePad),
                strokeWidth = grooveThickness * 0.7f
            )
            // Right specular highlight lip of V-groove
            drawLine(
                color = Color(0xFFFFFFFF),
                start = Offset(midX + grooveThickness * 0.75f, linePad),
                end = Offset(midX + grooveThickness * 0.75f, bodyH - linePad),
                strokeWidth = grooveThickness * 0.75f
            )
            // Deep center carved channel
            drawLine(
                color = Color(0xFF2E251B),
                start = Offset(midX, linePad),
                end = Offset(midX, bodyH - linePad),
                strokeWidth = grooveThickness
            )
            centerPin = Offset(midX, bodyH / 2f)
        }

        // Spherical 3D Brass Spinner Rivet (Clavo Central Dorado)
        val pinRadius = (minDim * 0.082f).coerceAtLeast(1.8f)
        // Drop shadow cast by the raised brass dome onto bottom-right ivory
        drawCircle(
            color = Color(0x66241B0C),
            radius = pinRadius * 1.25f,
            center = Offset(centerPin.x + pinRadius * 0.22f, centerPin.y + pinRadius * 0.25f)
        )
        // Recessed dark seating ring
        drawCircle(
            color = Color(0xFF47330B),
            radius = pinRadius * 1.15f,
            center = centerPin
        )
        // 3D Spherical Gold Dome with off-center specular light source
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFEF2), // Intense white-gold glint
                    Color(0xFFFCE068), // Bright polished gold
                    Color(0xFFD69F26), // Rich brass midtone
                    Color(0xFF6E4B0A)  // Dark bronze shaded rim
                ),
                center = Offset(centerPin.x - pinRadius * 0.32f, centerPin.y - pinRadius * 0.32f),
                radius = pinRadius * 1.35f
            ),
            radius = pinRadius,
            center = centerPin
        )

        // 9. Draw 3D Concave Drilled Pips in each half with generous 27% margin
        if (isVert) {
            val halfH = bodyH / 2f
            drawFournier3DHalfPips(
                hx = 0f,
                hy = 0f,
                hw = bodyW,
                hh = halfH,
                count = pip1,
                isVert = true
            )
            drawFournier3DHalfPips(
                hx = 0f,
                hy = halfH,
                hw = bodyW,
                hh = halfH,
                count = pip2,
                isVert = true
            )
        } else {
            val halfW = bodyW / 2f
            drawFournier3DHalfPips(
                hx = 0f,
                hy = 0f,
                hw = halfW,
                hh = bodyH,
                count = pip1,
                isVert = false
            )
            drawFournier3DHalfPips(
                hx = halfW,
                hy = 0f,
                hw = halfW,
                hh = bodyH,
                count = pip2,
                isVert = false
            )
        }
    }
}

private fun DrawScope.drawFournier3DHalfPips(
    hx: Float,
    hy: Float,
    hw: Float,
    hh: Float,
    count: Int,
    isVert: Boolean
) {
    if (count <= 0) return

    // 27% margin keeps pips comfortably centered and away from the tile bevel and divider
    val padX = hw * 0.27f
    val padY = hh * 0.27f
    val left = hx + padX
    val right = hx + hw - padX
    val top = hy + padY
    val bottom = hy + hh - padY
    val midX = (left + right) / 2f
    val midY = (top + bottom) / 2f

    val points: List<Offset> = when (count) {
        1 -> listOf(Offset(midX, midY))
        2 -> if (isVert) {
            listOf(Offset(left, top), Offset(right, bottom))
        } else {
            listOf(Offset(right, top), Offset(left, bottom))
        }
        3 -> if (isVert) {
            listOf(Offset(left, top), Offset(midX, midY), Offset(right, bottom))
        } else {
            listOf(Offset(right, top), Offset(midX, midY), Offset(left, bottom))
        }
        4 -> listOf(
            Offset(left, top),
            Offset(right, top),
            Offset(left, bottom),
            Offset(right, bottom)
        )
        5 -> listOf(
            Offset(left, top),
            Offset(right, top),
            Offset(midX, midY),
            Offset(left, bottom),
            Offset(right, bottom)
        )
        6 -> if (isVert) {
            // Vertical 6: 2 columns of 3 pips
            listOf(
                Offset(left, top), Offset(right, top),
                Offset(left, midY), Offset(right, midY),
                Offset(left, bottom), Offset(right, bottom)
            )
        } else {
            // Horizontal 6: 2 rows of 3 pips
            listOf(
                Offset(left, top), Offset(midX, top), Offset(right, top),
                Offset(left, bottom), Offset(midX, bottom), Offset(right, bottom)
            )
        }
        else -> emptyList()
    }

    val pipRadius = (hw.coerceAtMost(hh) * 0.092f).coerceAtLeast(1.4f)
    val rimOffset = (pipRadius * 0.18f).coerceAtLeast(0.45f)

    for (pt in points) {
        // 1. Bottom-right illuminated ivory rim of the drilled cavity (catches overhead top-left light)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = pipRadius * 1.18f,
            center = Offset(pt.x + rimOffset, pt.y + rimOffset)
        )
        // 2. Top-left recessed ivory wall shadow inside the drilled rim
        drawCircle(
            color = Color(0xFF8C7E65),
            radius = pipRadius * 1.15f,
            center = Offset(pt.x - rimOffset * 0.85f, pt.y - rimOffset * 0.85f)
        )
        // 3. Concave glossy black enamel pit (deep black upper-left shadow -> subtle warm charcoal lower-right bowl)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3A3A3A), // Lower-right concave reflection
                    Color(0xFF141414), // Glossy black enamel body
                    Color(0xFF030303)  // Deep upper-left cavity shadow
                ),
                center = Offset(pt.x + pipRadius * 0.28f, pt.y + pipRadius * 0.28f),
                radius = pipRadius * 1.15f
            ),
            radius = pipRadius,
            center = pt
        )
        // 4. Tiny crisp enamel specular glint
        drawCircle(
            color = Color(0x99FFFFFF),
            radius = pipRadius * 0.24f,
            center = Offset(pt.x - pipRadius * 0.32f, pt.y - pipRadius * 0.32f)
        )
    }
}

/**
 * 3D Face-down Domino Tile for the Boneyard (Pozo) and Opponent hands.
 */
@Composable
fun DominoBackTileView(
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    width: Dp = if (isHorizontal) 48.dp else 26.dp,
    height: Dp = if (isHorizontal) 26.dp else 48.dp,
    highlightBorder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .then(clickMod)
    ) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val corner = CornerRadius(minDim * 0.16f, minDim * 0.16f)
        val extrudeX = minDim * 0.08f
        val extrudeY = minDim * 0.10f
        val bodyW = w - extrudeX
        val bodyH = h - extrudeY

        // 1. Soft drop shadow
        drawRoundRect(
            color = Color(0x88000000),
            topLeft = Offset(extrudeX * 1.15f, extrudeY * 1.25f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 2. Lower Ivory layer visible from the back (bicapa)
        drawRoundRect(
            color = Color(0xFFB8AD93),
            topLeft = Offset(extrudeX * 0.55f, extrudeY * 0.60f),
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 3. 3D Polished Ebony Back Surface with bevel gradient
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3B3732),
                    Color(0xFF1F1C19),
                    Color(0xFF0E0C0A)
                ),
                start = Offset.Zero,
                end = Offset(bodyW, bodyH)
            ),
            topLeft = Offset.Zero,
            size = Size(bodyW, bodyH),
            cornerRadius = corner
        )

        // 4. Outer 3D rim border
        drawRoundRect(
            color = if (highlightBorder) SelectedGlow else Color(0xFF6B5B3E),
            topLeft = Offset.Zero,
            size = Size(bodyW, bodyH),
            cornerRadius = corner,
            style = Stroke(width = if (highlightBorder) 2.dp.toPx() else 1.dp.toPx())
        )

        // 5. Inner gold filigree frame
        val inset = minDim * 0.16f
        drawRoundRect(
            color = Color(0xFFEBC341).copy(alpha = 0.65f),
            topLeft = Offset(inset, inset),
            size = Size(bodyW - inset * 2, bodyH - inset * 2),
            cornerRadius = CornerRadius(corner.x * 0.6f, corner.y * 0.6f),
            style = Stroke(width = 1.dp.toPx())
        )

        // 6. 3D Spherical Center Brass Medallion
        val pinR = minDim * 0.095f
        val centerPt = Offset(bodyW / 2f, bodyH / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFEF0),
                    Color(0xFFF5CC4D),
                    Color(0xFF78530D)
                ),
                center = Offset(centerPt.x - pinR * 0.3f, centerPt.y - pinR * 0.3f),
                radius = pinR * 1.3f
            ),
            radius = pinR,
            center = centerPt
        )
    }
}

