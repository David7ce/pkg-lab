package com.david7ce.recetario.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun releaseJson(assetNames: List<String>) = """
    {"html_url": "https://github.com/David7ce/pkg-lab/releases/tag/android-Recetario-latest",
     "assets": [${assetNames.joinToString(",") { "{\"name\": \"$it\"}" }}]}
""".trimIndent()

class UpdateCheckTest {
    @Test
    fun newerBuildNumberIsAnUpdate() {
        val json = releaseJson(listOf("Recetario-android-1.0.42-arm64-v8a.apk", "Recetario-android-1.0.42-universal.apk"))
        val info = parseUpdateCheck(json, currentVersion = "1.0.7")
        assertEquals(UpdateInfo("1.0.42", "https://github.com/David7ce/pkg-lab/releases/tag/android-Recetario-latest"), info)
    }

    @Test
    fun desktopInstallerNameAlsoParses() {
        val json = releaseJson(listOf("Recetario-1.0.42.msi", "Recetario-linux-x64.tar.gz", "Recetario-1.0.42.dmg"))
        assertEquals("1.0.42", parseUpdateCheck(json, currentVersion = "1.0.1")?.version)
    }

    @Test
    fun sameOrOlderVersionIsNotAnUpdate() {
        val json = releaseJson(listOf("Recetario-android-1.0.42-universal.apk"))
        assertNull(parseUpdateCheck(json, currentVersion = "1.0.42"))
        assertNull(parseUpdateCheck(json, currentVersion = "1.0.99"))
    }

    @Test
    fun noParseableAssetIsNoUpdate() {
        val json = releaseJson(listOf("Recetario-linux-x64.tar.gz"))
        assertNull(parseUpdateCheck(json, currentVersion = "1.0.1"))
    }
}
