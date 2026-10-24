package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.AppLanguage
import com.david7ce.calistenia.logic.REST_OPTIONS
import com.david7ce.shared.ui.ChoiceDropdown
import com.david7ce.shared.ui.SectionTitle
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.shared.ui.SharedIcons
import com.david7ce.shared.ui.ThemeMode

@Composable
fun SettingsScreen(app: AppState) {
    val strings = LocalStrings.current
    val platform = LocalPlatform.current
    val settings = app.settings

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SectionTitle(strings.appearance)
        Text(
            strings.theme,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SegmentedChoice(
            options = ThemeMode.entries.map { it to themeLabel(it, strings) },
            selected = settings.theme,
            onSelect = app::setTheme,
        )
        Spacer(Modifier.height(16.dp))
        ChoiceDropdown(
            options = AppLanguage.entries.map { language ->
                language to language.nativeName.ifBlank { strings.languageSystem }
            },
            selected = settings.language,
            label = strings.language,
            leadingIcon = SharedIcons.Language,
            onSelect = app::setLanguage,
        )

        SectionTitle(strings.training)
        val restOptions = (REST_OPTIONS + settings.defaultRestSeconds).distinct().sorted()
        ChoiceDropdown(
            options = restOptions.map { seconds -> seconds to "$seconds ${strings.secondsUnit}" },
            selected = settings.defaultRestSeconds,
            label = strings.defaultRest,
            leadingIcon = SharedIcons.Timer,
            onSelect = app::setDefaultRest,
        )
        if (platform.notificationsAvailable) {
            ToggleRow(
                title = strings.restNotifications,
                hint = strings.restNotificationsHint,
                checked = settings.restNotifications,
                onCheckedChange = { enabled ->
                    app.setRestNotifications(enabled)
                    if (enabled && !platform.notificationsGranted) platform.requestNotificationPermission()
                },
            )
            if (settings.restNotifications && !platform.notificationsGranted) {
                Text(
                    strings.notificationsDenied,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            ToggleRow(
                title = strings.restSound,
                checked = settings.restSound,
                onCheckedChange = app::setRestSound,
            )
            ToggleRow(
                title = strings.restVibrate,
                checked = settings.restVibrate,
                onCheckedChange = app::setRestVibrate,
            )
        }
        if (platform.canKeepScreenOn) {
            ToggleRow(
                title = strings.keepScreenOn,
                checked = settings.keepScreenOn,
                onCheckedChange = app::setKeepScreenOn,
            )
        }

        SectionTitle(strings.data)
        Text(
            strings.backupHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Button(
            onClick = {
                app.exportBackup { fileName, json ->
                    platform.saveFile(fileName, "application/json", json.encodeToByteArray()) { ok ->
                        if (!ok) app.notifyExportFailed()
                    }
                }
            },
            enabled = !app.backupWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (app.backupWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(strings.preparingFile, modifier = Modifier.padding(start = 8.dp))
            } else {
                Text(strings.exportBackup)
            }
        }
        OutlinedButton(
            onClick = {
                platform.openFile { result ->
                    if (result != null) {
                        result.fold(
                            onSuccess = { bytes -> app.importBackup(bytes) },
                            onFailure = { app.notifyImportFailed() },
                        )
                    }
                }
            },
            enabled = !app.backupWorking,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(strings.importBackup) }

        SectionTitle(strings.about)
        Text(
            strings.privacy,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = app::openHelp, modifier = Modifier.padding(top = 4.dp)) {
            Text(strings.howItWorks)
        }
        Text(
            strings.versionLabel(app.appVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun themeLabel(mode: ThemeMode, strings: Strings): String = when (mode) {
    ThemeMode.System -> strings.themeSystem
    ThemeMode.Light -> strings.themeLight
    ThemeMode.Dark -> strings.themeDark
}
