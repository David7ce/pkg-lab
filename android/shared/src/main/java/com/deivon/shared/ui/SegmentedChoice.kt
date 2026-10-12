package com.deivon.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Selector de una opción en fila, con icono opcional. Sustituye a SegmentedButton, que con icono
 * dejaba el icono encima del texto. Aquí cada segmento es una fila de altura fija: icono + texto
 * (el texto se recorta con "…" si no cabe). Sin check: el color de fondo indica la opción activa.
 */
@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icons: Map<T, ImageVector> = emptyMap(),
    onSelect: (T) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val border = colors.outline.copy(alpha = if (enabled) 1f else 0.38f)
    Row(modifier.fillMaxWidth().height(48.dp).clip(shape).border(1.dp, border, shape)) {
        options.forEachIndexed { i, (value, label) ->
            val isSelected = value == selected
            val content = when {
                !enabled -> colors.onSurface.copy(alpha = 0.38f)
                isSelected -> colors.onSecondaryContainer
                else -> colors.onSurface
            }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icons[value]?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = content)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    label,
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
            if (i < options.lastIndex) VerticalDivider(Modifier.fillMaxHeight(), color = border)
        }
    }
}
