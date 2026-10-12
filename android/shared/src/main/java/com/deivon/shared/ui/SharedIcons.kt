package com.deivon.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Iconos que no trae material-icons-core. Mismo estilo que el resto de la app (24 dp, trazo lleno). */
object SharedIcons {
    private fun icon(name: String, path: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()

    val SelectAll = icon(
        "SelectAll",
        "M3,5h2L5,3c-1.1,0 -2,0.9 -2,2zM3,13h2v-2L3,11v2zM7,21h2v-2L7,19v2zM3,9h2L5,7L3,7v2zM13,3h-2v2h2L13,3z" +
            "M19,3v2h2c0,-1.1 -0.9,-2 -2,-2zM5,21v-2L3,19c0,1.1 0.9,2 2,2zM3,17h2v-2L3,15v2zM9,3L7,3v2h2L9,3z" +
            "M11,21h2v-2h-2v2zM19,13h2v-2h-2v2zM19,21c1.1,0 2,-0.9 2,-2h-2v2zM19,9h2L21,7h-2v2zM19,17h2v-2h-2v2z" +
            "M15,21h2v-2h-2v2zM15,5h2L17,3h-2v2zM7,17h10L17,7L7,7v10zM9,9h6v6L9,15L9,9z"
    )
}
