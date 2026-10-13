package com.david7ce.vozreves

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Catches the regressions that actually happen: a crash on launch, a broken toolbar. */
@RunWith(RobolectricTestRunner::class)
class MainActivitySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesWithTitleAndNavigationIcons() {
        compose.onNodeWithText("Reverse Voice").assertExists()
        compose.onNodeWithContentDescription("Phrases").assertExists()
        compose.onNodeWithContentDescription("History").assertExists()
        compose.onNodeWithContentDescription("Settings").assertExists()
    }
}
