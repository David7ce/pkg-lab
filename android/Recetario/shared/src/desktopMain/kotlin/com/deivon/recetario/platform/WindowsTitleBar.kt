package com.david7ce.recetario.platform

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference

// No Java API exposes the native window handle; dwmapi.dll (Windows only) is what actually paints
// the title bar. No-op on Linux/macOS (checked first, so dwmapi.dll is never even looked up there).
private interface Dwmapi : Library {
    fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: IntByReference, size: Int): Int
    companion object { val INSTANCE: Dwmapi by lazy { Native.load("dwmapi", Dwmapi::class.java) } }
}

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20

private val isWindows = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

/**
 * Matches the native title bar to the app's current theme (light/dark), same as Settings' theme
 * choice. Finds the window by its exact title text, since AWT/Swing has no public API for the
 * native handle.
 * ponytail: FindWindow-by-title misfires only if another top-level window shares the exact same
 * title text at the same time; upgrade to resolving the HWND straight from ComposeWindow's peer if
 * that ever causes a real collision.
 */
fun applyWindowsTitleBarTheme(windowTitle: String, dark: Boolean) {
    if (!isWindows) return
    runCatching {
        val hwnd = User32.INSTANCE.FindWindow(null, windowTitle) ?: return
        Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, IntByReference(if (dark) 1 else 0), 4)
    }
}
