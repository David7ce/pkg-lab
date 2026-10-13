package com.david7ce.sleepcycles.ui

import com.david7ce.shared.ui.ThemeMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.david7ce.shared.ui.ChoiceDropdown
import com.david7ce.shared.ui.SectionTitle
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.ClockStyle
import com.david7ce.sleepcycles.logic.MAX_LATENCY_MINUTES
import com.david7ce.sleepcycles.logic.latencyText
import com.david7ce.sleepcycles.logic.parseLatency

/**
 * Settings as a dialog with a Done button, like the PWA. Most-used first: age, time to fall asleep,
 * time format, theme, language; version at the very bottom.
 */
@Composable
fun SettingsDialog(app: AppState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    AlertDialog(
        onDismissRequest = app::back,
        title = { Text(s.settings) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SectionTitle(s.ageRange)
                ChoiceDropdown(
                    options = AgeRange.entries.map { it to s.ageLabel(it) },
                    selected = app.age,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = app::selectAge,
                )

                SectionTitle(s.fallAsleepTime)
                LatencyField(app.latencyMinutes, app::setLatency)

                SectionTitle(s.timeFormat)
                SegmentedChoice(
                    options = listOf(ClockFormat.H24 to s.format24, ClockFormat.H12 to s.format12),
                    selected = app.format,
                    onSelect = app::selectFormat,
                )

                // Only where the OS has its own time picker (Android); desktop types the time.
                if (platform.canPickTime) {
                    SectionTitle(s.clockType)
                    SegmentedChoice(
                        options = listOf(ClockStyle.Analog to s.clockAnalog, ClockStyle.Digital to s.clockDigital),
                        selected = app.clockStyle,
                        onSelect = app::selectClockStyle,
                    )
                }

                SectionTitle(s.theme)
                SegmentedChoice(
                    options = listOf(
                        ThemeMode.System to s.themeSystem, ThemeMode.Light to s.themeLight, ThemeMode.Dark to s.themeDark,
                    ),
                    selected = app.theme,
                    onSelect = app::selectTheme,
                )

                SectionTitle(s.language)
                ChoiceDropdown(
                    options = AppLanguage.entries.map { it to (if (it == AppLanguage.System) s.languageSystem else it.nativeName) },
                    selected = app.language,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = app::selectLanguage,
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    s.versionLabel(app.appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = app::back) { Text(s.done) } },
    )
}

/** Free text, applied as typed: empty or invalid counts as 0 and anything above the maximum is capped. */
@Composable
private fun LatencyField(minutes: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(minutes.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = latencyText(it) // shows the value actually used: "500" turns into "120"
            onChange(parseLatency(text))
        },
        supportingText = { Text("0–$MAX_LATENCY_MINUTES") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
