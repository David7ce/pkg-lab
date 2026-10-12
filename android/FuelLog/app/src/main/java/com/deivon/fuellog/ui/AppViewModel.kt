package com.deivon.fuellog.ui

import android.app.Application
import android.net.Uri
import android.net.http.HttpResponseCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deivon.fuellog.data.FuelDatabase
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.logic.Backup
import com.deivon.fuellog.logic.CatalogModel
import com.deivon.fuellog.logic.ModelCatalog
import com.deivon.fuellog.logic.Decimals
import com.deivon.fuellog.logic.FormError
import com.deivon.fuellog.logic.FormResult
import com.deivon.fuellog.logic.FuelSummary
import com.deivon.fuellog.logic.GeoPoint
import com.deivon.fuellog.logic.RefuelForm
import com.deivon.fuellog.logic.RefuelValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Screen state and actions. Database work runs on IO; statistics on Default. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = FuelDatabase(app)
    val settings = AppSettings(app)

    var vehicles by mutableStateOf(emptyList<Vehicle>())
        private set
    /** Every refuelling of every vehicle, newest first. */
    private var all by mutableStateOf(emptyList<Refuel>())
    /** Selected vehicle's refuellings, newest first. */
    var refuels by mutableStateOf(emptyList<Refuel>())
        private set
    var summary by mutableStateOf(FuelSummary.EMPTY)
        private set
    var loaded by mutableStateOf(false)
        private set

    /** Derived once per data change instead of on every recomposition. */
    var recentStations by mutableStateOf(emptyList<Pair<String, GeoPoint?>>())
        private set
    var savedLocations by mutableStateOf(emptyList<GeoPoint>())
        private set
    var refuelCounts by mutableStateOf(emptyMap<Long, Int>())
        private set

    /** Brand/model catalogue from assets, loaded on first use. */
    var catalog by mutableStateOf(emptyList<CatalogModel>())
        private set

    val vehicle: Vehicle get() = vehicles.firstOrNull { it.id == settings.vehicleId } ?: vehicles.firstOrNull() ?: Vehicle()

    /** Add/edit forms currently open, or null. Kept here so they survive rotation and language changes. */
    var form by mutableStateOf<RefuelForm?>(null)
        private set
    var formErrors by mutableStateOf(emptySet<FormError>())
        private set
    var vehicleForm by mutableStateOf<Vehicle?>(null)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Disk cache for map tiles: platform API, no extra library. Opening it touches disk, so not on main.
            if (HttpResponseCache.getInstalled() == null) {
                runCatching { HttpResponseCache.install(File(app.cacheDir, "http"), 30L * 1024 * 1024) }
            }
        }
        reload()
    }

    private fun reload() = viewModelScope.launch {
        val (v, list) = withContext(Dispatchers.IO) {
            if (db.vehicles().isEmpty()) db.saveVehicle(Vehicle())
            db.vehicles() to db.refuels()
        }
        vehicles = v
        all = list
        withContext(Dispatchers.Default) {
            Triple(
                list.distinctBy { it.station }.take(4).map { r ->
                    r.station to list.firstOrNull { it.station == r.station && it.latitude != null }
                        ?.let { GeoPoint.of(it.latitude, it.longitude) }
                },
                list.mapNotNull { GeoPoint.of(it.latitude, it.longitude) }.distinct(),
                list.groupingBy { it.vehicleId }.eachCount(),
            )
        }.let { (stations, locations, counts) ->
            recentStations = stations
            savedLocations = locations
            refuelCounts = counts
        }
        recompute()
        loaded = true
    }

    fun loadCatalog() {
        if (catalog.isNotEmpty()) return
        viewModelScope.launch {
            catalog = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().assets.open("models_es.csv").bufferedReader().use { ModelCatalog.parse(it.readText()) }
                }.getOrDefault(emptyList())
            }
        }
    }

    private suspend fun recompute() {
        val id = vehicle.id
        val mine = all.filter { it.vehicleId == id }
        refuels = mine
        summary = withContext(Dispatchers.Default) { FuelSummary.of(mine) }
    }

    fun selectVehicle(id: Long) {
        settings.updateVehicle(id)
        viewModelScope.launch { recompute() }
    }

    // --- Refuellings -------------------------------------------------------------------------

    /** New form with sensible defaults: now, full tank, last price of this vehicle. */
    fun newRefuel(locale: Locale) {
        val last = refuels.firstOrNull()
        form = RefuelForm(
            timestamp = System.currentTimeMillis(),
            price = last?.let { Decimals.toInput(it.pricePerLitreMilli, 3, locale) } ?: "",
        )
        formErrors = emptySet()
    }

    fun edit(r: Refuel, locale: Locale) { form = RefuelForm.from(r, locale); formErrors = emptySet() }
    fun updateForm(f: RefuelForm) { form = f; if (formErrors.isNotEmpty()) formErrors = emptySet() }
    fun closeForm() { form = null; formErrors = emptySet() }

    fun saveForm() {
        val f = form ?: return
        when (val result = RefuelValidator.validate(f, refuels, vehicle.id)) {
            is FormResult.Invalid -> formErrors = result.errors
            is FormResult.Valid -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.save(result.refuel) }
                closeForm()
                reload()
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        withContext(Dispatchers.IO) { db.delete(id) }
        closeForm()
        reload()
    }

    // --- Vehicles ----------------------------------------------------------------------------

    fun editVehicle(v: Vehicle?) { vehicleForm = v ?: Vehicle() }
    fun closeVehicle() { vehicleForm = null }

    fun saveVehicle(v: Vehicle) = viewModelScope.launch {
        val id = withContext(Dispatchers.IO) { db.saveVehicle(v) }
        settings.updateVehicle(id)
        vehicleForm = null
        reload()
    }

    /** Deletes a vehicle and its refuellings. The last vehicle cannot be deleted. */
    fun deleteVehicle(id: Long) = viewModelScope.launch {
        if (vehicles.size <= 1) return@launch
        withContext(Dispatchers.IO) { db.deleteVehicle(id) }
        if (settings.vehicleId == id) settings.updateVehicle(vehicles.first { it.id != id }.id)
        vehicleForm = null
        reload()
    }

    fun refuelCount(vehicleId: Long) = refuelCounts[vehicleId] ?: 0

    // --- Backup ------------------------------------------------------------------------------

    suspend fun exportCsv(uri: Uri) = write(uri) { Backup.toCsv(vehicles, all) }
    suspend fun exportJson(uri: Uri) = write(uri) { Backup.toJson(vehicles, all) }

    /** Reads and validates a backup; nothing is changed until [restore]. Throws on unusable files. */
    suspend fun readBackup(uri: Uri): Backup.Parsed = withContext(Dispatchers.IO) {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            // A personal fuel log is far below 10 MB; refuse anything larger instead of exhausting memory.
            val bytes = input.readAtMost(MAX_BACKUP_BYTES + 1)
            if (bytes.size > MAX_BACKUP_BYTES) throw Backup.InvalidBackup("File too large")
            bytes.decodeToString()
        } ?: throw Backup.InvalidBackup("Cannot open file")
        Backup.parseJson(text)
    }

    fun restore(parsed: Backup.Parsed) = viewModelScope.launch {
        withContext(Dispatchers.IO) { db.replaceAll(parsed.vehicles) }
        settings.updateVehicle(-1) // falls back to the first restored vehicle
        reload()
    }

    /** Builds the text and writes it off the main thread. */
    private suspend fun write(uri: Uri, text: () -> String) = withContext(Dispatchers.IO) {
        val content = text()
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
            ?: error("Cannot write file")
    }

    override fun onCleared() {
        HttpResponseCache.getInstalled()?.flush()
        db.close()
    }

    private companion object { const val MAX_BACKUP_BYTES = 10 * 1024 * 1024 }
}

/** InputStream.readNBytes is API 33+; this reads at most [limit] bytes on any version. */
private fun java.io.InputStream.readAtMost(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < limit) {
        val n = read(buf, 0, minOf(buf.size, limit - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
