@file:OptIn(ExperimentalMaterial3Api::class)

package com.david7ce.fuellog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.fuellog.logic.ConsumptionInterval
import com.david7ce.shared.ui.ChoiceDropdown

/** Read-only dropdown (language, currency, units). The implementation lives in android/shared. */
@Composable
fun <T> DropdownField(
    label: String,
    icon: ImageVector,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) = ChoiceDropdown(options, selected, modifier, label, icon, onSelect)

/**
 * Consumption of the last intervals as a simple line, oldest left, with the reference as a dashed
 * line. Values are in the user's unit (L/100 km or mpg); hand-drawn, no chart library.
 */
@Composable
fun ConsumptionChart(intervals: List<ConsumptionInterval>, referenceL100: Double?, formats: Formats, title: String) {
    val values = intervals.takeLast(12).map { formats.consumptionValue(it.litresPer100Km) }
    if (values.size < 2) return
    val reference = referenceL100?.let { formats.consumptionValue(it) }
    val all = values + listOfNotNull(reference)
    val min = all.min()
    val max = all.max()
    val span = (max - min).takeIf { it > 1e-9 } ?: 1.0
    val line = MaterialTheme.colorScheme.primary
    val refColor = MaterialTheme.colorScheme.outline

    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text(
                    "${formats.number1(min)} – ${formats.number1(max)} ${formats.consumptionUnit}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Canvas(Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp)) {
                val pad = 6.dp.toPx()
                fun x(i: Int) = pad + i * (size.width - 2 * pad) / (values.size - 1)
                fun y(v: Double) = (pad + (1 - (v - min) / span) * (size.height - 2 * pad)).toFloat()
                reference?.let {
                    drawLine(refColor, Offset(0f, y(it)), Offset(size.width, y(it)), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                }
                val path = Path().apply { values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
                drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                values.forEachIndexed { i, v -> drawCircle(line, 3.5.dp.toPx(), Offset(x(i), y(v))) }
            }
        }
    }
}
