package com.david7ce.calistenia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.model.Muscle

/** Which side of the body a drawing shows. */
enum class BodyView { Front, Back }

/**
 * One drawn zone of the body. [muscle] is null for the parts that are only there so the figure reads as
 * a person (head, neck, hands, feet, shins): they are drawn but never selectable.
 *
 * Points are in a 100 x 200 box and describe the viewer's right half only (x >= 50); the left half is the
 * mirror image, so every muscle has two zones that mean the same thing and the figure is symmetric for
 * free.
 */
internal class BodyRegion(val muscle: Muscle?, val points: List<Pair<Float, Float>>)

private fun region(muscle: Muscle?, vararg xy: Float): BodyRegion =
    BodyRegion(muscle, List(xy.size / 2) { xy[it * 2] to xy[it * 2 + 1] })

private const val BOX_W = 100f
private const val BOX_H = 200f

/** Neutral parts shared by both views. Drawn first, so muscles sit on top of them. */
private val commonDecor = listOf(
    region(null, 50f, 5f, 55f, 6f, 59f, 11f, 59f, 18f, 55f, 24f, 50f, 26f), // head
    region(null, 78f, 119f, 87f, 118f, 88f, 128f, 80f, 130f), // hand
    region(null, 52f, 184f, 62f, 184f, 64f, 192f, 52f, 192f), // foot
)

private val frontRegions: List<BodyRegion> = commonDecor + listOf(
    region(null, 50f, 26f, 55f, 27f, 57f, 34f, 50f, 34f), // neck
    region(null, 50f, 92f, 64f, 94f, 62f, 104f, 50f, 106f), // pelvis
    region(null, 53f, 152f, 64f, 152f, 63f, 180f, 57f, 184f, 53f, 180f), // shin
    region(Muscle.Chest, 51f, 38f, 63f, 37f, 67f, 42f, 66f, 54f, 58f, 58f, 51f, 56f),
    region(Muscle.Shoulders, 67f, 36f, 74f, 34f, 80f, 40f, 80f, 52f, 75f, 54f, 69f, 47f),
    region(Muscle.Biceps, 75f, 55f, 82f, 54f, 84f, 60f, 83f, 80f, 77f, 81f, 74f, 68f),
    region(Muscle.Forearms, 77f, 83f, 83f, 82f, 86f, 92f, 88f, 115f, 83f, 118f, 78f, 104f),
    region(Muscle.Abs, 51f, 59f, 58f, 60f, 59f, 72f, 58f, 88f, 51f, 90f),
    region(Muscle.Obliques, 60f, 60f, 66f, 57f, 66f, 76f, 63f, 92f, 60f, 88f, 60f, 72f),
    region(Muscle.Quads, 51f, 96f, 64f, 94f, 66f, 100f, 64f, 130f, 60f, 148f, 52f, 148f),
)

private val backRegions: List<BodyRegion> = commonDecor + listOf(
    region(null, 51f, 28f, 55f, 29f, 66f, 38f, 66f, 45f, 52f, 46f), // trapezius
    region(Muscle.Shoulders, 67f, 36f, 74f, 34f, 80f, 40f, 80f, 52f, 75f, 54f, 69f, 47f),
    region(Muscle.Lats, 52f, 47f, 66f, 46f, 68f, 54f, 61f, 73f, 52f, 76f),
    region(Muscle.Triceps, 75f, 55f, 82f, 54f, 84f, 60f, 83f, 80f, 77f, 81f, 74f, 68f),
    region(Muscle.Forearms, 77f, 83f, 83f, 82f, 86f, 92f, 88f, 115f, 83f, 118f, 78f, 104f),
    region(Muscle.LowerBack, 51f, 78f, 59f, 76f, 60f, 90f, 51f, 92f),
    region(Muscle.Glutes, 51f, 93f, 65f, 91f, 67f, 100f, 64f, 112f, 51f, 112f),
    region(Muscle.Hamstrings, 52f, 114f, 64f, 114f, 66f, 122f, 63f, 148f, 53f, 148f),
    region(Muscle.Calves, 53f, 152f, 64f, 152f, 65f, 164f, 62f, 180f, 56f, 180f, 53f, 166f),
)

internal fun regionsOf(view: BodyView): List<BodyRegion> = when (view) {
    BodyView.Front -> frontRegions
    BodyView.Back -> backRegions
}

/** The mirror of a right-half point list, for the viewer's left half. */
private fun mirrored(points: List<Pair<Float, Float>>) = points.map { (BOX_W - it.first) to it.second }

private fun contains(polygon: List<Pair<Float, Float>>, x: Float, y: Float): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val (xi, yi) = polygon[i]
        val (xj, yj) = polygon[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

/**
 * The muscle under a point of the 100 x 200 drawing box, or null on the background or on a decorative
 * part. Pure on purpose: it is the one rule that decides what a tap means, so it is unit-tested instead
 * of being buried in a gesture handler.
 */
fun muscleAt(view: BodyView, x: Float, y: Float): Muscle? =
    regionsOf(view).asReversed().firstOrNull { region ->
        region.muscle != null &&
            (contains(region.points, x, y) || contains(mirrored(region.points), x, y))
    }?.muscle

/** The muscles a drawing can select; what the chips must cover for the map to be reachable by touch alone. */
fun musclesOnMap(view: BodyView): Set<Muscle> = regionsOf(view).mapNotNull { it.muscle }.toSet()

private fun pathOf(points: List<Pair<Float, Float>>, scaleX: Float, scaleY: Float): Path = Path().apply {
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) moveTo(x * scaleX, y * scaleY) else lineTo(x * scaleX, y * scaleY)
    }
    close()
}

/**
 * One body, front or back, drawn with Compose paths (no raster asset). A selected muscle takes the
 * primary tint, the rest use the surface variant. Several views share one [selected] set, so a muscle
 * picked on the front stays lit on the back.
 */
@Composable
fun BodyDrawing(
    view: BodyView,
    selected: Set<Muscle>,
    onToggle: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.surfaceVariant
    val edge = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = modifier
            .aspectRatio(BOX_W / BOX_H)
            .pointerInput(view) {
                detectTapGestures { tap ->
                    muscleAt(view, tap.x / size.width * BOX_W, tap.y / size.height * BOX_H)?.let(onToggle)
                }
            },
    ) {
        val sx = size.width / BOX_W
        val sy = size.height / BOX_H
        val stroke = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
        regionsOf(view).forEach { region ->
            val fill: Color = when {
                region.muscle == null -> off.copy(alpha = 0.5f)
                region.muscle in selected -> on
                else -> off
            }
            listOf(region.points, mirrored(region.points)).forEach { half ->
                val path = pathOf(half, sx, sy)
                drawPath(path, fill)
                drawPath(path, edge.copy(alpha = 0.6f), style = stroke)
            }
        }
    }
}

/** Front and back side by side, sharing one selection. */
@Composable
fun MuscleMap(
    selected: Set<Muscle>,
    onToggle: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        listOf(BodyView.Front to strings.bodyFront, BodyView.Back to strings.bodyBack).forEach { (view, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                BodyDrawing(view, selected, onToggle, Modifier.fillMaxWidth())
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
