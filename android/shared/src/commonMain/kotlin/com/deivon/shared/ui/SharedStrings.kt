package com.david7ce.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Common text used by the components. Each app fills it in with its own translated resources
 * (CompositionLocalProvider) at its root. The defaults are English, in case one is missing.
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
