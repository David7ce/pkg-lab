package com.david7ce.sleepcycles.logic

import com.david7ce.sleepcycles.MapStore
import kotlin.test.Test
import kotlin.test.assertEquals

class UserSettingsTest {
    @Test
    fun emptyStoreGivesTheDefaults() {
        val s = MapStore().load()
        assertEquals(UserSettings(), s)
        assertEquals(Mode.Wake, s.mode)
        assertEquals(7 * 60, s.wakeTime)
        assertEquals(23 * 60, s.bedTime)
        assertEquals(AgeRange.Adult, s.age)
        assertEquals(15, s.latencyMinutes)
        assertEquals(ClockFormat.H24, s.format)
    }

    @Test
    fun saveThenLoadRoundTrips() {
        val store = MapStore()
        val saved = UserSettings(
            mode = Mode.Bed, wakeTime = 6 * 60 + 30, bedTime = 22 * 60 + 10, age = AgeRange.Senior,
            latencyMinutes = 40, format = ClockFormat.H12, theme = ThemeMode.Dark, language = AppLanguage.De,
        )
        store.save(saved)
        assertEquals(saved, store.load())
    }

    @Test
    fun corruptValuesFallBackFieldByField() {
        val store = MapStore(
            mutableMapOf(
                "mode" to "Sideways", "wakeTime" to "soon", "bedTime" to "1440", "age" to "Martian",
                "latency" to "999", "format" to "H12", "theme" to "", "language" to "Klingon",
            )
        )
        val s = store.load()
        val d = UserSettings()
        assertEquals(d.mode, s.mode)
        assertEquals(d.wakeTime, s.wakeTime)
        assertEquals(d.bedTime, s.bedTime)
        assertEquals(d.age, s.age)
        assertEquals(120, s.latencyMinutes)
        assertEquals(ClockFormat.H12, s.format)
        assertEquals(d.theme, s.theme)
        assertEquals(d.language, s.language)
    }

    @Test
    fun negativeTimeIsRejected() {
        assertEquals(UserSettings().wakeTime, MapStore(mutableMapOf("wakeTime" to "-5")).load().wakeTime)
    }
}
