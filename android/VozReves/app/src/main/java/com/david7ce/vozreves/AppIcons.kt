package com.david7ce.vozreves

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

    val Mic by lazy {
        icon(
            "Mic",
            "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 " +
                "-2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.48 6,-3.3 6," +
                "-6.72h-1.7z",
        )
    }
    val PlayReverse by lazy {
        icon(
            "PlayReverse",
            "M16,5v14l-11,-7z",
        )
    }
    val Quote by lazy {
        icon(
            "Quote",
            "M6,17h3l2,-4V7H5v6h3zM14,17h3l2,-4V7h-6v6h3z",
        )
    }
}
