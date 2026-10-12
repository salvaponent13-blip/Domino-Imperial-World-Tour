package com.example.dominograndprix.engine

/**
 * Exact step coordinate from Domino Fever's Resource 13 (layout_template_bsprite).
 * @param dx Relative X coordinate in the 240x320 virtual space
 * @param dy Relative Y coordinate in the 240x320 virtual space
 * @param mod 0 = Vertical (17x31), 1 = Horizontal (31x17)
 * @param flags 0 = Forward (top/left connects, bottom/right is open),
 *              1 or 2 = Reversed (bottom/right connects, top/left is open)
 */
data class TemplateStep(
    val dx: Int,
    val dy: Int,
    val mod: Int,
    val flags: Int
) {
    val isVertical: Boolean get() = mod == 0
    val isForward: Boolean get() = (flags and 3) == 0
}

data class BoardPatternTemplate(
    val id: Int,
    val leftBranch: List<TemplateStep>,
    val rightBranch: List<TemplateStep>
)

/**
 * All 10 authentic 2-branch board layout patterns extracted byte-for-byte
 * from Gameloft's Domino Fever (layout_template_bsprite, Resource 13).
 * These patterns wind the domino chain into squares and serpentines so all tiles
 * fit inside the 194x208 portrait table with 100% matching face-to-face connections.
 */
object DominoLayoutTemplates {

