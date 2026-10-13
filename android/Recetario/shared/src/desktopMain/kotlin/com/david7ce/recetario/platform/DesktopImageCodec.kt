package com.david7ce.recetario.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.max

/**
 * Skia (the engine under Compose on desktop) decodes, scales and re-encodes images.
 * Known gap: EXIF rotation is not applied yet, so some phone photos can import sideways.
 */
class DesktopImageCodec : ImageCodec {
    override fun normalize(bytes: ByteArray): ByteArray? {
        val image = decodeOrNull(bytes) ?: return null
        val (w, h) = fitSize(image.width, image.height, MAX_STORED_SIDE)
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.clear(WHITE) // JPEG has no transparency: transparent areas become white, not black.
        surface.canvas.drawImageRect(image, Rect.makeWH(w.toFloat(), h.toFloat()))
        return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes
    }

    override fun decode(bytes: ByteArray, maxSize: Int): ImageBitmap? {
        val image = decodeOrNull(bytes) ?: return null
        val (w, h) = fitSize(image.width, image.height, maxSize)
        if (w == image.width && h == image.height) return image.toComposeImageBitmap()
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(image, Rect.makeWH(w.toFloat(), h.toFloat()))
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }

    private fun decodeOrNull(bytes: ByteArray): Image? = runCatching { Image.makeFromEncoded(bytes) }.getOrNull()

    private fun fitSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val f = maxSide.toFloat() / longest
        return (width * f).toInt().coerceAtLeast(1) to (height * f).toInt().coerceAtLeast(1)
    }

    private companion object {
        const val MAX_STORED_SIDE = 1600
        const val JPEG_QUALITY = 85
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}
