package com.deivon.fuellog.ui.map

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.logic.GeoPoint
import com.deivon.fuellog.logic.Mercator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

const val MIN_ZOOM = 2.0
const val MAX_ZOOM = 19.0

/**
 * Downloads OpenStreetMap tiles. Memory: an LRU of decoded bitmaps. Disk: the platform
 * HttpResponseCache installed by the app, so tiles seen before load offline.
 * At most 4 downloads at once, as the OSM tile usage policy asks for modest use.
 */
private class TileLoader(private val scope: CoroutineScope) {
    private val memory = LruCache<String, ImageBitmap>(96)
    private val pending = HashSet<String>() // main thread only
    private val failed = HashSet<String>()  // not retried while this map is open
    private val downloads = Semaphore(4)

    /** Bumped whenever a tile arrives, so the Canvas redraws. */
    var version by mutableIntStateOf(0)
        private set

    fun get(z: Int, x: Int, y: Int): ImageBitmap? = memory.get("$z/$x/$y")

    fun request(z: Int, x: Int, y: Int) {
        val key = "$z/$x/$y"
        if (memory.get(key) != null || key in pending || key in failed) return
        pending += key
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { downloads.withPermit { download(key) } }
            pending -= key
            if (bitmap != null) { memory.put(key, bitmap); version++ } else failed += key
        }
    }

    private fun download(key: String): ImageBitmap? = runCatching {
        val c = URL("https://tile.openstreetmap.org/$key.png").openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.useCaches = true
        try { c.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap() } finally { c.disconnect() }
    }.getOrNull()

    companion object { const val USER_AGENT = "FuelLog/1.0 (Android; personal fuel log; github.com/David7ce/pkg-lab)" }
}

/**
 * Minimal pannable/zoomable map. [center] and [zoom] are owned by the caller; gestures report
 * changes through [onMove]. [pins] are drawn as small circles (e.g. stations used before).
 */
@Composable
fun TileMap(
    center: GeoPoint,
    zoom: Double,
    onMove: (GeoPoint, Double) -> Unit,
    pins: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tiles = remember { TileLoader(scope) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentCenter by rememberUpdatedState(center)
    val currentZoom by rememberUpdatedState(zoom)
    val currentOnMove by rememberUpdatedState(onMove)

    // Tiles come from the nearest integer zoom level and are scaled for fractional zoom.
    val level = zoom.roundToInt()
    val scale = 2.0.pow(zoom - level)
    val cx = Mercator.x(center.lon, level.toDouble())
    val cy = Mercator.y(center.lat, level.toDouble())
    val tileCount = 1 shl level
    val halfW = size.width / 2.0 / scale
    val halfH = size.height / 2.0 / scale
    val xs = floor((cx - halfW) / Mercator.TILE).toInt()..floor((cx + halfW) / Mercator.TILE).toInt()
    val ys = (floor((cy - halfH) / Mercator.TILE).toInt().coerceAtLeast(0))..
        (floor((cy + halfH) / Mercator.TILE).toInt().coerceAtMost(tileCount - 1))
    fun wrap(x: Int) = ((x % tileCount) + tileCount) % tileCount

    SideEffect { if (size != IntSize.Zero) for (x in xs) for (y in ys) tiles.request(level, wrap(x), y) }

    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    val pinColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val z = (currentZoom + log2(zoomChange.toDouble())).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val c = currentCenter
                    // Pan in world pixels at the current zoom, then keep that centre at the new zoom.
                    val x = Mercator.x(c.lon, currentZoom) - pan.x
                    val y = Mercator.y(c.lat, currentZoom) - pan.y
                    val lat = Mercator.lat(y, currentZoom).coerceIn(-Mercator.MAX_LAT, Mercator.MAX_LAT)
                    var lon = Mercator.lon(x, currentZoom)
                    lon = ((lon + 540.0) % 360.0) - 180.0
                    currentOnMove(GeoPoint(lat, lon), z)
                }
            }
    ) {
        tiles.version // read so new tiles trigger a redraw
        val tileSize = (Mercator.TILE * scale).toFloat()
        val px = (tileSize + 0.5f).toInt() + 1 // +1 hides seams between scaled tiles
        for (x in xs) for (y in ys) {
            val left = ((x * Mercator.TILE - cx) * scale + size.width / 2.0).toFloat()
            val top = ((y * Mercator.TILE - cy) * scale + size.height / 2.0).toFloat()
            val image = tiles.get(level, wrap(x), y)
            if (image != null) {
                drawImage(image, dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(px, px))
            } else {
                drawRect(placeholder, Offset(left, top), androidx.compose.ui.geometry.Size(tileSize, tileSize))
            }
        }
        val radius = 6.dp.toPx()
        pins.forEach { p ->
            val x = ((Mercator.x(p.lon, level.toDouble()) - cx) * scale + size.width / 2.0).toFloat()
            val y = ((Mercator.y(p.lat, level.toDouble()) - cy) * scale + size.height / 2.0).toFloat()
            drawCircle(pinColor.copy(alpha = 0.35f), radius, Offset(x, y))
            drawCircle(pinColor, radius, Offset(x, y), style = Stroke(2.dp.toPx()))
        }
    }
}
