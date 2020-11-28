package com.headsupwatchface

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import kotlin.time.TimeMark


/**
 * Calendar helpers
 */
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,                     // 0
        CalendarContract.Calendars.ACCOUNT_NAME,            // 1
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
        CalendarContract.Calendars.OWNER_ACCOUNT,            // 3
)
// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
private const val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
private const val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3


/**
 * Model class for a timeline and its content
 */
class Timeline (
        val resources: Resources,
        val contentResolver: ContentResolver, // for querying the calendar
        val context: Context,  // to check handle permissions
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

    /**
     * Querying the calendars from the device to update what is represented in the timeline
     */
    fun updateCalendar (){
        if (!checkPermissions(false))
            println("Can not update calendar data without permission")

        println("Timeline updating calendar data")

        // Construct Query
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "()"
        val selectionArgs: Array<String> = arrayOf()

        val cur: Cursor? = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)

        if(cur != null){
            while(cur.moveToNext()){
                // Get the field values
                val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
                val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
                val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
                val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
                println("found $calID $displayName ($accountName)")
            }
            cur.close()
        }
        else
            println("Calendar query returned no cursor")
    }

    /**
     * Check if the watch face has all permissions it needs for the timeline
     *
     * @param notify: Whether the user should be notified about missing permissions
     * @return: If all permissions were granted
     */
    fun checkPermissions(notify: Boolean): Boolean{
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_DENIED){
            if(notify)
                Toast.makeText(context.applicationContext, R.string.permission_calendar_missing,
                        Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}