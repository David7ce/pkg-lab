package com.david7ce.calistenia.ui.figure

/**
 * One line of a [Contact]'s drawing, in figure space. [width] is in figure units (the same unit as
 * the limbs, which are 0.05 thick) and [alpha] is applied to the contact colour. Pure data so the
 * difference between a floor and a bar is testable without a Compose harness.
 */
data class ContactStroke(
    val from: FigurePoint,
    val to: FigurePoint,
    val width: Float,
    val alpha: Float,
)

/** Overhang of the contact line past the body's box, as a fraction of that box's width. */
private const val OVERHANG = 0.08f

/** Half-height of the bar's end caps, above and below the bar line. */
private const val BAR_CAP_HALF_HEIGHT = 0.12f

/**
 * What to draw through the anchor (the origin) for [contact], spanning [minX]..[maxX] plus an
 * overhang. The floor is a thin, faint line; the bar is a thick, stronger line with a cap at each
 * end, so a hang reads as a bar and not as ground.
 */
fun contactStrokes(contact: Contact, minX: Float, maxX: Float): List<ContactStroke> {
    val overhang = (maxX - minX) * OVERHANG
    val left = minX - overhang
    val right = maxX + overhang
    return when (contact) {
        Contact.None -> emptyList()
        Contact.Ground -> listOf(
            ContactStroke(FigurePoint(left, 0f), FigurePoint(right, 0f), width = 0.02f, alpha = 0.4f),
        )
        Contact.Bar -> listOf(
            ContactStroke(FigurePoint(left, 0f), FigurePoint(right, 0f), width = 0.07f, alpha = 0.8f),
            ContactStroke(
                FigurePoint(left, -BAR_CAP_HALF_HEIGHT), FigurePoint(left, BAR_CAP_HALF_HEIGHT),
                width = 0.07f, alpha = 0.8f,
            ),
            ContactStroke(
                FigurePoint(right, -BAR_CAP_HALF_HEIGHT), FigurePoint(right, BAR_CAP_HALF_HEIGHT),
                width = 0.07f, alpha = 0.8f,
            ),
        )
    }
}
