package com.david7ce.fuellog.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Catches the regressions that actually happen: a crash on launch, a broken navigation bar. */
@RunWith(RobolectricTestRunner::class)
class MainActivitySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesWithAllThreeTabs() {
        compose.onNodeWithText("Home").assertExists()
        compose.onNodeWithText("History").assertExists()
        compose.onNodeWithText("Settings").assertExists()
    }
}
