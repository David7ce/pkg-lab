package com.david7ce.calistenia.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icons that only mean something in a training log. Generic UI icons live in SharedIcons
 * (android/shared); rules in the pkg-lab-icons skill.
 */
object AppIcons {
    private fun icon(name: String, path: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()

    /** A dumbbell drawn from simple rectangles: plates, inner plates and the bar. */
    val Dumbbell by lazy {
        icon(
            "Dumbbell",
            "M4,9h2v6H4zM6,10.5h1v3H6zM9,11h6v2H9zM17,10.5h1v3h-1zM18,9h2v6h-2z",
        )
    }

    /** A medal for a finished session summary. */
    val Medal by lazy {
        icon(
            "Medal",
            "M12,12c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -2.24,-5 -5,-5zM12,20c-1.66,0 " +
                "-3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zM7.5,2L5,7h3l2.5,-5zM16.5,2L19,7h-3l-2.5,-5z",
        )
    }
}
