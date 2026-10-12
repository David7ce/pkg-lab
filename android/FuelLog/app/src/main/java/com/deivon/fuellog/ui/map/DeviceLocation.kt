package com.deivon.fuellog.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.deivon.fuellog.logic.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * One-shot device position with the platform LocationManager (no Google Play services).
 * Tries a fresh fix on Android 11+, otherwise the most recent known position. Never tracks.
 * Callers must hold a location permission.
 */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): GeoPoint? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        for (provider in providers) {
            val fresh = withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine { cont ->
                    val cancel = android.os.CancellationSignal()
                    cont.invokeOnCancellation { cancel.cancel() }
                    lm.getCurrentLocation(provider, cancel, context.mainExecutor) { cont.resume(it) }
                }
            }
            if (fresh != null) return GeoPoint(fresh.latitude, fresh.longitude)
        }
    }
    return (providers + LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { GeoPoint(it.latitude, it.longitude) }
}
