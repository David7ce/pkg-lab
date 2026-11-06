package com.david7ce.calistenia

import com.david7ce.calistenia.logic.videoPathFor
import kotlin.test.Test
import kotlin.test.assertNull

class ExerciseVideosTest {

    @Test
    fun noVideoYetForAnyExercise() {
        assertNull(videoPathFor("builtin:push-up"))
        assertNull(videoPathFor("builtin:jumping-jack"))
    }

    @Test
    fun customExercisesNeverGetAVideo() {
        assertNull(videoPathFor("custom:abc123"))
    }
}
