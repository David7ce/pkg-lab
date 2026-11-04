package com.david7ce.calistenia.model

/**
 * A workout planned for a future (or any) day: the one thing history cannot know. Past days are always
 * read from the sessions themselves, never copied here.
 */
data class PlannedWorkout(
    val id: String,
    /** Local calendar date, ISO "2026-11-04": no time, no timezone. One plan per day. */
    val day: String,
    /** Null when the plan is "something, I'll decide", or when the routine was deleted. */
    val routineId: String?,
    /** Snapshot of the routine name; blank for "something, I'll decide". */
    val routineName: String,
    val note: String,
) {
    /** The plan names a routine that no longer exists: shown greyed, still readable. */
    val routineDeleted: Boolean get() = routineId == null && routineName.isNotBlank()
}
