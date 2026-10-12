package com.deivon.recetario.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset

/** Plain HttpURLConnection (Android and desktop JVM): one GET per import, no need for an HTTP library. */
class HttpWebClient(private val userAgent: String) : WebClient {
    override suspend fun getText(url: String, maxBytes: Int): String = withContext(Dispatchers.IO) {
        val (bytes, contentType) = fetch(url, maxBytes)
        String(bytes, charsetOf(contentType))
    }

    override suspend fun getBytes(url: String, maxBytes: Int): ByteArray = withContext(Dispatchers.IO) {
        fetch(url, maxBytes).first
    }

    private fun fetch(start: String, maxBytes: Int): Pair<ByteArray, String?> {
        var current = start
        repeat(MAX_REDIRECTS + 1) {
            val url = try {
                URL(current)
            } catch (e: MalformedURLException) {
                throw WebException(WebException.Kind.InvalidUrl, cause = e)
            }
            if (url.protocol != "https" && url.protocol != "http") throw WebException(WebException.Kind.InvalidUrl)
            var conn: HttpURLConnection? = null
            try {
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false // followed below, including http → https
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("User-Agent", userAgent)
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,image/*;q=0.9,*/*;q=0.8")
                val code = conn.responseCode
                when (code) {
                    in 200..299 -> {
                        if (conn.contentLengthLong > maxBytes) throw WebException(WebException.Kind.TooLarge)
                        return readLimited(conn.inputStream, maxBytes) to conn.contentType
                    }
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location") ?: throw WebException(WebException.Kind.Http, code)
                        current = URL(url, location).toString()
                    }
                    else -> throw WebException(WebException.Kind.Http, code)
                }
            } catch (e: IOException) {
                throw WebException(WebException.Kind.Network, cause = e)
            } finally {
                conn?.disconnect()
            }
        }
        throw WebException(WebException.Kind.Network) // redirect loop
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray = input.use { stream ->
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(32 * 1024)
        var n = stream.read(chunk)
        while (n >= 0) {
            if (out.size() + n > maxBytes) throw WebException(WebException.Kind.TooLarge)
            out.write(chunk, 0, n)
            n = stream.read(chunk)
        }
        out.toByteArray()
    }

    private fun charsetOf(contentType: String?): Charset {
        val name = contentType?.let { Regex("charset=\"?([^;\"\\s]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        return name?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