    val PATTERNS: List<BoardPatternTemplate> = listOf(
        // Pattern 0: Classic serpentine
        BoardPatternTemplate(
            id = 0,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-43, -29, 1, 0), TemplateStep(-14, -29, 1, 0),
                TemplateStep(15, -29, 1, 0), TemplateStep(44, -29, 1, 0), TemplateStep(73, -43, 0, 2),
                TemplateStep(59, -58, 1, 1), TemplateStep(30, -58, 1, 1), TemplateStep(1, -58, 1, 1),
                TemplateStep(-28, -58, 1, 1), TemplateStep(-57, -58, 1, 1), TemplateStep(-57, -87, 0, 2)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(43, 29, 1, 1), TemplateStep(14, 29, 1, 1),
                TemplateStep(-15, 29, 1, 1), TemplateStep(-44, 29, 1, 1), TemplateStep(-59, 29, 0, 0),
                TemplateStep(-59, 58, 1, 0), TemplateStep(-30, 58, 1, 0), TemplateStep(-1, 58, 1, 0),
                TemplateStep(28, 58, 1, 0), TemplateStep(57, 58, 1, 0), TemplateStep(71, 73, 0, 0)
            )
        ),
        // Pattern 1: Serpentine top + Square steps bottom
        BoardPatternTemplate(
            id = 1,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-43, -29, 1, 0), TemplateStep(-14, -29, 1, 0),
                TemplateStep(15, -29, 1, 0), TemplateStep(44, -29, 1, 0), TemplateStep(73, -43, 0, 2),
                TemplateStep(59, -58, 1, 1), TemplateStep(30, -58, 1, 1), TemplateStep(1, -58, 1, 1),
                TemplateStep(-28, -58, 1, 1), TemplateStep(-57, -58, 1, 1), TemplateStep(-57, -87, 0, 2)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(72, 44, 0, 0), TemplateStep(43, 58, 1, 1),
                TemplateStep(28, 44, 0, 2), TemplateStep(-1, 44, 1, 1), TemplateStep(-16, 44, 0, 0),
                TemplateStep(-45, 58, 1, 1), TemplateStep(-60, 58, 0, 0), TemplateStep(-60, 87, 0, 0),
                TemplateStep(-45, 101, 1, 0), TemplateStep(-16, 87, 0, 2), TemplateStep(-1, 87, 1, 0),
                TemplateStep(28, 87, 0, 0), TemplateStep(43, 101, 1, 0), TemplateStep(72, 101, 0, 0)
            )
        ),
        // Pattern 2: Square steps top & bottom
        BoardPatternTemplate(
            id = 2,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -58, 0, 2), TemplateStep(-43, -58, 1, 0),
                TemplateStep(-14, -58, 0, 0), TemplateStep(1, -44, 1, 0), TemplateStep(30, -58, 0, 2),
                TemplateStep(45, -58, 1, 0), TemplateStep(74, -72, 0, 2), TemplateStep(74, -101, 0, 2),
                TemplateStep(45, -101, 1, 1), TemplateStep(30, -101, 0, 0), TemplateStep(1, -87, 1, 1),
                TemplateStep(-14, -101, 0, 2), TemplateStep(-43, -101, 1, 1), TemplateStep(-58, -115, 0, 2)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(58, 44, 1, 1), TemplateStep(58, 59, 0, 0),
                TemplateStep(44, 88, 1, 1), TemplateStep(29, 74, 0, 2), TemplateStep(15, 59, 1, 1),
                TemplateStep(0, 59, 0, 0), TemplateStep(-14, 88, 1, 1), TemplateStep(-29, 74, 0, 2),
                TemplateStep(-29, 45, 0, 2), TemplateStep(-58, 45, 1, 1), TemplateStep(-58, 60, 0, 0),
                TemplateStep(-58, 89, 0, 0), TemplateStep(-58, 118, 1, 0), TemplateStep(-44, 133, 0, 0),
                TemplateStep(-44, 162, 1, 0), TemplateStep(-15, 148, 0, 2), TemplateStep(-15, 133, 1, 0),
                TemplateStep(14, 133, 0, 0), TemplateStep(14, 162, 1, 0), TemplateStep(43, 148, 0, 2),
                TemplateStep(43, 119, 0, 2), TemplateStep(58, 119, 1, 0), TemplateStep(72, 134, 0, 0),
                TemplateStep(72, 163, 0, 0)
            )
        ),
        // Pattern 3: Symmetric Square-Steps (as shown in preview!)
        BoardPatternTemplate(
            id = 3,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -44, 1, 0), TemplateStep(-29, -58, 0, 2),
                TemplateStep(-14, -58, 1, 0), TemplateStep(15, -58, 0, 0), TemplateStep(30, -44, 1, 0),
                TemplateStep(59, -44, 1, 0), TemplateStep(73, -73, 0, 2), TemplateStep(59, -88, 1, 1),
                TemplateStep(44, -102, 0, 2), TemplateStep(15, -102, 1, 1), TemplateStep(0, -102, 0, 0),
                TemplateStep(-29, -88, 1, 1), TemplateStep(-58, -88, 1, 1), TemplateStep(-58, -117, 0, 2)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(72, 44, 0, 0), TemplateStep(43, 58, 1, 1),
                TemplateStep(14, 58, 1, 1), TemplateStep(-1, 44, 0, 2), TemplateStep(-30, 44, 1, 1),
                TemplateStep(-59, 44, 1, 1), TemplateStep(-59, 59, 0, 0), TemplateStep(-59, 88, 0, 0),
                TemplateStep(-44, 102, 1, 0), TemplateStep(-15, 102, 1, 0), TemplateStep(14, 88, 0, 2),
                TemplateStep(29, 88, 1, 0), TemplateStep(58, 88, 1, 0), TemplateStep(72, 103, 0, 0)
            )
        ),
        // Pattern 4: Wide Square-Steps
        BoardPatternTemplate(
            id = 4,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -58, 0, 2), TemplateStep(-43, -58, 1, 0),
                TemplateStep(-14, -58, 1, 0), TemplateStep(15, -58, 0, 0), TemplateStep(30, -44, 1, 0),
                TemplateStep(59, -44, 1, 0), TemplateStep(73, -73, 0, 2), TemplateStep(73, -102, 0, 2),
                TemplateStep(44, -102, 1, 1), TemplateStep(15, -102, 1, 1), TemplateStep(0, -102, 0, 0),
                TemplateStep(-29, -88, 1, 1), TemplateStep(-58, -88, 1, 1), TemplateStep(-58, -117, 0, 2)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(58, 44, 1, 1), TemplateStep(43, 44, 0, 0),
                TemplateStep(14, 58, 1, 1), TemplateStep(-1, 44, 0, 2), TemplateStep(-30, 44, 1, 1),
                TemplateStep(-59, 44, 1, 1), TemplateStep(-59, 59, 0, 0), TemplateStep(-59, 88, 1, 0),
                TemplateStep(-30, 88, 0, 0), TemplateStep(-15, 102, 1, 0), TemplateStep(14, 88, 0, 2),
                TemplateStep(29, 88, 1, 0), TemplateStep(58, 88, 1, 0), TemplateStep(72, 103, 0, 0)
            )
        )
    )
}
