package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.Rating
import com.david7ce.sleepcycles.logic.Suggestion
import com.david7ce.sleepcycles.logic.formatHours
import com.david7ce.sleepcycles.logic.formatTime

@Composable
fun MainScreen(app: AppState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val dark = isDark(app.theme)
    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TopRow(app)
                SegmentedChoice(
                    options = listOf(Mode.Bed to s.goToBedAt, Mode.Wake to s.wakeUpAt),
                    selected = app.mode,
                    onSelect = app::selectMode,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeField(app.anchor, app.format, app::setAnchor, Modifier.weight(1f))
                    if (platform.canPickTime) {
                        IconButton(
                            onClick = {
                                platform.pickTime(app.anchor, app.format == ClockFormat.H24, app::setAnchor)
                            },
                        ) { Icon(AppIcons.Clock, contentDescription = s.pickTime) }
                    }
                }
                Button(onClick = app::sleepNow, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(s.sleepNow) }
                Text(
                    s.hint(app.latencyMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (app.mode == Mode.Wake) s.resultsWake else s.resultsBed,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentFor(app.mode, dark),
                )
                if (platform.canSaveAlarm) {
                    Text(
                        s.alarmHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                app.suggestions.forEach { SuggestionCard(it, app, dark) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (dark) NightSky { content() }
    else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

// The PWA pale lavender is unreadable on a light background; light mode uses the theme colors.
@Composable
private fun accentFor(mode: Mode, dark: Boolean): Color = when {
    mode == Mode.Wake && dark -> WakeAccent
    mode == Mode.Wake -> MaterialTheme.colorScheme.tertiary
    dark -> BedAccent
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun TopRow(app: AppState) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("SleepCycles", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = app::openHelp) { Icon(AppIcons.Info, contentDescription = s.howItWorks) }
        IconButton(onClick = app::openSettings) { Icon(AppIcons.Settings, contentDescription = s.settings) }
    }
}

/** Tapping a card hands that time to the system clock app as a new alarm (where the platform can). */
@Composable
private fun SuggestionCard(item: Suggestion, app: AppState, dark: Boolean) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val description = when (item.rating) {
        Rating.Good -> s.ratingGood
        Rating.Ok -> s.ratingOk
        Rating.Low -> s.ratingLow
    }
    val cardColor = if (dark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceContainer
    val shape = RoundedCornerShape(16.dp)
    val tap = if (platform.canSaveAlarm) {
        Modifier.clickable(onClickLabel = s.alarmHint) { platform.saveAlarm(item.timeOfDay, "SleepCycles") }
    } else {
        Modifier
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .then(tap)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatTime(item.timeOfDay, app.format, s.am, s.pm),
                style = MaterialTheme.typography.headlineMedium,
                color = accentFor(app.mode, dark),
            )
            Text(
                s.cycles(item.cycles, formatHours(item.sleepMinutes, s.decimalSeparator)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RatingFace(item.rating, description, dark)
    }
}
