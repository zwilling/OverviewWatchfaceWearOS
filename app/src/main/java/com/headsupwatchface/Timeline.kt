package com.headsupwatchface

import android.content.res.Resources
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import kotlin.time.TimeMark


/**
 * Model class for a timeline and its content
 */
class Timeline (
        val resources: Resources,
) {
    private val mTimeScope: Duration = Duration.ofHours(
            resources.getInteger(R.integer.timeline_scope).toLong())

    /**
     * Hour marks to be shown on the timeline to show the scale
     */
    val hourMarks: List<LocalDateTime>
        get() {
            val hourMarks = mutableListOf<LocalDateTime>()
            val timeNow = LocalDateTime.now()
            var nextMark = timeNow.truncatedTo(ChronoUnit.HOURS)
            nextMark = nextMark.plusHours(1)

            // Add a mark for every hour in the scope
            while (Duration.between(timeNow, nextMark) < mTimeScope){
                hourMarks += nextMark
                nextMark = nextMark.plusHours(1)
            }
            return hourMarks
        }
}