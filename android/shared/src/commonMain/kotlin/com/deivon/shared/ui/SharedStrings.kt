package com.deivon.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Textos comunes que usan los componentes. Cada app los rellena con sus recursos traducidos
 * (CompositionLocalProvider) en su raíz. Los valores por defecto son inglés, por si falta alguno.
 */
data class SharedStrings(
    val cancel: String,
    val back: String,
    val close: String,
    val delete: String,
    val selectAll: String,
)

val LocalSharedStrings = staticCompositionLocalOf {
    SharedStrings(cancel = "Cancel", back = "Back", close = "Close", delete = "Delete", selectAll = "Select all")
}
