package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.david7ce.sleepcycles.logic.Rating

private fun ratingColor(rating: Rating, dark: Boolean): Color = when (rating) {
    Rating.Good -> if (dark) Color(0xFF7EE0A3) else Color(0xFF1E7B45)
    Rating.Ok -> if (dark) Color(0xFFF0CD7A) else Color(0xFF8A6200)
    Rating.Low -> if (dark) Color(0xFFF08A8A) else Color(0xFFB3261E)
}

/**
 * Smile, flat mouth or frown, drawn rather than emoji: colour emoji render badly on desktop JVM/Linux.
 * The shape (not just the colour) carries the meaning, and [description] is read by screen readers.
 */
@Composable
fun RatingFace(rating: Rating, description: String, dark: Boolean, modifier: Modifier = Modifier) {
    val color = ratingColor(rating, dark)
    Canvas(modifier.size(32.dp).semantics { contentDescription = description }) {
        val w = size.width
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        drawCircle(color, radius = w / 2 - stroke.width, style = stroke)
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.36f, w * 0.40f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.64f, w * 0.40f))
        when (rating) {
            Rating.Good -> drawArc(
                color, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(w * 0.30f, w * 0.40f), size = Size(w * 0.40f, w * 0.30f), style = stroke,
            )
            Rating.Ok -> drawLine(
                color, Offset(w * 0.35f, w * 0.68f), Offset(w * 0.65f, w * 0.68f),
                strokeWidth = stroke.width, cap = StrokeCap.Round,
            )
            Rating.Low -> drawArc(
                color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(w * 0.30f, w * 0.58f), size = Size(w * 0.40f, w * 0.30f), style = stroke,
            )
        }
    }
}
