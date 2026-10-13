package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Mountain ridge as (x, y) fractions of the canvas; the area below the line is filled. */
private val FarRidge = listOf(0f to 0.78f, 0.18f to 0.70f, 0.34f to 0.76f, 0.55f to 0.66f, 0.78f to 0.75f, 1f to 0.69f)
private val MidRidge = listOf(0f to 0.84f, 0.22f to 0.78f, 0.45f to 0.86f, 0.68f to 0.77f, 0.88f to 0.83f, 1f to 0.80f)
private val NearRidge = listOf(0f to 0.92f, 0.25f to 0.88f, 0.5f to 0.94f, 0.75f to 0.89f, 1f to 0.93f)

private fun DrawScope.ridge(points: List<Pair<Float, Float>>, color: Color) {
    val path = Path().apply {
        moveTo(0f, size.height)
        points.forEach { (x, y) -> lineTo(x * size.width, y * size.height) }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

/**
 * The PWA night sky: gradient, stars and three mountain ridges, drawn (no images) so it costs nothing
 * to ship and scales to any window. [content] is laid out on top.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val random = Random(7) // fixed seed: the same sky on every launch and recomposition
        List(70) {
            Star(random.nextFloat(), random.nextFloat() * 0.7f, 0.6f + random.nextFloat() * 1.4f, 0.4f + random.nextFloat() * 0.5f)
        }
    }
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFF0A0920), 0.4f to Color(0xFF140E3D), 0.7f to Color(0xFF1A114F), 1f to Color(0xFF2C2170),
                )
            )
            stars.forEach {
                drawCircle(Color.White.copy(alpha = it.alpha), it.radius * density, Offset(it.x * size.width, it.y * size.height))
            }
            ridge(FarRidge, Color(0xFF453A86))
            ridge(MidRidge, Color(0xFF2B2166))
            ridge(NearRidge, Color(0xFF120C33))
        }
        content()
    }
}
