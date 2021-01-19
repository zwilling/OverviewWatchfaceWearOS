package com.headsupwatchface

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit





/**
 * Model class for a timeline and its content
 */
class Timeline(
    val resources: Resources,
    private val contentResolver: ContentResolver, // for querying the calendar
    private val context: Context,  // to check handle permissions
    private val mSharedPreferences: SharedPreferences,
) {
    private val mTimeScope: Duration = Duration.ofHours(
            resources.getInteger(R.integer.timeline_scope).toLong())

    val weather = Weather(context, mSharedPreferences, resources)
    val meetingCalendar = MeetingCalendar(context, resources, contentResolver, mTimeScope)

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