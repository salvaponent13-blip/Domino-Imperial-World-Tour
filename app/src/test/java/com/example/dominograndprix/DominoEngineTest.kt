package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoBonusType
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.*
import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot

class DominoEngineTest {

    data class HalfSquare(val centerX: Double, val centerY: Double, val pip: Int)

    private fun getHalves(placed: BoardPlacedTile): Pair<HalfSquare, HalfSquare> {
        val (h1, h2) = if (placed.isVertical) {
            Pair(
                HalfSquare(placed.virtX.toDouble(), (placed.virtY - 14).toDouble(), placed.pipLeft),
                HalfSquare(placed.virtX.toDouble(), (placed.virtY + 14).toDouble(), placed.pipRight)
            )
        } else {
            Pair(
                HalfSquare((placed.virtX - 14).toDouble(), placed.virtY.toDouble(), placed.pipLeft),
                HalfSquare((placed.virtX + 14).toDouble(), placed.virtY.toDouble(), placed.pipRight)
            )
        }
        return Pair(h1, h2)
    }

    @Test
    fun testAllEightModesAndGeometricPhysicalCoupling() {
        for (mode in GameMode.values()) {
            if (mode == GameMode.ALL_FIVES) continue
            for (patternIdx in 0..4) {
                val engine = DominoEngine.allAi(gameMode = mode, targetScore = 50, patternIndex = patternIdx)
                var turns = 0
                while (!engine.isRoundOver && turns < 100) {
                    turns++
                    engine.executeAIMove()

                    // Verify 1: No two tiles on the board overlap!
                    val chain = engine.boardChain
                    for (i in chain.indices) {
                        val a = chain[i]
                        val aw = if (a.isVertical) 28 else 56
                        val ah = if (a.isVertical) 56 else 28
                        val aLeft = a.virtX - aw / 2
                        val aTop = a.virtY - ah / 2
                        for (j in i + 1 until chain.size) {
                            val b = chain[j]
                            val bw = if (b.isVertical) 28 else 56
                            val bh = if (b.isVertical) 56 else 28
                            val bLeft = b.virtX - bw / 2
                            val bTop = b.virtY - bh / 2
                            val overlapX = aLeft < bLeft + bw && aLeft + aw > bLeft
                            val overlapY = aTop < bTop + bh && aTop + ah > bTop
                            assertFalse(
                                "Mode $mode: Tiles $i (${a.virtX},${a.virtY},${a.branchSide}) and $j (${b.virtX},${b.virtY},${b.branchSide}) must not overlap in pattern $patternIdx",
                                overlapX && overlapY
                            )
                        }
                    }

                    // Verify 2: Every adjacent pair in boardChain physically touches at matching pip halves!
                    if (mode != GameMode.MATADOR) {
                        for (i in 0 until chain.size - 1) {
                            val (a1, a2) = getHalves(chain[i])
                            val (b1, b2) = getHalves(chain[i + 1])
                            val pairs = listOf(
                                Triple(a1, b1, hypot(a1.centerX - b1.centerX, a1.centerY - b1.centerY)),
                                Triple(a1, b2, hypot(a1.centerX - b2.centerX, a1.centerY - b2.centerY)),
                                Triple(a2, b1, hypot(a2.centerX - b1.centerX, a2.centerY - b1.centerY)),
                                Triple(a2, b2, hypot(a2.centerX - b2.centerX, a2.centerY - b2.centerY))
                            )
                            val closest = pairs.minByOrNull { it.third }!!
                            assertTrue(
                                "Adjacent tiles $i and ${i + 1} must physically touch (dist=${closest.third})",
                                closest.third <= 35.0
                            )
                            val minDist = pairs.minOf { it.third }
                            val tiePairs = pairs.filter { kotlin.math.abs(it.third - minDist) < 0.1 }
                            val bestPair = tiePairs.find { it.first.pip == it.second.pip } ?: tiePairs.first()
                            assertTrue(
                                "Adjacent tiles $i and ${i + 1} must physically touch (dist=${bestPair.third})",
                                bestPair.third <= 35.0
                            )
                            assertEquals(
                                "Physically touching halves between tile $i (${chain[i].tile}) and ${i + 1} (${chain[i + 1].tile}) must have identical pip count in pattern $patternIdx",
                                bestPair.first.pip,
                                bestPair.second.pip
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun renderVisualBoardProofFromKotlinEngine() {
        val engine = DominoEngine.allAi(gameMode = GameMode.DRAW, targetScore = 100, patternIndex = 3)
        // Play until at least 16 tiles are on the board or round ends
        var steps = 0
        while (!engine.isRoundOver && steps < 60) {
            steps++
            engine.executeAIMove()
        }

        val scale = 4
        val img = BufferedImage(240 * scale, 320 * scale, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Felt background with subtle radial vignette
        g.paint = java.awt.RadialGradientPaint(
            (120 * scale).toFloat(), (160 * scale).toFloat(), (180 * scale).toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(Color(24, 92, 53), Color(9, 44, 24))
        )
        g.fillRect(0, 0, 240 * scale, 320 * scale)

        // Draw every placed tile from Kotlin engine with full 3D shaders
        for (placed in engine.boardChain) {
            val w = (if (placed.isVertical) 17 else 31) * scale
            val h = (if (placed.isVertical) 31 else 17) * scale
            val x = placed.virtX * scale
            val y = placed.virtY * scale
            val minDim = minOf(w, h)
            val corner = (minDim * 0.28f).toInt()
            val extrudeX = 5
            val extrudeY = 6
            val bodyW = w - extrudeX
            val bodyH = h - extrudeY

            // 1. Soft drop shadow
            g.color = Color(0, 0, 0, 95)
            g.fillRoundRect(x + extrudeX + 2, y + extrudeY + 3, bodyW, bodyH, corner, corner)
            g.color = Color(0, 0, 0, 145)
            g.fillRoundRect(x + extrudeX, y + extrudeY + 1, bodyW, bodyH, corner, corner)

            // 2. 3D Extruded Bicapa Lower Ebony Layer
            g.paint = java.awt.LinearGradientPaint(
                x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(43, 41, 39), Color(10, 9, 8))
            )
            g.fillRoundRect(x + 4, y + 5, bodyW, bodyH, corner, corner)

            // 3. 3D Extruded Upper Shaded Ivory Wall
            g.paint = java.awt.LinearGradientPaint(
                x.toFloat(), y.toFloat(), (x + bodyW).toFloat(), (y + bodyH).toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(201, 190, 163), Color(140, 126, 98))
            )
            g.fillRoundRect(x + 2, y + 2, bodyW, bodyH, corner, corner)

            // 4. Outer 3D Chamfered Bevel Slope
            g.paint = java.awt.LinearGradientPaint(
                x.toFloat(), y.toFloat(), (x + bodyW).toFloat(), (y + bodyH).toFloat(),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                arrayOf(
                    Color(255, 255, 255),
                    Color(243, 236, 216),
                    Color(194, 181, 152),
                    Color(158, 143, 112)
                )
            )
            g.fillRoundRect(x, y, bodyW, bodyH, corner, corner)

            // 5. Raised Inner Acrylic Ivory Plateau
            val bevel = 4
            val innerCorner = maxOf(4, corner - 3)
            g.paint = java.awt.LinearGradientPaint(
                x.toFloat(), y.toFloat(), (x + bodyW).toFloat(), (y + bodyH).toFloat(),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                arrayOf(
                    Color(255, 254, 249),
                    Color(248, 243, 227),
                    Color(237, 228, 204),
                    Color(221, 208, 179)
                )
            )
            g.fillRoundRect(x + bevel, y + bevel, bodyW - bevel * 2, bodyH - bevel * 2, innerCorner, innerCorner)

            // 6. Specular Acrylic Gloss Ridge
            g.paint = java.awt.LinearGradientPaint(
                x.toFloat(), y.toFloat(), (x + bodyW).toFloat(), (y + bodyH).toFloat(),
                floatArrayOf(0f, 0.4f, 0.7f, 1f),
                arrayOf(
                    Color(255, 255, 255, 235),
                    Color(255, 255, 255, 65),
                    Color(255, 255, 255, 0),
                    Color(138, 123, 94, 90)
                )
            )
            g.stroke = BasicStroke(1.6f)
            g.drawRoundRect(x + bevel, y + bevel, bodyW - bevel * 2, bodyH - bevel * 2, innerCorner, innerCorner)

            // 7. Outer contour
            g.color = Color(45, 37, 28)
            g.stroke = BasicStroke(1.5f)
            g.drawRoundRect(x, y, bodyW, bodyH, corner, corner)

            // 8. 3D Engraved V-Groove Divider & Spherical Brass Spinner Pin
            val cx = x + bodyW / 2
            val cy = y + bodyH / 2
            if (placed.isVertical) {
                val pad = (bodyW * 0.11f).toInt()
                g.color = Color(156, 142, 115)
                g.stroke = BasicStroke(1.5f)
                g.drawLine(x + pad, cy - 2, x + bodyW - pad, cy - 2)
                g.color = Color(255, 255, 255)
                g.drawLine(x + pad, cy + 2, x + bodyW - pad, cy + 2)
                g.color = Color(46, 37, 27)
                g.stroke = BasicStroke(2.2f)
                g.drawLine(x + pad, cy, x + bodyW - pad, cy)
            } else {
                val pad = (bodyH * 0.11f).toInt()
                g.color = Color(156, 142, 115)
                g.stroke = BasicStroke(1.5f)
                g.drawLine(cx - 2, y + pad, cx - 2, y + bodyH - pad)
                g.color = Color(255, 255, 255)
                g.drawLine(cx + 2, y + pad, cx + 2, y + bodyH - pad)
                g.color = Color(46, 37, 27)
                g.stroke = BasicStroke(2.2f)
                g.drawLine(cx, y + pad, cx, y + bodyH - pad)
            }

            // Spherical 3D Brass Spinner Pin
            val pinR = 5
            g.color = Color(36, 27, 12, 110)
            g.fillOval(cx - pinR + 1, cy - pinR + 2, pinR * 2 + 1, pinR * 2 + 1)
            g.color = Color(71, 51, 11)
            g.fillOval(cx - pinR - 1, cy - pinR - 1, (pinR + 1) * 2, (pinR + 1) * 2)
            g.paint = java.awt.RadialGradientPaint(
                (cx - 2).toFloat(), (cy - 2).toFloat(), (pinR * 1.4f),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                arrayOf(
                    Color(255, 254, 242),
                    Color(252, 224, 104),
                    Color(214, 159, 38),
                    Color(110, 75, 10)
                )
            )
            g.fillOval(cx - pinR, cy - pinR, pinR * 2, pinR * 2)

            // 9. 3D Concave Drilled Pips
            if (placed.isVertical) {
                drawAwtHalfPips(g, x, y, bodyW, bodyH / 2, placed.pipLeft, true)
                drawAwtHalfPips(g, x, y + bodyH / 2, bodyW, bodyH / 2, placed.pipRight, true)
            } else {
                drawAwtHalfPips(g, x, y, bodyW / 2, bodyH, placed.pipLeft, false)
                drawAwtHalfPips(g, x + bodyW / 2, y, bodyW / 2, bodyH, placed.pipRight, false)
            }
        }

        g.dispose()
        val outFile = File("build/kotlin_engine_board_proof.png")
        outFile.parentFile?.mkdirs()
        ImageIO.write(img, "PNG", outFile)
        println("Saved visual proof from Kotlin engine to: ${outFile.absolutePath}")
        assertTrue(outFile.exists())
    }

    private fun drawAwtHalfPips(
        g: java.awt.Graphics2D,
        hx: Int,
        hy: Int,
        hw: Int,
        hh: Int,
        count: Int,
        isVert: Boolean
    ) {
        if (count <= 0) return
        val padX = (hw * 0.27).toInt()
        val padY = (hh * 0.27).toInt()
        val left = hx + padX
        val right = hx + hw - padX
        val top = hy + padY
        val bottom = hy + hh - padY
        val midX = (left + right) / 2
        val midY = (top + bottom) / 2

        val pts = when (count) {
            1 -> listOf(Pair(midX, midY))
            2 -> if (isVert) listOf(Pair(left, top), Pair(right, bottom)) else listOf(Pair(right, top), Pair(left, bottom))
            3 -> if (isVert) listOf(Pair(left, top), Pair(midX, midY), Pair(right, bottom)) else listOf(Pair(right, top), Pair(midX, midY), Pair(left, bottom))
            4 -> listOf(Pair(left, top), Pair(right, top), Pair(left, bottom), Pair(right, bottom))
            5 -> listOf(Pair(left, top), Pair(right, top), Pair(midX, midY), Pair(left, bottom), Pair(right, bottom))
            6 -> if (isVert) {
                listOf(Pair(left, top), Pair(right, top), Pair(left, midY), Pair(right, midY), Pair(left, bottom), Pair(right, bottom))
            } else {
                listOf(Pair(left, top), Pair(midX, top), Pair(right, top), Pair(left, bottom), Pair(midX, bottom), Pair(right, bottom))
            }
            else -> emptyList()
        }

        val r = 6
        for ((px, py) in pts) {
            // Bottom-right illuminated cavity rim
            g.color = Color(255, 255, 255)
            g.fillOval(px - r + 1, py - r + 1, r * 2 + 1, r * 2 + 1)
            // Top-left recessed cavity shadow
            g.color = Color(140, 126, 101)
            g.fillOval(px - r - 1, py - r - 1, r * 2 + 1, r * 2 + 1)
            // Concave glossy black enamel pit
            g.paint = java.awt.RadialGradientPaint(
                (px + 2).toFloat(), (py + 2).toFloat(), (r * 1.2f),
                floatArrayOf(0f, 0.55f, 1f),
                arrayOf(Color(58, 58, 58), Color(20, 20, 20), Color(3, 3, 3))
            )
            g.fillOval(px - r, py - r, r * 2, r * 2)
            // Tiny crisp specular glint
            g.color = Color(255, 255, 255, 160)
            g.fillOval(px - r / 2, py - r / 2, 3, 3)
        }
    }

    @Test
    fun testAllFivesNoOverlaps() {
        for (seed in 1L..100L) {
            val engine = DominoEngine(
                gameMode = GameMode.ALL_FIVES,
                targetScore = 100,
                random = kotlin.random.Random(seed),
                playerNames = listOf("Carlos", "Mateo", "Rodrigo", "Sofía"),
                humanPlayerIndices = emptySet()
            )
            engine.startNewRound()
            var turns = 0
            while (!engine.isRoundOver && turns < 100) {
                turns++
                val curr = engine.currentPlayerIndex
                val p = engine.players[curr]
                engine.executeAIMove()
                if (seed == 1L) {
                    val last = engine.boardChain.lastOrNull()
                    println("Turn $turns: ${engine.roundLog} -> placed: ${last?.tile} at (${last?.virtX}, ${last?.virtY}) side=${last?.branchSide} isVert=${last?.isVertical}")
                }

                val chain = engine.boardChain
                for (i in chain.indices) {
                    val a = chain[i]
                    // Convencion real del layout: virtX/virtY es el CENTRO y las fichas miden 56x28.
                    val aw = if (a.isVertical) 28 else 56
                    val ah = if (a.isVertical) 56 else 28
                    val aLeft = a.virtX - aw / 2
                    val aTop = a.virtY - ah / 2
                    for (j in i + 1 until chain.size) {
                        val b = chain[j]
                        val bw = if (b.isVertical) 28 else 56
                        val bh = if (b.isVertical) 56 else 28
                        val bLeft = b.virtX - bw / 2
                        val bTop = b.virtY - bh / 2
                        val overlapX = aLeft < bLeft + bw && aLeft + aw > bLeft
                        val overlapY = aTop < bTop + bh && aTop + ah > bTop
                        if (overlapX && overlapY) {
                            println("FAIL at seed $seed, turn $turns:")
                            for (k in chain.indices) {
                                println("  Tile $k: ${chain[k].tile} at (${chain[k].virtX}, ${chain[k].virtY}) side=${chain[k].branchSide} isVert=${chain[k].isVertical}")
                            }
                            fail("Seed $seed: Tile $i (${a.tile}, ${a.branchSide}) and $j (${b.tile}, ${b.branchSide}) overlap at (${a.virtX},${a.virtY}) vs (${b.virtX},${b.virtY})")
                        }
                    }
            }
        }
    }
}

    @Test
    fun testExactUserScenario() {
        val engine = DominoEngine(
            gameMode = GameMode.ALL_FIVES,
            targetScore = 50,
            playerNames = listOf("Tú", "Carlos 'El Rayo'", "Mateo", "Rodrigo"),
            humanPlayerIndices = setOf(0)
        )
        engine.startNewRound()
        // Carlos has [6|6], Mateo has [6|4], Rodrigo has [5|4] plus dead tiles
        val dead = com.example.dominograndprix.model.DominoTile(0, 0)
        engine.players[1] = engine.players[1].copy(hand = listOf(com.example.dominograndprix.model.DominoTile(6, 6), dead, dead))
        engine.players[2] = engine.players[2].copy(hand = listOf(com.example.dominograndprix.model.DominoTile(6, 4), dead, dead))
        engine.players[3] = engine.players[3].copy(hand = listOf(com.example.dominograndprix.model.DominoTile(5, 4), dead, dead))
        engine.currentPlayerIndex = 1

        val t1 = engine.players[1].hand.first()
        val s1 = engine.playTile(1, t1, com.example.dominograndprix.model.BoardSide.RIGHT)
        println("Turn 1 (Carlos): played $t1 -> success=$s1")

        val t2 = engine.players[2].hand.first()
        val s2 = engine.playTile(2, t2, com.example.dominograndprix.model.BoardSide.RIGHT)
        println("Turn 2 (Mateo): played $t2 -> success=$s2")

        val t3 = engine.players[3].hand.first()
        val s3 = engine.playTile(3, t3, com.example.dominograndprix.model.BoardSide.RIGHT)
        println("Turn 3 (Rodrigo): played $t3 -> success=$s3")

        for (i in engine.boardChain.indices) {
            val placed = engine.boardChain[i]
            println("BoardChain[$i]: ${placed.tile} at (${placed.virtX}, ${placed.virtY}) side=${placed.branchSide} isVert=${placed.isVertical}")
        }
    }

    @Test
    fun testBonusDominoDetection() {
        val engine = DominoEngine(
            gameMode = GameMode.BLOCK,
            targetScore = 100,
            playerCount = 2,
            playerNames = listOf("Tú", "Rival"),
            humanPlayerIndices = setOf(0)
        )

        // 1. Double Domino: closes with [6|6] when ends are 6 and 2 (only matches 6)
        engine.startNewRound()
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(6, 3), DominoTile(6, 6)))
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(3, 2), DominoTile(0, 0)))
        engine.currentPlayerIndex = 0

        assertTrue(engine.playTile(0, DominoTile(6, 3), BoardSide.RIGHT))
        assertTrue(engine.playTile(1, DominoTile(3, 2), BoardSide.RIGHT))
        assertEquals(6, engine.leftOpenPip)
        assertEquals(2, engine.rightOpenPip)

        assertTrue(engine.playTile(0, DominoTile(6, 6), BoardSide.LEFT))
        assertTrue(engine.isRoundOver)
        assertEquals(DominoBonusType.DOUBLE, engine.lastRoundBonusType)

        // 2. Capicúa: closes with [5|3] when ends are 5 and 3 (matches both open ends)
        engine.startNewRound()
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(5, 1), DominoTile(5, 3)))
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(1, 3), DominoTile(0, 0)))
        engine.currentPlayerIndex = 0

        assertTrue(engine.playTile(0, DominoTile(5, 1), BoardSide.RIGHT))
        assertTrue(engine.playTile(1, DominoTile(1, 3), BoardSide.RIGHT))
        assertEquals(5, engine.leftOpenPip)
        assertEquals(3, engine.rightOpenPip)

        assertTrue(engine.playTile(0, DominoTile(5, 3), BoardSide.LEFT))
        assertTrue(engine.isRoundOver)
        assertEquals(DominoBonusType.CAPICUA, engine.lastRoundBonusType)

        // 3. Double Capicúa: closes with [4|4] when ends are 4 and 4 (is double AND matches both ends)
        engine.startNewRound()
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(4, 2), DominoTile(4, 4)))
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(2, 4), DominoTile(0, 0)))
        engine.currentPlayerIndex = 0

        assertTrue(engine.playTile(0, DominoTile(4, 2), BoardSide.RIGHT))
        assertTrue(engine.playTile(1, DominoTile(2, 4), BoardSide.RIGHT))
        assertEquals(4, engine.leftOpenPip)
        assertEquals(4, engine.rightOpenPip)

        assertTrue(engine.playTile(0, DominoTile(4, 4), BoardSide.LEFT))
        assertTrue(engine.isRoundOver)
        assertEquals(DominoBonusType.DOUBLE_CAPICUA, engine.lastRoundBonusType)

        // 4. Standard Domino: closes with [6|1] when ends are 6 and 2
        engine.startNewRound()
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(6, 3), DominoTile(6, 1)))
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(3, 2), DominoTile(0, 0)))
        engine.currentPlayerIndex = 0

        assertTrue(engine.playTile(0, DominoTile(6, 3), BoardSide.RIGHT))
        assertTrue(engine.playTile(1, DominoTile(3, 2), BoardSide.RIGHT))
        assertEquals(6, engine.leftOpenPip)
        assertEquals(2, engine.rightOpenPip)

        assertTrue(engine.playTile(0, DominoTile(6, 1), BoardSide.LEFT))
        assertTrue(engine.isRoundOver)
        assertEquals(DominoBonusType.NONE, engine.lastRoundBonusType)
    }

    @Test
    fun testDeepSeekAuditCriticalFixes() {
        val engine = DominoEngine(
            gameMode = GameMode.DRAW,
            targetScore = 100,
            playerCount = 2,
            playerNames = listOf("Tú", "Rival"),
            humanPlayerIndices = setOf(0)
        )
        engine.startNewRound()

        // 1. Out of turn play/draw/pass is strictly rejected
        engine.currentPlayerIndex = 0
        assertFalse("No puede jugar fuera de turno", engine.playTile(1, DominoTile(6, 6), BoardSide.LEFT))
        assertNull("No puede robar fuera de turno", engine.drawTile(1, 0))
        val initialPasses = engine.consecutivePasses
        engine.passTurn(1)
        assertEquals("No puede pasar fuera de turno", initialPasses, engine.consecutivePasses)

        // 2. Cannot draw twice in the same turn
        engine.currentPlayerIndex = 0
        val firstDraw = engine.drawTile(0, 0)
        assertNotNull("Primer robo debe permitirse", firstDraw)
        assertTrue(engine.hasDrawnThisTurn)
        val secondDraw = engine.drawTile(0, 0)
        assertNull("Segundo robo consecutivo en el mismo turno debe rechazarse", secondDraw)

        // 3. Later double becomes spinner in ALL_FIVES
        val engineFives = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 100, playerCount = 2)
        engineFives.startNewRound()
        engineFives.players[0] = engineFives.players[0].copy(hand = listOf(DominoTile(5, 4), DominoTile(4, 4)))
        engineFives.players[1] = engineFives.players[1].copy(hand = listOf(DominoTile(5, 3), DominoTile(0, 0)))
        engineFives.currentPlayerIndex = 0

        // Asiento 0 abre con [5|4] (NO es doble)
        assertTrue(engineFives.playTile(0, DominoTile(5, 4), BoardSide.RIGHT))
        assertNull("Ficha normal no activa el spinner", engineFives.spinnerTile)

        // Asiento 1 juega [5|3]
        assertTrue(engineFives.playTile(1, DominoTile(5, 3), BoardSide.LEFT))
        assertNull(engineFives.spinnerTile)

        // Asiento 0 juega [4|4] (primer doble que entra en la mano): debe convertirse en spinner
        assertTrue(engineFives.playTile(0, DominoTile(4, 4), BoardSide.RIGHT))
        assertEquals("El primer doble posterior debe convertirse en spinner", DominoTile(4, 4), engineFives.spinnerTile)

        // 4. Victory by points does not count as blocked round (tranca)
        val enginePoints = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 100)
        enginePoints.startNewRound()
        enginePoints.players[0] = enginePoints.players[0].copy(score = 90, hand = listOf(DominoTile(5, 5)))
        enginePoints.currentPlayerIndex = 0
        assertTrue(enginePoints.playTile(0, DominoTile(5, 5), BoardSide.RIGHT)) // +10 pts => 100
        assertTrue(enginePoints.isMatchOver)
        assertTrue(enginePoints.isRoundOver)
        assertFalse("Victoria por puntos no debe marcarse como mesa trancada", enginePoints.lastRoundWasBlocked)
        assertEquals("No debe haber trancas en el acumulador", 0, enginePoints.matchBlockedRounds)
    }
}

