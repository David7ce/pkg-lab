package com.david7ce.recetario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.david7ce.shared.ui.LocalSharedStrings
import com.david7ce.shared.ui.SharedStrings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.david7ce.recetario.logic.ConflictPolicy
import com.david7ce.recetario.logic.ForeignImport
import com.david7ce.recetario.ui.AppState
import com.david7ce.recetario.ui.CookingScreen
import com.david7ce.recetario.ui.DetailScreen
import com.david7ce.recetario.ui.EditorScreen
import com.david7ce.recetario.ui.LibraryScreen
import com.david7ce.recetario.ui.LocalPlatform
import com.david7ce.recetario.ui.LocalStrings
import com.david7ce.recetario.ui.PlatformUi
import com.david7ce.recetario.ui.RecetarioTheme
import com.david7ce.recetario.ui.Screen
import com.david7ce.recetario.ui.SettingsScreen
import com.david7ce.recetario.ui.Transfer
import com.david7ce.recetario.ui.isDark

/** Root of the shared UI. Each platform provides [PlatformUi] and back handling, and calls this. */
@Composable
fun RecetarioApp(app: AppState, platform: PlatformUi) {
    CompositionLocalProvider(
        LocalStrings provides app.strings,
        LocalPlatform provides platform,
        // Text for the shared components (android/shared), in the app's chosen language.
        LocalSharedStrings provides SharedStrings(
            cancel = app.strings.cancel, back = app.strings.back, close = app.strings.close,
            delete = app.strings.delete, selectAll = "Select all", // Recetario has no multi-select
        ),
    ) {
        RecetarioTheme(isDark(app.themeMode)) {
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(app) { app.messages.collect { snackbar.showSnackbar(it) } }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (val screen = app.screen) {
                    Screen.Library -> LibraryScreen(app)
                    Screen.Settings -> SettingsScreen(app)
                    is Screen.Detail -> DetailScreen(app, screen)
                    is Screen.Editor -> EditorScreen(app, screen.state)
                    is Screen.Cooking -> CookingScreen(app, screen)
                }
                if (app.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding())
                }
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(16.dp),
                )
            }
            app.transfer.urlImport?.let { UrlImportDialog(app.transfer, it) }
            app.transfer.review?.let { ReviewDialog(app.transfer, it) }
            app.transfer.photoProgress?.let { (done, total) -> PhotoProgressDialog(app.transfer, done, total) }
        }
    }
}

@Composable
private fun UrlImportDialog(transfer: Transfer, dialog: Transfer.UrlImport) {
    val s = LocalStrings.current
    val submit = { if (!dialog.loading && dialog.url.isNotBlank()) transfer.importUrl(dialog) }
    AlertDialog(
        onDismissRequest = transfer::cancelUrlImport,
        title = { Text(s.importFromUrl) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dialog.url,
                    onValueChange = {
                        dialog.url = it
                        dialog.error = null
                    },
                    label = { Text(s.urlLabel) },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    enabled = !dialog.loading,
                    isError = dialog.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (dialog.loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(s.reading, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                dialog.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = !dialog.loading && dialog.url.isNotBlank()) { Text(s.importAction) }
        },
        dismissButton = { TextButton(onClick = transfer::cancelUrlImport) { Text(s.cancel) } },
    )
}

/** Name shown to the user; product names are not translated. */
private val ForeignImport.Source.label: String
    get() = when (this) {
        ForeignImport.Source.RecipeSage -> "RecipeSage"
        ForeignImport.Source.RecipeBox -> "My RecipeBox"
    }

@Composable
private fun PhotoProgressDialog(transfer: Transfer, done: Int, total: Int) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = { },
        title = { Text(s.downloadingPhotos(done, total)) },
        text = { LinearProgressIndicator(progress = { if (total == 0) 1f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { },
        dismissButton = { TextButton(onClick = transfer::cancelPhotoDownload) { Text(s.cancel) } },
    )
}

@Composable
private fun ReviewDialog(transfer: Transfer, review: Transfer.Review) {
    val s = LocalStrings.current
    val plan = review.plan
    AlertDialog(
        onDismissRequest = transfer::cancelImport,
        title = { Text(review.source?.let { s.importFrom(it.label) } ?: s.importTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.importSummary(plan.added.size, plan.identical.size, plan.conflicts.size))
                if (review.skipped > 0) Text(s.skipped(review.skipped), style = MaterialTheme.typography.bodySmall)
                if (review.photosToDownload > 0) Text(s.photosToDownload(review.photosToDownload), style = MaterialTheme.typography.bodySmall)
                if (plan.conflicts.isNotEmpty()) {
                    Text(s.conflictQuestion, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { transfer.confirmImport(ConflictPolicy.KeepMine) }) { Text(s.keepMine) }
                    TextButton(onClick = { transfer.confirmImport(ConflictPolicy.ImportAsCopies) }) { Text(s.importAsCopies) }
                    TextButton(onClick = { transfer.confirmImport(ConflictPolicy.ReplaceMine) }) {
                        Text(s.replaceMine, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (plan.conflicts.isEmpty()) {
                TextButton(onClick = { transfer.confirmImport(ConflictPolicy.KeepMine) }) { Text(s.importAction) }
            }
        },
        dismissButton = { TextButton(onClick = transfer::cancelImport) { Text(s.cancel) } },
    )
}
