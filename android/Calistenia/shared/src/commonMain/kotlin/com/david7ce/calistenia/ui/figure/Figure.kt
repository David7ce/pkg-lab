package com.david7ce.calistenia.ui.figure

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A 2D profile-view stick figure that moves through [animation]'s keyframes. Colours come from the
 * Material theme; the rig's contact joint gets a thin floor line or a thick bar with end caps (see [contactStrokes]), so the figure reads as anchored
 * rather than floating. Purely a form illustration: it needs no image assets, storage or network.
 *
 * The caller controls the box (pass a height): the figure is scaled to fit inside it, preserving
 * proportions, and centred. Use [playing] to freeze it on the current pose.
 */
@Composable
fun Figure(
    animation: FigureAnimation,
    playing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val clock = remember { Animatable(0f) }

    LaunchedEffect(animation) { clock.snapTo(0f) }
    LaunchedEffect(animation, playing) {
        if (!playing || animation.isStatic) return@LaunchedEffect
        val spec = tween<Float>(animation.periodMs, easing = LinearEasing)
        while (true) {
            clock.animateTo(animation.periodMs.toFloat(), spec)
            clock.snapTo(0f)
        }
    }

    // A fixed box over the whole cycle: computing it per frame would resize and jump the figure.
    val bounds = remember(animation) { boundsFor(animation) }
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val contactColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val skeleton = anchoredSkeleton(animation.poseAt(clock.value.toLong()), animation.rig)
        val scale = minOf(size.width / bounds.width, size.height / bounds.height) * 0.86f
        if (scale <= 0f) return@Canvas
        val offsetX = size.width / 2f - bounds.centerX * scale
        val offsetY = size.height / 2f - bounds.centerY * scale
        fun at(point: FigurePoint) = Offset(point.x * scale + offsetX, point.y * scale + offsetY)

        for (line in contactStrokes(animation.rig.contact, bounds.minX, bounds.maxX)) {
            drawLimb(
                at(line.from),
                at(line.to),
                contactColor.copy(alpha = line.alpha),
                (line.width * scale).coerceAtLeast(1f),
            )
        }

        val stroke = scale * 0.05f
        drawLimb(at(skeleton.hip), at(skeleton.shoulder), bodyColor, stroke)
        drawLimb(at(skeleton.shoulder), at(skeleton.elbow), bodyColor, stroke)
        drawLimb(at(skeleton.elbow), at(skeleton.hand), bodyColor, stroke)
        drawLimb(at(skeleton.hip), at(skeleton.knee), bodyColor, stroke)
        drawLimb(at(skeleton.knee), at(skeleton.ankle), bodyColor, stroke)
        drawCircle(bodyColor, radius = skeleton.headRadius * scale, center = at(skeleton.headCenter))
    }
}

private fun DrawScope.drawLimb(from: Offset, to: Offset, color: Color, strokeWidth: Float) {
    drawLine(color, from, to, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
