package com.deivon.recetario.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Every setting and every library-wide action in one place: appearance, language, data, about. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: AppState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val t = app.transfer
    Scaffold(
        topBar = { TopAppBar(title = { Text(s.settings) }, navigationIcon = { BackButton(app) }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 32.dp),
            ) {
                SectionTitle(s.theme, icon = AppIcons.Theme)
                Column(Modifier.selectableGroup()) {
                    listOf(ThemeMode.System to s.themeSystem, ThemeMode.Light to s.themeLight, ThemeMode.Dark to s.themeDark)
                        .forEach { (mode, label) -> OptionRow(label, null, app.themeMode == mode) { app.setTheme(mode) } }
                }

                HorizontalDivider(Modifier.padding(top = 12.dp))
                SectionTitle(s.language, icon = AppIcons.Language)
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        val label = if (language == AppLanguage.System) s.languageSystem else language.nativeName
                        OptionRow(label, null, app.language == language) { app.chooseLanguage(language) }
                    }
                }

                HorizontalDivider(Modifier.padding(top = 12.dp))
                SectionTitle(s.data, icon = AppIcons.Save)
                ActionRow(s.importFile, AppIcons.Upload) { t.importFile(platform) }
                ActionRow(s.importFromUrl, AppIcons.Link) { t.startUrlImport() }
                ActionRow(s.exportLibrary, AppIcons.Download) { t.exportLibrary(platform) }
                ActionRow(s.backup, AppIcons.Save) { t.backup(platform) }
                if (t.canExportPdf) ActionRow(s.cookbookPdf, AppIcons.Pdf) { t.cookbook(platform) }

                HorizontalDivider(Modifier.padding(top = 12.dp))
                SectionTitle(s.about, icon = AppIcons.Info)
                Text(
                    s.aboutData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
