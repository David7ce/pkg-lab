package com.deivon.fuellog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.logic.ConsumptionInterval

val CardShape = RoundedCornerShape(16.dp)

/** One figure on the overview. [value] null → "—" with [unavailable] explaining why. */
@Composable
fun StatCard(label: String, value: String?, unavailable: String, modifier: Modifier = Modifier) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(top = 6.dp))
            Text(value ?: "—", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value == null) {
                Text(unavailable, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** A refuelling in lists: date and total on top, fuel details below, consumption if this fill closes an interval. */
@Composable
fun RefuelRow(r: Refuel, interval: ConsumptionInterval?, formats: Formats, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${formats.date(r.timestamp)} · ${r.station}",
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(formats.money(r.totalCents), style = MaterialTheme.typography.titleSmall)
        }
        val details = buildList {
            add(formats.litres(r.volumeMl))
            add(formats.pricePerLitre(r.pricePerLitreMilli))
            add(formats.km(r.odometerKm))
            if (!r.fullTank) add(stringResource(R.string.partial))
        }.joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (interval != null) {
            Text(
                formats.consumption(interval.litresPer100Km) + " · " + formats.km(interval.distanceKm),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
