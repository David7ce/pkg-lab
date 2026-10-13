package com.david7ce.recetario.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icons that only mean something inside this app (its domain). Generic UI icons live in
 * `SharedIcons` (android/shared); see the pkg-lab-icons skill.
 */
object AppIcons {
    private fun icon(name: String, path: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()

    val Dish by lazy {
        icon(
            "Dish",
            "M11,9H9V2H7v7H5V2H3v7c0,2.12 1.66,3.84 3.75,3.97V22h2.5v-9.03C11.34,12.84 13,11.12 13,9V2h-2V9zM16," +
                "6v8h2.5v8H21V2C18.24,2 16,4.24 16,6z",
        )
    }
}
