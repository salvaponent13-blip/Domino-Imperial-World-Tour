package com.example.dominograndprix.engine

import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile

object DominoBoardLayout {
    const val HALF_SHORT = 14
    const val TILE_SHORT = 28
    const val HALF_LONG = 28
    const val TILE_LONG = 56

    // Horizontal threshold (13 * 14 = 182) before starting a vertical corner bridge
    const val TURN_LIMIT_X = 182
    // Vertical center-to-center distance between parallel serpentine rows (6 * 14 = 84)
    // Guarantees 56 units of green felt between normal tiles and 28 units between opposing vertical doubles
    const val ROW_SPACING_Y = 84

    /**
     * Computes exact mathematical coordinates and orientations for all domino tiles on the table.
     *
     * Every coordinate (virtX, virtY) and half-dimension (14 or 28) is an exact integer multiple of 14.
     * - First tile is placed at (0, 0).
     * - Right branch grows RIGHT, then makes a 2-tile vertical bridge DOWN (y += 84) and returns LEFT.
     * - Left branch grows LEFT, then makes a 2-tile vertical bridge UP (y -= 84) and returns RIGHT.
     * - Parallel rows never touch or overlap, even when two doubles sit directly across from each other.
     * - All doubles are placed crosswise (en cruz: perpendicular to chain direction).
     */
    fun layoutBoardTiles(
        firstTile: BoardPlacedTile?,
        leftBranch: List<BoardPlacedTile>,
        rightBranch: List<BoardPlacedTile>,
        upBranch: List<BoardPlacedTile> = emptyList(),
        downBranch: List<BoardPlacedTile> = emptyList()
    ): List<BoardPlacedTile> {
        if (firstTile == null) return emptyList()

        // 1. Place opening tile at center (0, 0)
        val isFirstDouble = firstTile.tile.isDouble
        val updatedFirst = firstTile.copy(
            virtX = 0,
            virtY = 0,
            isVertical = isFirstDouble,
            pipLeft = firstTile.tile.high,
            pipRight = firstTile.tile.low
        )

        // 2. Lay out RIGHT branch (moves RIGHT, turns DOWN by 84 units, returns LEFT)
        val rightLaidOut = mutableListOf<BoardPlacedTile>()
        var rX = if (isFirstDouble) HALF_SHORT else HALF_LONG
        var rY = 0
        var rDir = 1 // 1 for RIGHT, -1 for LEFT
        var rTurnStep = 0 // 0 = horizontal, 1 = 1st vertical corner, 2 = 2nd vertical bridge, 3 = horizontal exit
        var rCurrentOpenPip = updatedFirst.pipRight

        for (placed in rightBranch) {
            val isDbl = placed.tile.isDouble
            val matchPip = placed.connectedPip
                ?: if (placed.tile.high == rCurrentOpenPip) placed.tile.high else placed.tile.low
            val openPip = placed.openEndPip
                ?: if (placed.tile.high == rCurrentOpenPip) placed.tile.low else placed.tile.high

            if (rTurnStep == 0 && !isDbl) {
                if (rDir == 1 && rX >= TURN_LIMIT_X) {
                    rTurnStep = 1
                } else if (rDir == -1 && rX <= -TURN_LIMIT_X) {
                    rTurnStep = 1
                }
            }

            val updatedPlaced: BoardPlacedTile
            if (rTurnStep == 1) {
                // 1st vertical turn tile going DOWN
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (rDir == 1) rX + HALF_SHORT else rX - HALF_SHORT,
                    virtY = rY + HALF_SHORT, // spans [rY - 14, rY + 42]
                    pipLeft = matchPip,      // top half touches end of horizontal row
                    pipRight = openPip       // bottom half faces down
                )
                rTurnStep = 2
            } else if (rTurnStep == 2) {
                // 2nd vertical bridge tile going DOWN to create wide felt separation between rows
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (rDir == 1) rX + HALF_SHORT else rX - HALF_SHORT,
                    virtY = rY + 70,         // spans [rY + 42, rY + 98], bottom square centered at rY + 84
                    pipLeft = matchPip,      // top half touches 1st vertical turn tile at rY + 42
                    pipRight = openPip       // bottom half [rY + 70, rY + 98] faces new horizontal row
                )
                rY += ROW_SPACING_Y          // new row centered at rY + 84
                rDir = -rDir                 // reverse horizontal direction
                rTurnStep = 3
            } else {
                // Horizontal movement along row
                val placeCrosswiseDouble = isDbl && rTurnStep == 0
                rTurnStep = 0

                if (placeCrosswiseDouble) {
                    // Doble SIEMPRE en cruz (vertical en fila horizontal)
                    if (rDir == 1) {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = rX + HALF_SHORT,
                            virtY = rY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        rX += TILE_SHORT
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = rX - HALF_SHORT,
                            virtY = rY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        rX -= TILE_SHORT
                    }
                } else {
                    // Ficha normal horizontal
                    if (rDir == 1) {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = rX + HALF_LONG,
                            virtY = rY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        rX += TILE_LONG
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = rX - HALF_LONG,
                            virtY = rY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        rX -= TILE_LONG
                    }
                }
            }

