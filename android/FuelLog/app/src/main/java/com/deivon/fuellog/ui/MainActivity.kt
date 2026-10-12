@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.deivon.fuellog.R
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FuelTheme(vm.settings.theme) { FuelApp(vm) } }
    }
}

private enum class Tab(val label: Int, val icon: ImageVector) {
    Overview(R.string.tab_overview, Icons.Filled.Home),
    History(R.string.tab_history, Icons.AutoMirrored.Filled.List),
    Vehicle(R.string.tab_vehicle, Icons.Filled.Settings),
}

/** Three tabs plus the add/edit form on top. Plain Compose state: no navigation library needed. */
@Composable
fun FuelApp(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Overview) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    val formats = rememberFormats(vm.settings.currency)

    val form = vm.form
    if (form != null) {
        BackHandler(onBack = vm::closeForm)
        RefuelEditor(vm, form, formats)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab != Tab.Vehicle) ExtendedFloatingActionButton(
                onClick = { vm.newRefuel(formats.locale) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_refuel)) }
            )
        },
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
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.Overview -> OverviewScreen(vm, formats, modifier) { vm.edit(it, formats.locale) }
            Tab.History -> HistoryScreen(vm, formats, modifier) { vm.edit(it, formats.locale) }
            Tab.Vehicle -> VehicleScreen(vm, showMessage, modifier)
        }
    }
}
