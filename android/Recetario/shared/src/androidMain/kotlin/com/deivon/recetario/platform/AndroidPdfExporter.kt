package com.deivon.recetario.platform

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.deivon.recetario.logic.Quantities
import com.deivon.recetario.model.Recipe
import com.deivon.recetario.ui.Strings
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** A4 PDF with the platform's own PdfDocument: no library, text stays selectable. */
class AndroidPdfExporter : PdfExporter {
    override fun export(recipes: List<Recipe>, image: (String) -> ByteArray?, strings: Strings, cookbookTitle: String?): ByteArray {
        val doc = PdfDocument()
        try {
            val writer = PageWriter(doc)
            if (cookbookTitle != null) writer.cover(cookbookTitle, strings.pdfCount(recipes.size))
            recipes.forEach { writer.recipe(it, image, strings) }
            writer.finish()
            return ByteArrayOutputStream().also(doc::writeTo).toByteArray()
        } finally {
            doc.close()
        }
    }
}

/** Lays text out line by line, starting a new page whenever the next line does not fit. */
private class PageWriter(private val doc: PdfDocument) {
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f

    private val title = paint(22f, bold = true)
    private val heading = paint(14f, bold = true, color = 0xFF9A4521.toInt())
    private val body = paint(11f)
    private val muted = paint(10f, color = 0xFF6B5B55.toInt())

    fun cover(text: String, subtitle: String) {
        newPage()
        y = PAGE_H / 3f
        write(text, paint(32f, bold = true), after = 12f)
        write(subtitle, muted, after = 0f)
    }

    fun recipe(r: Recipe, image: (String) -> ByteArray?, s: Strings) {
        newPage()
        r.imageFile?.let(image)?.let(::photo)
        write(r.title, title, after = 8f)
        val meta = listOfNotNull(
            r.servings?.let(s::pdfServings),
            r.prepMinutes?.let { "${s.prep}: ${s.duration(it)}" },
            r.cookMinutes?.let { "${s.cook}: ${s.duration(it)}" },
        ).joinToString("  ·  ")
        if (meta.isNotEmpty()) write(meta, muted, after = 4f)
        if (r.tags.isNotEmpty()) write(r.tags.joinToString(", "), muted, after = 4f)

        write(s.ingredients, heading, before = 14f, after = 6f)
        r.ingredients.forEach { ing ->
            val amount = listOfNotNull(ing.quantity?.let { Quantities.format(it, s.decimalSeparator) }, ing.unit).joinToString(" ")
            val line = listOf(amount, ing.name).filter { it.isNotEmpty() }.joinToString(" ") + (ing.note?.let { ", $it" } ?: "")
            write("•  $line", body, after = 3f)
        }

        write(s.steps, heading, before = 14f, after = 6f)
        r.steps.forEachIndexed { i, step -> write("${i + 1}.  $step", body, after = 6f) }

        r.notes?.let {
            write(s.notes, heading, before = 14f, after = 6f)
            it.split("\n").forEach { line -> write(line.ifBlank { " " }, body, after = 2f) }
        }

        r.sourceUrl?.let { write("${s.source}: $it", muted, before = 12f, after = 0f) }
    }

    fun finish() {
        page?.let(doc::finishPage)
        page = null
    }

    private fun newPage() {
        finish()
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        y = MARGIN
    }

    private fun write(text: String, paint: TextPaint, before: Float = 0f, after: Float) {
        y += before
        val width = PAGE_W - 2 * MARGIN.toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width).setLineSpacing(0f, 1.15f).build()
        for (line in 0 until layout.lineCount) {
            val top = layout.getLineTop(line)
            val bottom = layout.getLineBottom(line)
            if (page == null || y + (bottom - top) > PAGE_H - MARGIN) newPage()
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN, y - top)
            canvas.clipRect(0, top, width, bottom)
            layout.draw(canvas)
            canvas.restore()
            y += bottom - top
        }
        y += after
    }

    private fun photo(bytes: ByteArray) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return // not decodable: the recipe prints without its photo
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1200) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        val maxW = PAGE_W - 2 * MARGIN
        val scale = min(maxW / bitmap.width, MAX_PHOTO_H / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        page!!.canvas.drawBitmap(bitmap, null, RectF(MARGIN, y, MARGIN + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        y += h + 16f
        bitmap.recycle()
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private companion object {
        const val PAGE_W = 595 // A4 in points
        const val PAGE_H = 842
        const val MARGIN = 48f
        const val MAX_PHOTO_H = 280f
    }
}
