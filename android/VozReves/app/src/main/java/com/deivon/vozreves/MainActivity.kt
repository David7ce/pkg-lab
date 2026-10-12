@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.vozreves

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(Settings.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private enum class Screen { Main, History, Settings }

@Composable
fun App(vm: MainViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.Main) }
    val back = { screen = Screen.Main }
    AppTheme(vm.settings.theme) {
        BackHandler(enabled = screen != Screen.Main, onBack = back)
        when (screen) {
            Screen.Main -> MainScreen(vm, { screen = Screen.History }, { screen = Screen.Settings })
            Screen.History -> HistoryScreen(vm, back)
            Screen.Settings -> SettingsScreen(vm, back)
        }
    }
}

@Composable
fun AppTheme(theme: Int, content: @Composable () -> Unit) {
    val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun secs(v: Float) = "%.1f".format(v)

@Composable
private fun MainScreen(vm: MainViewModel, onHistory: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.onRecordButton() }

    fun onRecordClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.onRecordButton() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val shareTitle = stringResource(R.string.share_title)
    fun share() = scope.launch {
        val file = vm.exportSelection() ?: return@launch
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("audio/wav")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, shareTitle))
    }

    val capturing = vm.mode == Mode.Recording || vm.mode == Mode.Countdown
    val playing = vm.mode == Mode.Playing || vm.mode == Mode.Paused

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onHistory, enabled = !capturing) {
                    Icon(AppIcons.History, stringResource(R.string.history))
                }
                IconButton(onClick = onSettings, enabled = !capturing) {
                    Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (vm.mode) {
                    Mode.Countdown, Mode.Recording -> stringResource(R.string.status_recording, vm.settings.maxSeconds)
                    Mode.Playing -> stringResource(R.string.status_playing)
                    Mode.Paused -> stringResource(R.string.status_paused)
                    Mode.Idle -> if (vm.hasRecording) stringResource(R.string.recording_length, secs(vm.duration))
                                 else stringResource(R.string.status_idle)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { onRecordClick() },
                shape = CircleShape,
                modifier = Modifier.size(150.dp),
                colors = if (capturing) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors()
            ) {
                if (vm.mode == Mode.Countdown) {
                    Text("${vm.countdown}", fontSize = 56.sp, fontWeight = FontWeight.Bold)
                } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (capturing) AppIcons.Stop else AppIcons.Mic, null, Modifier.size(40.dp))
                    Text(stringResource(if (capturing) R.string.stop else R.string.record), fontSize = 18.sp)
                }
            }

            if (vm.hasRecording && !capturing) {
                Spacer(Modifier.height(28.dp))
                // El tiempo del audio siempre va de izquierda a derecha, también en árabe.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Waveform(vm.peaks, vm.selection, vm.progress, Modifier.fillMaxWidth().height(72.dp))
                        RangeSlider(
                            value = vm.selection,
                            onValueChange = { vm.selection = it },
                            enabled = !playing
                        )
                    }
                }
                Text(
                    stringResource(
                        R.string.selection,
                        secs(vm.selection.start * vm.duration), secs(vm.selection.endInclusive * vm.duration)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Choice(
                    listOf(true to stringResource(R.string.reverse), false to stringResource(R.string.normal)),
                    vm.reverse, enabled = !playing
                ) { vm.reverse = it }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::stop, enabled = playing) {
                        Icon(AppIcons.Stop, stringResource(R.string.stop))
                    }
                    FilledIconButton(onClick = vm::togglePlay, modifier = Modifier.size(64.dp)) {
                        val isPlaying = vm.mode == Mode.Playing
                        Icon(
                            if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                            stringResource(if (isPlaying) R.string.pause else R.string.play),
                            Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { share() }, enabled = !playing) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share))
                    }
                }
            }
        }
    }
}

/** Selector de una opción (botones segmentados). */
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, enabled: Boolean = true, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                enabled = enabled
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            }
        )
    }, content = content)
}

@Composable
private fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val format = remember { DateFormat.getMediumDateFormat(context) to DateFormat.getTimeFormat(context) }
    SubScreen(stringResource(R.string.history), onBack) { padding ->
        if (vm.recordings.isEmpty()) {
            Text(stringResource(R.string.history_empty), Modifier.padding(padding).padding(24.dp))
        }
        LazyColumn(contentPadding = padding) {
            items(vm.recordings, key = { it.file.name }) { item ->
                val date = Date(item.file.lastModified())
                ListItem(
                    headlineContent = { Text("${format.first.format(date)}  ${format.second.format(date)}") },
                    supportingContent = { Text("${secs(item.seconds)} s") },
                    trailingContent = {
                        IconButton(onClick = { vm.deleteRecording(item) }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    },
                    modifier = Modifier.clickable { vm.loadRecording(item); onBack() }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val activity = LocalContext.current as Activity
    var confirmClear by remember { mutableStateOf(false) }
    val seconds = { n: Int -> activity.getString(R.string.seconds_short, n) }

    SubScreen(stringResource(R.string.settings), onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Section(stringResource(R.string.language))
            (listOf("" to stringResource(R.string.system_default)) + Settings.LANGUAGES).forEach { (code, name) ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = s.language == code) {
                        if (s.language != code) { s.updateLanguage(code); activity.recreate() }
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = s.language == code, onClick = null)
                    Spacer(Modifier.size(12.dp))
                    Text(name)
                }
            }

            Section(stringResource(R.string.theme))
            Choice(
                listOf(0 to stringResource(R.string.system_default), 1 to stringResource(R.string.theme_light),
                    2 to stringResource(R.string.theme_dark)),
                s.theme, onSelect = s::updateTheme
            )

            Section(stringResource(R.string.countdown))
            Choice(listOf(0 to stringResource(R.string.off), 3 to seconds(3), 5 to seconds(5)), s.countdown,
                onSelect = s::updateCountdown)

            Section(stringResource(R.string.max_length))
            Choice(listOf(30 to seconds(30), 60 to seconds(60), 120 to seconds(120)), s.maxSeconds,
                onSelect = s::updateMaxSeconds)

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { confirmClear = true }, enabled = vm.recordings.isNotEmpty()) {
                Text(stringResource(R.string.clear_history))
            }
            Spacer(Modifier.height(16.dp))
            val version = remember { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName }
            Text(
                stringResource(R.string.version, version ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        text = { Text(stringResource(R.string.clear_history) + "?") },
        confirmButton = {
            TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.back)) } }
    )
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

