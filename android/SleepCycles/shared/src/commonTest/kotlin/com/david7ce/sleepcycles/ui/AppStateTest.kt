package com.david7ce.sleepcycles.ui

import com.david7ce.sleepcycles.FakeClock
import com.david7ce.sleepcycles.MapStore
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.load
import com.david7ce.sleepcycles.platform.Services
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStateTest {
    private fun state(store: MapStore = MapStore(), clock: FakeClock = FakeClock(), language: String = "en") =
        AppState(Services(store, clock), language, "1.0")

    @Test
    fun startsWithTheDefaults() {
        val s = state()
        assertEquals(Mode.Wake, s.mode)
        assertEquals(7 * 60, s.anchor)
        assertEquals(6, s.suggestions.size)
        assertEquals(Screen.Main, s.screen)
        assertFalse(s.showHelp)
    }

    @Test
    fun theAnchorBelongsToTheCurrentMode() {
        val s = state()
        s.setAnchor(6 * 60)
        assertEquals(6 * 60, s.wakeTime)
        assertEquals(23 * 60, s.bedTime)
        s.setMode(Mode.Bed)
        assertEquals(23 * 60, s.anchor)
        s.setAnchor(22 * 60)
        assertEquals(22 * 60, s.bedTime)
        assertEquals(6 * 60, s.wakeTime)
    }

    @Test
    fun sleepNowSwitchesToBedModeAtTheCurrentTimeAndPersists() {
        val store = MapStore()
        val s = state(store, FakeClock(23 * 60 + 59))
        s.sleepNow()
        s.sleepNow()
        assertEquals(Mode.Bed, s.mode)
        assertEquals(23 * 60 + 59, s.bedTime)
        assertEquals(23 * 60 + 59, store.load().bedTime)
        assertEquals(Mode.Bed, store.load().mode)
        assertTrue(s.suggestions.all { it.timeOfDay in 0..1439 })
    }

    @Test
    fun changesSurviveARestart() {
        val store = MapStore()
        val first = state(store)
        first.setAge(AgeRange.Teen)
        first.setLatency(30)
        first.setFormat(ClockFormat.H12)
        val second = state(store)
        assertEquals(AgeRange.Teen, second.age)
        assertEquals(30, second.latencyMinutes)
        assertEquals(ClockFormat.H12, second.format)
    }

    @Test
    fun latencyIsClamped() {
        val s = state()
        s.setLatency(5000)
        assertEquals(120, s.latencyMinutes)
        s.setLatency(-3)
        assertEquals(0, s.latencyMinutes)
    }

    @Test
    fun backClosesHelpFirstThenSettings() {
        val s = state()
        s.openSettings()
        s.openHelp()
        assertTrue(s.canGoBack)
        s.back()
        assertFalse(s.showHelp)
        assertEquals(Screen.Settings, s.screen)
        s.back()
        assertEquals(Screen.Main, s.screen)
        assertFalse(s.canGoBack)
    }
}
