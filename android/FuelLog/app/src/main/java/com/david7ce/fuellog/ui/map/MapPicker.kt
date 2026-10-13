@file:OptIn(ExperimentalMaterial3Api::class)

package com.david7ce.fuellog.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.david7ce.fuellog.R
import com.david7ce.fuellog.logic.GeoPoint
import com.david7ce.fuellog.ui.AppIcons
import kotlinx.coroutines.launch

/**
 * Full-screen location picker: the point under the centre pin is the chosen location.
 * Also accepts pasted coordinates and a one-shot "my location".
 */
@Composable
fun MapPicker(
    initial: GeoPoint,
    initialZoom: Double,
    pins: List<GeoPoint>,
    onPick: (GeoPoint) -> Unit,
    onClose: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lat by rememberSaveable { mutableDoubleStateOf(initial.lat) }
    var lon by rememberSaveable { mutableDoubleStateOf(initial.lon) }
    var zoom by rememberSaveable { mutableDoubleStateOf(initialZoom) }
    var typed by rememberSaveable { mutableStateOf("") }
    val center = GeoPoint(lat, lon)
    fun moveTo(p: GeoPoint, z: Double = zoom) { lat = p.lat; lon = p.lon; zoom = z.coerceIn(MIN_ZOOM, MAX_ZOOM) }

    val noLocation = stringResource(R.string.location_unavailable)
    fun locate() = scope.launch { currentLocation(context)?.let { moveTo(it, 16.0) } ?: showMessage(noLocation) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate() else showMessage(noLocation)
    }
    fun onMyLocation() {
        val has = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (has) locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.pick_location)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                TileMap(center, zoom, { p, z -> moveTo(p, z) }, pins, Modifier.fillMaxSize())
                // The pin's tip marks the centre.
                Icon(
                    Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).size(40.dp).offset(y = (-20).dp)
                )
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(onClick = { moveTo(center, zoom + 1) }) { Icon(Icons.Filled.Add, stringResource(R.string.zoom_in)) }
                    FilledTonalIconButton(onClick = { moveTo(center, zoom - 1) }) { Icon(AppIcons.Remove, stringResource(R.string.zoom_out)) }
                    FilledTonalIconButton(onClick = { onMyLocation() }) { Icon(AppIcons.MyLocation, stringResource(R.string.my_location)) }
                }
                // Required attribution for OpenStreetMap data.
                Text(
                    "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall, color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(topStart = 6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it; GeoPoint.parse(it)?.let { p -> moveTo(p, maxOf(zoom, 15.0)) } },
                    label = { Text(stringResource(R.string.paste_coordinates)) },
                    placeholder = { Text("40.41680, -3.70380") },
                    isError = typed.isNotBlank() && GeoPoint.parse(typed) == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { GeoPoint.parse(typed)?.let { moveTo(it) } }),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(center.format(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Button(onClick = { onPick(center) }) { Text(stringResource(R.string.use_location)) }
                }
            }
        }
    }
}