            rCurrentOpenPip = openPip
            rightLaidOut.add(updatedPlaced)
        }

        // 3. Lay out LEFT branch (moves LEFT, turns UP by 84 units, returns RIGHT)
        val leftLaidOut = mutableListOf<BoardPlacedTile>()
        var lX = if (isFirstDouble) -HALF_SHORT else -HALF_LONG
        var lY = 0
        var lDir = -1 // -1 for LEFT, 1 for RIGHT
        var lTurnStep = 0
        var lCurrentOpenPip = updatedFirst.pipLeft

        for (placed in leftBranch) {
            val isDbl = placed.tile.isDouble
            val matchPip = placed.connectedPip
                ?: if (placed.tile.high == lCurrentOpenPip) placed.tile.high else placed.tile.low
            val openPip = placed.openEndPip
                ?: if (placed.tile.high == lCurrentOpenPip) placed.tile.low else placed.tile.high

            if (lTurnStep == 0 && !isDbl) {
                if (lDir == -1 && lX <= -TURN_LIMIT_X) {
                    lTurnStep = 1
                } else if (lDir == 1 && lX >= TURN_LIMIT_X) {
                    lTurnStep = 1
                }
            }

            val updatedPlaced: BoardPlacedTile
            if (lTurnStep == 1) {
                // 1st vertical turn tile going UP
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (lDir == -1) lX - HALF_SHORT else lX + HALF_SHORT,
                    virtY = lY - HALF_SHORT, // spans [lY - 42, lY + 14]
                    pipLeft = openPip,       // top half faces up
                    pipRight = matchPip      // bottom half touches end of horizontal row
                )
                lTurnStep = 2
            } else if (lTurnStep == 2) {
                // 2nd vertical bridge tile going UP to create wide felt separation between rows
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (lDir == -1) lX - HALF_SHORT else lX + HALF_SHORT,
                    virtY = lY - 70,         // spans [lY - 98, lY - 42], top square centered at lY - 84
                    pipLeft = openPip,       // top half [lY - 98, lY - 70] faces new horizontal row
                    pipRight = matchPip      // bottom half touches 1st vertical turn tile at lY - 42
                )
                lY -= ROW_SPACING_Y          // new row centered at lY - 84
                lDir = -lDir                 // reverse horizontal direction
                lTurnStep = 3
            } else {
                // Horizontal movement along row
                val placeCrosswiseDouble = isDbl && lTurnStep == 0
                lTurnStep = 0

                if (placeCrosswiseDouble) {
                    // Doble SIEMPRE en cruz (vertical en fila horizontal)
                    if (lDir == -1) {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = lX - HALF_SHORT,
                            virtY = lY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        lX -= TILE_SHORT
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = lX + HALF_SHORT,
                            virtY = lY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        lX += TILE_SHORT
                    }
                } else {
                    // Ficha normal horizontal
                    if (lDir == -1) {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = lX - HALF_LONG,
                            virtY = lY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        lX -= TILE_LONG
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = lX + HALF_LONG,
                            virtY = lY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        lX += TILE_LONG
                    }
                }
            }

            lCurrentOpenPip = openPip
            leftLaidOut.add(updatedPlaced)
        }

        // 4. Lay out UP branch (if present in 4-branch mode)
        val upLaidOut = mutableListOf<BoardPlacedTile>()
        if (upBranch.isNotEmpty()) {
            var uY = if (isFirstDouble) -HALF_LONG else -HALF_SHORT
            var uCurrentOpenPip = updatedFirst.pipLeft

            for (placed in upBranch) {
                val isDbl = placed.tile.isDouble
                val matchPip = placed.connectedPip
                    ?: if (placed.tile.high == uCurrentOpenPip) placed.tile.high else placed.tile.low
                val openPip = placed.openEndPip
                    ?: if (placed.tile.high == uCurrentOpenPip) placed.tile.low else placed.tile.high

                val updatedPlaced = if (isDbl) {
                    // Doble en cruz (horizontal en rama vertical)
                    placed.copy(
                        isVertical = false,
                        virtX = 0,
                        virtY = uY - HALF_SHORT,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { uY -= TILE_SHORT }
                } else {
                    placed.copy(
                        isVertical = true,
                        virtX = 0,
                        virtY = uY - HALF_LONG,
                        pipLeft = openPip,
                        pipRight = matchPip
                    ).also { uY -= TILE_LONG }
                }
                uCurrentOpenPip = openPip
                upLaidOut.add(updatedPlaced)
            }
        }

        // 5. Lay out DOWN branch (if present in 4-branch mode)
        val downLaidOut = mutableListOf<BoardPlacedTile>()
        if (downBranch.isNotEmpty()) {
            var dY = if (isFirstDouble) HALF_LONG else HALF_SHORT
            var dCurrentOpenPip = updatedFirst.pipRight

            for (placed in downBranch) {
                val isDbl = placed.tile.isDouble
                val matchPip = placed.connectedPip
                    ?: if (placed.tile.high == dCurrentOpenPip) placed.tile.high else placed.tile.low
                val openPip = placed.openEndPip
                    ?: if (placed.tile.high == dCurrentOpenPip) placed.tile.low else placed.tile.high

                val updatedPlaced = if (isDbl) {
                    // Doble en cruz (horizontal en rama vertical)
                    placed.copy(
                        isVertical = false,
                        virtX = 0,
                        virtY = dY + HALF_SHORT,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { dY += TILE_SHORT }
                } else {
                    placed.copy(
                        isVertical = true,
                        virtX = 0,
                        virtY = dY + HALF_LONG,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { dY += TILE_LONG }
                }
                dCurrentOpenPip = openPip
                downLaidOut.add(updatedPlaced)
            }
        }

        val result = mutableListOf<BoardPlacedTile>()
        result.addAll(leftLaidOut.asReversed())
        result.add(updatedFirst)
        result.addAll(rightLaidOut)
        result.addAll(upLaidOut)
        result.addAll(downLaidOut)
        return result
    }

    data class BoardBounds(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
        val totalCols: Float,
        val totalRows: Float
    )

    fun computeBounds(tiles: List<BoardPlacedTile>): BoardBounds {
        if (tiles.isEmpty()) {
            return BoardBounds(minX = -56f, maxX = 56f, minY = -42f, maxY = 42f, totalCols = 8f, totalRows = 6f)
        }
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (placed in tiles) {
            val halfW = if (placed.isVertical) HALF_SHORT.toFloat() else HALF_LONG.toFloat()
            val halfH = if (placed.isVertical) HALF_LONG.toFloat() else HALF_SHORT.toFloat()
            minX = minOf(minX, placed.virtX - halfW)
            maxX = maxOf(maxX, placed.virtX + halfW)
            minY = minOf(minY, placed.virtY - halfH)
            maxY = maxOf(maxY, placed.virtY + halfH)
        }

        // Snap to 14-unit cell boundaries with 1-cell safety margin (14 units)
        val snapMinX = kotlin.math.floor((minX - 14f) / 14f) * 14f
        val snapMaxX = kotlin.math.ceil((maxX + 14f) / 14f) * 14f
        val snapMinY = kotlin.math.floor((minY - 14f) / 14f) * 14f
        val snapMaxY = kotlin.math.ceil((maxY + 14f) / 14f) * 14f

        val totalCols = maxOf(8f, (snapMaxX - snapMinX) / 14f)
        val totalRows = maxOf(6f, (snapMaxY - snapMinY) / 14f)

        return BoardBounds(
            minX = snapMinX,
            maxX = snapMaxX,
            minY = snapMinY,
            maxY = snapMaxY,
            totalCols = totalCols,
            totalRows = totalRows
        )
    }

    fun getNextSlotPlacement(
        firstTile: BoardPlacedTile?,
        leftBranch: List<BoardPlacedTile>,
        rightBranch: List<BoardPlacedTile>,
        upBranch: List<BoardPlacedTile> = emptyList(),
        downBranch: List<BoardPlacedTile> = emptyList(),
        side: BoardSide,
        candidateTile: DominoTile?
    ): NextSlotPlacement {
        if (firstTile == null) {
            return NextSlotPlacement(virtX = 0, virtY = 0, isVertical = candidateTile?.isDouble == true, isForward = true)
        }
        val dummyTile = candidateTile ?: DominoTile(6, 0)
        val dummyPlaced = BoardPlacedTile(
            tile = dummyTile,
            pipLeft = dummyTile.high,
            pipRight = dummyTile.low,
            isDouble = dummyTile.isDouble,
            branchSide = side
        )
        val simulated = when (side) {
            BoardSide.LEFT -> layoutBoardTiles(firstTile, leftBranch + dummyPlaced, rightBranch, upBranch, downBranch)
            BoardSide.RIGHT -> layoutBoardTiles(firstTile, leftBranch, rightBranch + dummyPlaced, upBranch, downBranch)
            BoardSide.UP -> layoutBoardTiles(firstTile, leftBranch, rightBranch, upBranch + dummyPlaced, downBranch)
            BoardSide.DOWN -> layoutBoardTiles(firstTile, leftBranch, rightBranch, upBranch, downBranch + dummyPlaced)
        }
        val target = when (side) {
            BoardSide.LEFT -> simulated.firstOrNull { it.branchSide == BoardSide.LEFT } ?: simulated.first()
            BoardSide.RIGHT -> simulated.lastOrNull { it.branchSide == BoardSide.RIGHT } ?: simulated.last()
            BoardSide.UP -> simulated.lastOrNull { it.branchSide == BoardSide.UP } ?: simulated.last()
            BoardSide.DOWN -> simulated.lastOrNull { it.branchSide == BoardSide.DOWN } ?: simulated.last()
        }
        return NextSlotPlacement(
            virtX = target.virtX,
            virtY = target.virtY,
            isVertical = target.isVertical,
            isForward = true
        )
    }
}
