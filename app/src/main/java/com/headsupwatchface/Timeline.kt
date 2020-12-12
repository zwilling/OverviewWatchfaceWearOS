package com.headsupwatchface

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.support.wearable.provider.WearableCalendarContract
import android.widget.Toast
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


/**
 * Calendar helpers
 */
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Events._ID,                            // 0
        CalendarContract.Events.TITLE,                          // 1
        CalendarContract.Events.CALENDAR_COLOR,          // 2
        CalendarContract.Instances.BEGIN,                       // 3
        CalendarContract.Instances.END,                         // 4
)
// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_TITLE_INDEX: Int = 1
private const val PROJECTION_CALENDAR_COLOR_INDEX: Int = 2
private const val PROJECTION_BEGIN_INDEX: Int = 3
private const val PROJECTION_END_INDEX: Int = 4


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
    var mCalendarEvents: MutableList<Event> = mutableListOf()

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
        val uriBuilder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon()
        val uri: Uri = uriBuilder.build()
        // ToDo: Handle all day events somehow
        val selection = "(" +
                // Only show events that have not ended yet
                "(${EVENT_PROJECTION[PROJECTION_END_INDEX]} > ?)" +
                " AND " +
                // Only show events that begin before the end of the time scope
                "(${EVENT_PROJECTION[PROJECTION_BEGIN_INDEX]} < ?)" +
                ")"
        // time for query is needed in ms epoch UTC
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
        val timeScopeEnd = currentTime + mTimeScope.toMillis()
        // Argument array needed according to docs for caching
        val selectionArgs: Array<String> = arrayOf(currentTime.toString(), timeScopeEnd.toString())
        println("selection query: $selection")
        println("selection args: ${selectionArgs[0]} ${selectionArgs[1]}")
        val cur: Cursor? = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)

        if(cur != null){
            val newCalendarEvents: MutableList<Event> = mutableListOf()

            println("Found calendar events: ${cur.count}")
            while(cur.moveToNext()){
                // Get the field values
                val begin: Long = cur.getLong(PROJECTION_BEGIN_INDEX)
                val end: Long = cur.getLong(PROJECTION_END_INDEX)
                val event = Event(
                        cur.getLong(PROJECTION_ID_INDEX),
                        cur.getString(PROJECTION_TITLE_INDEX),
                        LocalDateTime.ofEpochSecond(begin/1000, 0, ZoneOffset.UTC),
                        LocalDateTime.ofEpochSecond(end/1000, 0, ZoneOffset.UTC),
                        cur.getString(PROJECTION_CALENDAR_COLOR_INDEX)
                )
                println("found ${event.title}  ($event)")
                newCalendarEvents.add(event)
            }
            cur.close()
            mCalendarEvents = newCalendarEvents
        }
        else
            println("Calendar query returned no cursor")
    }

    /**
     * Check if the watch face has all permissions it needs for the timeline
     * To grant the permission, the user has to go to the settings because this has to be done
     * from and activity, and a watch face is only a service
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

/**
 * Representation of Events to be shown
 */
data class Event(
        val id: Long,
        val title: String,
        val begin: LocalDateTime,
        val end: LocalDateTime,
        val color: String,
)