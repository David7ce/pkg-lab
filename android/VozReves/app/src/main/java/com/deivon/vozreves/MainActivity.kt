@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.vozreves

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Edit
import android.widget.Toast
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun attachBaseContext(base: Context) = super.attachBaseContext(Settings.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.onBackground()
    }
}

private enum class Screen { Main, History, Settings }

@Composable
fun App(vm: MainViewModel) {
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
    // Iconos de la barra de estado/navegación acordes al tema de la app, no solo al del sistema.
    DisposableEffect(dark) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        (ctx as? ComponentActivity)?.enableEdgeToEdge(style, style)
        onDispose {}
    }
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun MainScreen(vm: MainViewModel, onHistory: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionDenied = !granted; if (granted) vm.onRecordButton() }

    fun onRecordClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.onRecordButton() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val shareTitle = stringResource(R.string.share_title)
    val msgSaved = stringResource(R.string.saved_to_music)
    val msgSaveError = stringResource(R.string.save_error)
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    // Android 8–9 have no MediaStore folder API without storage permission: let the user pick a place.
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        if (uri != null) scope.launch { toast(if (vm.writeSelection(uri)) msgSaved else msgSaveError) }
    }
    fun saveToDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) scope.launch { toast(if (vm.saveToMusic()) msgSaved else msgSaveError) }
        else createDocument.launch(vm.exportName())
    }
    fun share() = scope.launch {
        val file = vm.exportSelection() ?: return@launch
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("audio/wav")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, shareTitle))
    }

    val recording = vm.mode == Mode.Recording
    val playing = vm.isPlaying

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onHistory, enabled = !recording) {
                    Icon(AppIcons.History, stringResource(R.string.history))
                }
                IconButton(onClick = onSettings, enabled = !recording) {
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
            Status(vm)
            if (permissionDenied || vm.micError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.mic_permission),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                if (permissionDenied) TextButton(onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) { Text(stringResource(R.string.open_settings)) }
            }
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { onRecordClick() },
                shape = CircleShape,
                modifier = Modifier.size(150.dp),
                colors = if (recording) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (recording) AppIcons.Stop else AppIcons.Mic, null, Modifier.size(40.dp))
                    Text(stringResource(if (recording) R.string.stop else R.string.record), fontSize = 18.sp)
                }
            }

            if (vm.hasRecording && !recording) {
                Spacer(Modifier.height(28.dp))
                // El tiempo del audio siempre va de izquierda a derecha, también en árabe.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Waveform(vm.peaks, vm.selection, { vm.progress }, Modifier.fillMaxWidth().height(72.dp))
                        RangeSlider(
                            value = vm.selection,
                            onValueChange = { vm.selection = it },
                            enabled = !playing
                        )
                        PlaybackTimes(vm)
                    }
                }
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
                    IconButton(onClick = { saveToDevice() }, enabled = !playing) {
                        Icon(AppIcons.Download, stringResource(R.string.save_to_device))
                    }
                }
            }
        }
    }
}

/** Texto de estado; aislado para que el contador de grabación no recomponga toda la pantalla. */
@Composable
private fun Status(vm: MainViewModel) {
    val time = rememberTimeFormat()
    Text(
        text = when (vm.mode) {
            Mode.Recording -> stringResource(
                R.string.status_recording, time.clock(vm.recorded), time.clock(vm.settings.maxSeconds.toFloat())
            )
            Mode.Playing -> stringResource(R.string.status_playing)
            Mode.Paused -> stringResource(R.string.status_paused)
            Mode.Idle -> if (vm.hasRecording) stringResource(R.string.recording_length, time.seconds(vm.duration))
                         else stringResource(R.string.status_idle)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/**
 * Tiempos estilo reproductor: transcurrido en el lado donde empieza a sonar y duración del tramo
 * en el otro. Al revés el audio empieza por la derecha, así que el contador va a la derecha.
 */
@Composable
private fun PlaybackTimes(vm: MainViewModel) {
    val time = rememberTimeFormat()
    val sel = vm.selection
    val length = (sel.endInclusive - sel.start) * vm.duration
    val p = vm.progress
    val elapsed = if (p < 0f) 0f else (if (vm.reverse) sel.endInclusive - p else p - sel.start) * vm.duration
    val counter = time.clock(elapsed.coerceIn(0f, length))
    val total = time.clock(length)
    val style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(if (vm.reverse) total else counter, style = style)
        Spacer(Modifier.weight(1f))
        Text(if (vm.reverse) counter else total, style = style)
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
            ) { Text(label, maxLines = 1, textAlign = TextAlign.Center) }
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
    var renaming by remember { mutableStateOf<History.Item?>(null) }
    renaming?.let { item -> RenameDialog(item.title.orEmpty(), onDismiss = { renaming = null }) { vm.renameRecording(item, it); renaming = null } }
    val time = rememberTimeFormat()
    val dates = remember(time.locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, time.locale) }
    SubScreen(stringResource(R.string.history), onBack) { padding ->
        if (vm.recordings.isEmpty()) {
            Text(stringResource(R.string.history_empty), Modifier.padding(padding).padding(24.dp))
        }
        LazyColumn(contentPadding = padding) {
            items(vm.recordings, key = { it.file.name }) { item ->
                val date = dates.format(Date(item.file.lastModified()))
                ListItem(
                    headlineContent = { Text(item.title ?: date) },
                    supportingContent = { Text(if (item.title != null) "$date · ${time.clock(item.seconds)}" else time.clock(item.seconds)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renaming = item }) {
                                Icon(Icons.Filled.Edit, stringResource(R.string.rename))
                            }
                            IconButton(onClick = { vm.deleteRecording(item) }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                            }
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
                listOf(0 to stringResource(R.string.theme_system), 1 to stringResource(R.string.theme_light),
                    2 to stringResource(R.string.theme_dark)),
                s.theme, onSelect = s::updateTheme
            )

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

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(History.MAX_TITLE) },
                label = { Text(stringResource(R.string.name)) }, singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } }
    )
}
