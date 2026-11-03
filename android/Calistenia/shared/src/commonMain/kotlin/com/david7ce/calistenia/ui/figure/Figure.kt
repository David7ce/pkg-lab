package com.david7ce.calistenia.ui.figure

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
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
 * A 2D profile-view stick figure, animated by interpolating between two [Pose]s. Spike for
 * `builtin:push-up` only -- see `categories/design-art/animation/gym-body-animation.md` in
 * AgentHub for the full design and the other families it could grow into.
 */
@Composable
fun Figure(
    poses: List<Pose>,
    periodMs: Int = 2200,
    playing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    require(poses.size == 2) { "Figure animates between exactly two poses (up/down); got ${poses.size}." }
    val progress = remember(poses) { Animatable(0f) }

    LaunchedEffect(playing, periodMs, poses) {
        if (!playing) return@LaunchedEffect
        val spec = tween<Float>(periodMs, easing = LinearEasing)
        while (true) {
            progress.animateTo(1f, spec)
            progress.animateTo(0f, spec)
        }
    }

    // A fixed bounding box over both poses, computed once per `poses`: the transform below must
    // not change frame to frame, or the figure would visibly resize/jump as it animates.
    val bounds = remember(poses) { boundsOf(skeletonFor(poses[0]), skeletonFor(poses[1])) }

    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.aspectRatio(1.8f)) {
        val skeleton = skeletonFor(lerp(poses[0], poses[1], progress.value))
        val scale = minOf(size.width / bounds.width, size.height / bounds.height) * 0.85f
        val offsetX = size.width / 2f - bounds.centerX * scale
        val offsetY = size.height / 2f - bounds.centerY * scale
        fun at(point: FigurePoint) = Offset(point.x * scale + offsetX, point.y * scale + offsetY)

        val stroke = size.minDimension * 0.05f
        drawLimb(at(skeleton.hip), at(skeleton.shoulder), color, stroke)
        drawLimb(at(skeleton.shoulder), at(skeleton.elbow), color, stroke)
        drawLimb(at(skeleton.elbow), at(skeleton.hand), color, stroke)
        drawLimb(at(skeleton.hip), at(skeleton.knee), color, stroke)
        drawLimb(at(skeleton.knee), at(skeleton.ankle), color, stroke)
        drawCircle(color, radius = skeleton.headRadius * scale, center = at(skeleton.headCenter))
    }
}

private fun DrawScope.drawLimb(from: Offset, to: Offset, color: Color, strokeWidth: Float) {
    drawLine(color, from, to, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private data class FigureBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val width get() = maxX - minX
    val height get() = maxY - minY
    val centerX get() = (minX + maxX) / 2f
    val centerY get() = (minY + maxY) / 2f
}

private fun boundsOf(vararg skeletons: FigureSkeleton): FigureBounds {
    val points = skeletons.flatMap {
        listOf(it.headCenter, it.shoulder, it.hip, it.elbow, it.hand, it.knee, it.ankle)
    }
    val headPad = skeletons.maxOf { it.headRadius }
    return FigureBounds(
        minX = points.minOf { it.x } - headPad,
        maxX = points.maxOf { it.x } + headPad,
        minY = points.minOf { it.y } - headPad,
        maxY = points.maxOf { it.y } + headPad,
    )
}
