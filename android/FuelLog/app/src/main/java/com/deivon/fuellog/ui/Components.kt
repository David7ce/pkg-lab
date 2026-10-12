package com.deivon.fuellog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.VehicleType
import com.deivon.fuellog.logic.ConsumptionInterval
import com.deivon.fuellog.logic.Extremes

val CardShape = RoundedCornerShape(16.dp)

/** One figure on the overview. [value] null → "—" with [unavailable] explaining why. */
@Composable
fun StatCard(
    icon: ImageVector, label: String, value: String?, unavailable: String,
    modifier: Modifier = Modifier, extra: String? = null, extraColor: Color = Color.Unspecified,
) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(value ?: "—", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value == null) {
                Text(unavailable, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (extra != null) {
                Text(extra, style = MaterialTheme.typography.bodySmall, color = extraColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant })
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** Vehicle type icon in a tinted circle. */
@Composable
fun VehicleBadge(type: VehicleType, size: Int = 40) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(size.dp)) {
        Icon(
            AppIcons.of(type), null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding((size / 5).dp)
        )
    }
}

/** Colour for "high" (worse) and "low" (better) highlights: error red and the accent. */
@Composable fun highColor(): Color = MaterialTheme.colorScheme.error
@Composable fun lowColor(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun Tag(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** A refuelling in lists: date and total on top, fuel details below, highlights and consumption. */
@Composable
fun RefuelRow(r: Refuel, interval: ConsumptionInterval?, extremes: Extremes, formats: Formats, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formats.date(r.timestamp)} · ${r.station}",
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (r.latitude != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(formats.money(r.totalCents), style = MaterialTheme.typography.titleSmall)
        }
        val details = buildList {
            add(formats.volume(r.volumeMl))
            add(formats.pricePerVolume(r.pricePerLitreMilli))
            add(formats.distance(r.odometerKm))
            if (!r.fullTank) add(stringResource(R.string.partial))
        }.joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (r.id == extremes.highestPrice?.id) Tag(AppIcons.TrendingUp, stringResource(R.string.highest_price), highColor())
        if (r.id == extremes.lowestPrice?.id) Tag(AppIcons.TrendingDown, stringResource(R.string.lowest_price), lowColor())
        if (interval != null) {
            val color = when (interval) {
                extremes.worstConsumption -> highColor()
                extremes.bestConsumption -> lowColor()
                else -> MaterialTheme.colorScheme.onSurface
            }
            val label = formats.consumption(interval.litresPer100Km) + " · " + formats.distance(interval.distanceKm)
            Tag(AppIcons.Speed, label, color)
        }
    }
}
