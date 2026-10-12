package com.david7ce.recetario.logic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val MAX_RELEASE_JSON_BYTES = 256 * 1024

data class UpdateInfo(val version: String, val url: String)

private val VERSION_IN_ASSET_NAME = Regex("""-(\d+\.\d+\.\d+)[-.]""")

/**
 * Parses the JSON from GitHub's "get a release by tag" endpoint and compares the version baked
 * into its asset filenames (e.g. Recetario-android-1.0.42-arm64-v8a.apk, Recetario-1.0.42.msi)
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

/** "1.0.42" vs "1.0.7": compares the trailing CI run number numerically, not lexically. */
private fun isNewer(remote: String, current: String): Boolean {
    val r = remote.substringAfterLast('.').toIntOrNull() ?: return false
    val c = current.substringAfterLast('.').toIntOrNull() ?: return false
    return r > c
}
