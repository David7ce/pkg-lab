package com.david7ce.calistenia

import com.david7ce.calistenia.logic.videoPathFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseVideosTest {

    @Test
    fun registeredExercisesGetTheirVideo() {
        assertEquals("files/exercise_videos/pull-up.mp4", videoPathFor("builtin:pull-up"))
        assertEquals("files/exercise_videos/push-up.mp4", videoPathFor("builtin:push-up"))
        assertEquals("files/exercise_videos/squat.mp4", videoPathFor("builtin:squat"))
        assertEquals("files/exercise_videos/mountain-climber.mp4",
            videoPathFor("builtin:mountain-climber"))
        assertEquals("files/exercise_videos/l-sit.mp4", videoPathFor("builtin:l-sit"))
    }

    @Test
    fun unknownBuiltinHasNoVideo() {
        assertNull(videoPathFor("builtin:not-an-exercise"))
    }

    @Test
    fun customExercisesNeverGetAVideo() {
        assertNull(videoPathFor("custom:abc123"))
    }
}
