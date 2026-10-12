package com.deivon.vozreves

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Reduce la grabación a [bars] picos normalizados 0..1 (se calcula una vez por grabación). */
fun peaks(samples: ShortArray, bars: Int = 120): FloatArray {
    if (samples.isEmpty()) return FloatArray(0)
    val step = max(1, samples.size / bars)
    val out = FloatArray(min(bars, samples.size)) { b ->
        var m = 0
        for (j in b * step until min((b + 1) * step, samples.size)) m = max(m, abs(samples[j].toInt()))
        m.toFloat()
    }
    val top = out.maxOrNull()?.takeIf { it > 0f } ?: 1f
    for (i in out.indices) out[i] /= top
    return out
}

/** Barras de la onda; las del tramo seleccionado en color, y una línea donde va la reproducción. */
@Composable
fun Waveform(
    peaks: FloatArray,
    selection: ClosedFloatingPointRange<Float>,
    progress: () -> Float, // Se lee al dibujar: solo se repinta el Canvas, no toda la pantalla.
    modifier: Modifier = Modifier
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val cursor = MaterialTheme.colorScheme.error
    Canvas(modifier) {
        val n = peaks.size
        if (n == 0) return@Canvas
        val barW = size.width / n
        val mid = size.height / 2
        peaks.forEachIndexed { i, p ->
            val x = i * barW + barW / 2
            val h = max(p * mid, 1.5f)
            val color = if ((i + 0.5f) / n in selection) active else inactive
            drawLine(color, Offset(x, mid - h), Offset(x, mid + h), barW * 0.6f, StrokeCap.Round)
        }
        val p = progress()
        if (p >= 0f) {
            val x = p * size.width
            drawLine(cursor, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
        }
    }
}
