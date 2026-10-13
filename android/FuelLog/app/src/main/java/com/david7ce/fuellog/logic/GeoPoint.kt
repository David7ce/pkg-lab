package com.david7ce.fuellog.logic

import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class GeoPoint(val lat: Double, val lon: Double) {
    /** "40.41680, -3.70380": dot decimals, the form maps apps and websites understand. */
    fun format(): String = String.format(Locale.US, "%.5f, %.5f", lat, lon)

    companion object {
        private val pair = Regex("""^\s*(-?\d{1,3}(?:[.,]\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:[.,]\d+)?)\s*$""")

        /** Parses "40.4168, -3.7038" (also ";" or space separated, comma decimals with ";"). Null if invalid. */
        fun parse(text: String): GeoPoint? {
            val t = text.trim()
            val m = pair.find(t) ?: Regex("""^\s*(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)\s*$""").find(t) ?: return null
            val lat = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val lon = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
            return of(lat, lon)
        }

        fun of(lat: Double?, lon: Double?): GeoPoint? =
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) GeoPoint(lat, lon) else null
    }
}

/**
 * Web Mercator maths for OpenStreetMap "slippy map" tiles (256 px squares, 2^zoom per side).
 * Coordinates are world pixels at a given (possibly fractional) zoom.
 */
object Mercator {
    const val TILE = 256
    const val MAX_LAT = 85.05112878

    private fun world(zoom: Double) = TILE * 2.0.pow(zoom)

    fun x(lon: Double, zoom: Double) = (lon + 180.0) / 360.0 * world(zoom)

    fun y(lat: Double, zoom: Double): Double {
        val r = Math.toRadians(lat.coerceIn(-MAX_LAT, MAX_LAT))
        return (1 - ln(tan(r) + 1 / cos(r)) / PI) / 2 * world(zoom)
    }

    fun lon(x: Double, zoom: Double) = x / world(zoom) * 360.0 - 180.0

    fun lat(y: Double, zoom: Double) = Math.toDegrees(atan(sinh(PI * (1 - 2 * y / world(zoom)))))
}
