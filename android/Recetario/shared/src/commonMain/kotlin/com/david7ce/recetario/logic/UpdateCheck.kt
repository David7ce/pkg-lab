package com.david7ce.recetario.logic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val MAX_RELEASE_JSON_BYTES = 256 * 1024

data class UpdateInfo(val version: String, val url: String)

// Matches a run of 2+ dot-separated numbers: "1.0" (hand-set Android versionName) and
// "1.0.42" (CI-run-number desktop packageVersion) alike.
private val VERSION_IN_ASSET_NAME = Regex("""-(\d+(?:\.\d+)+)[-.]""")

/**
 * Parses the JSON from GitHub's "get a release by tag" endpoint and compares the version baked
 * into its asset filenames (e.g. Recetario-android-1.0-arm64-v8a.apk, Recetario-1.0.42.msi)
 * against [currentVersion]. Null when nothing is parseable or nothing is newer.
 */
fun parseUpdateCheck(releaseJson: String, currentVersion: String): UpdateInfo? {
    val release = Json.parseToJsonElement(releaseJson).jsonObject
    val assets = release["assets"]?.jsonArray.orEmpty()
    val version = assets.firstNotNullOfOrNull { asset ->
        val name = asset.jsonObject["name"]?.jsonPrimitive?.content ?: return@firstNotNullOfOrNull null
        VERSION_IN_ASSET_NAME.find(name)?.groupValues?.get(1)
    } ?: return null
    if (!isNewer(version, currentVersion)) return null
    val htmlUrl = release["html_url"]?.jsonPrimitive?.content ?: return null
    return UpdateInfo(version, htmlUrl)
}

/** Numeric, segment-by-segment comparison ("1.10" > "1.9", "2.0" > "1.9"), not lexical or last-segment-only. */
private fun isNewer(remote: String, current: String): Boolean {
    val r = remote.split('.').map { it.toIntOrNull() ?: return false }
    val c = current.split('.').map { it.toIntOrNull() ?: return false }
    for (i in 0 until maxOf(r.size, c.size)) {
        val rv = r.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (rv != cv) return rv > cv
    }
    return false
}
