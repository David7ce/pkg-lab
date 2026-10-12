package com.deivon.fuellog.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deivon.fuellog.data.FuelDatabase
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.logic.Backup
import com.deivon.fuellog.logic.Decimals
import com.deivon.fuellog.logic.FormError
import com.deivon.fuellog.logic.FormResult
import com.deivon.fuellog.logic.FuelSummary
import com.deivon.fuellog.logic.RefuelForm
import com.deivon.fuellog.logic.RefuelValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Screen state and actions. Database work runs on IO; statistics on Default. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = FuelDatabase(app)
    val settings = AppSettings(app)

    var vehicle by mutableStateOf(Vehicle())
        private set
    /** Newest first. */
    var refuels by mutableStateOf(emptyList<Refuel>())
        private set
    var summary by mutableStateOf(FuelSummary.EMPTY)
        private set
    var loaded by mutableStateOf(false)
        private set

    /** Add/edit form currently open, or null. Lives here so it survives rotation. */
    var form by mutableStateOf<RefuelForm?>(null)
        private set
    var formErrors by mutableStateOf(emptySet<FormError>())
        private set

    init { reload() }

    private fun reload() = viewModelScope.launch {
        val (v, list) = withContext(Dispatchers.IO) { db.vehicle() to db.refuels() }
        vehicle = v
        refuels = list
        summary = withContext(Dispatchers.Default) { FuelSummary.of(list) }
        loaded = true
    }

    /** New form with sensible defaults: now, full tank, last price (prices change slowly). */
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

    fun saveVehicle(v: Vehicle) = viewModelScope.launch {
        withContext(Dispatchers.IO) { db.saveVehicle(v) }
        reload()
    }

    /** Recent distinct station names, for one-tap entry. */
    fun recentStations(limit: Int = 4): List<String> = refuels.map { it.station }.distinct().take(limit)

    suspend fun exportCsv(uri: Uri) = write(uri, Backup.toCsv(refuels))
    suspend fun exportJson(uri: Uri) = write(uri, Backup.toJson(vehicle, refuels))

    /** Reads and validates a backup; nothing is changed until [restore]. Throws on unusable files. */
    suspend fun readBackup(uri: Uri): Backup.Parsed = withContext(Dispatchers.IO) {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            // A personal fuel log is far below 10 MB; refuse anything larger instead of exhausting memory.
            val bytes = input.readNBytesCompat(MAX_BACKUP_BYTES + 1)
            if (bytes.size > MAX_BACKUP_BYTES) throw Backup.InvalidBackup("File too large")
            bytes.decodeToString()
        } ?: throw Backup.InvalidBackup("Cannot open file")
        Backup.parseJson(text)
    }

    fun restore(parsed: Backup.Parsed) = viewModelScope.launch {
        withContext(Dispatchers.IO) { db.replaceAll(parsed.vehicle.copy(id = vehicle.id), parsed.refuels) }
        reload()
    }

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
            ?: error("Cannot write file")
    }

    override fun onCleared() = db.close()

    private companion object { const val MAX_BACKUP_BYTES = 10 * 1024 * 1024 }
}

/** InputStream.readNBytes is API 33+; this reads at most [limit] bytes on any version. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < limit) {
        val n = read(buf, 0, minOf(buf.size, limit - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
