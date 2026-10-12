package com.david7ce.vozreves

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Convierte una frase en audio con el motor de voz del sistema (sin librerías ni red propias):
 * lo escribe a un archivo temporal, lo lee como PCM y lo borra. Devuelve null si no hay motor o idioma.
 */
suspend fun synthesizePhrase(ctx: Context, text: String, locale: Locale): ShortArray? {
    val file = File(ctx.cacheDir, "tts_phrase.wav").also { it.delete() }
    var tts: TextToSpeech? = null
    try {
        val ready = suspendCancellableCoroutine { c ->
            tts = TextToSpeech(ctx.applicationContext) { if (c.isActive) c.resume(it == TextToSpeech.SUCCESS) }
        }
        val engine = tts?.takeIf { ready } ?: return null
        val lang = engine.setLanguage(locale)
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) return null
        val done = suspendCancellableCoroutine { c ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { if (c.isActive) c.resume(true) }
                @Deprecated("Required by the abstract class")
                override fun onError(utteranceId: String?) { if (c.isActive) c.resume(false) }
                override fun onError(utteranceId: String?, errorCode: Int) { if (c.isActive) c.resume(false) }
            })
            if (engine.synthesizeToFile(text, null, file, "phrase") != TextToSpeech.SUCCESS && c.isActive) c.resume(false)
        }
        if (!done) return null
        return withContext(Dispatchers.IO) { decodeWav(file.readBytes()) }.takeIf { it.isNotEmpty() }
    } finally {
        tts?.shutdown()
        file.delete()
    }
}
