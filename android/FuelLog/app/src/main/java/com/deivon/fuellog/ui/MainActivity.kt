@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.deivon.fuellog.R
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppSettings.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FuelTheme(vm.settings.theme) { FuelApp(vm) } }
    }
}

private enum class Tab(val label: Int) {
    Home(R.string.tab_home), History(R.string.tab_history), Settings(R.string.tab_settings);

    val icon: ImageVector get() = when (this) {
        Home -> Icons.Filled.Home
        History -> Icons.AutoMirrored.Filled.List
        Settings -> Icons.Filled.Settings
    }
}

/**
 * Three tabs plus full-screen forms on top. Plain Compose state: no navigation library needed.
 * Wide windows (tablets, landscape) get a side navigation rail instead of the bottom bar.
 */
@Composable
fun FuelApp(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    val formats = rememberFormats(vm.settings.currency, vm.settings.units)
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    val msgSaved = stringResource(R.string.saved)
    val msgSavedWith = stringResource(R.string.saved_consumption)
    val msgDeleted = stringResource(R.string.deleted)
    LaunchedEffect(vm.event) {
        when (val e = vm.event) {
            is UiEvent.Saved -> {
                vm.consumeEvent()
                snackbar.showSnackbar(e.l100?.let { msgSavedWith.format(formats.consumption(it)) } ?: msgSaved)
            }
            UiEvent.Deleted -> {
                vm.consumeEvent()
                snackbar.showSnackbar(msgDeleted)
            }
            null -> Unit
        }
    }

    vm.vehicleForm?.let { v ->
        BackHandler(onBack = vm::closeVehicle)
        VehicleEditor(vm, v)
        return
    }
    vm.form?.let { f ->
        RefuelEditor(vm, f, formats, showMessage)
        return
    }

    val addButton: @Composable () -> Unit = {
        if (tab != Tab.Settings) ExtendedFloatingActionButton(
            onClick = { vm.newRefuel(formats.locale) },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(stringResource(R.string.add_refuel)) }
        )
    }
    val content: @Composable (Modifier) -> Unit = { modifier ->
        when (tab) {
            Tab.Home -> OverviewScreen(vm, formats, modifier) { vm.edit(it, formats.locale) }
            Tab.History -> HistoryScreen(vm, formats, modifier) { vm.edit(it, formats.locale) }
            Tab.Settings -> SettingsScreen(vm, showMessage, modifier)
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(Modifier.fillMaxHeight().safeDrawingPadding()) {
                Spacer(Modifier.weight(1f))
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.label)) }
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, floatingActionButton = addButton) { padding ->
                // Keep text lines readable on very wide screens.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    content(Modifier.padding(padding).widthIn(max = 720.dp))
                }
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = addButton,
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t, onClick = { tab = t },
                            icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.label)) }
                        )
                    }
                }
            }
        ) { padding -> content(Modifier.padding(padding)) }
    }
}
