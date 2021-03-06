package com.headsupwatchface

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.support.wearable.provider.WearableCalendarContract
import android.text.format.DateUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


/**
 * Definitions for querying the device calendar
 */
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Events._ID,                            // 0
        CalendarContract.Events.TITLE,                          // 1
        CalendarContract.Events.CALENDAR_COLOR,                 // 2
        CalendarContract.Instances.BEGIN,                       // 3
        CalendarContract.Instances.END,                         // 4
        CalendarContract.Events.ALL_DAY,                        // 5
)
// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_TITLE_INDEX: Int = 1
private const val PROJECTION_CALENDAR_COLOR_INDEX: Int = 2
private const val PROJECTION_BEGIN_INDEX: Int = 3
private const val PROJECTION_END_INDEX: Int = 4
private const val PROJECTION_ALL_DAY: Int = 5


/**
 * Representation of Events to be shown
 */
data class Event(
        val id: Long,
        val title: String,
        val begin: LocalDateTime,
        val end: LocalDateTime,
        val color: String,
        val allDay: Boolean,
)


/**
 * Model and update logic for meetings and appointments of the calendar to be shown on the watch face
 */
class MeetingCalendar (
        private val context: Context,
        private val resources: Resources,
        private val contentResolver: ContentResolver, // for querying the calendar
        private val timeScope: Duration,
){

    /**
     * Calendar Events to be shown on the timeline
     */
    var calendarEvents: MutableList<Event> = mutableListOf()

    /**
     * Querying the calendars from the device to update what is represented in the timeline
     */
    fun updateCalendar (){
        if (!PermissionChecker.checkPermissions(context, false, mapOf(Manifest.permission.READ_CALENDAR to R.string.permission_calendar_missing))){
            println("Can not update calendar data without permission")
            return
        }

        if(resources.getBoolean(R.bool.simulated_calendar)){
            calendarEvents = getSimulatedCalendar()
            return
        }

        // Construct Query
        val uriBuilder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon()
        val now: Long = Date().time
        ContentUris.appendId(uriBuilder, now - DateUtils.DAY_IN_MILLIS * 10000);
        ContentUris.appendId(uriBuilder, now + DateUtils.DAY_IN_MILLIS * 10000);
        val uri: Uri = uriBuilder.build()
//        // SQL where selection does not seem to affect the result -.- querying afterwards by hand
//        val selection = "" +
//                // Only show events that have not ended yet
//                "${EVENT_PROJECTION[PROJECTION_END_INDEX]} > ?" +
//                " AND " +
//                // Only show events that begin before the end of the time scope
//                "${EVENT_PROJECTION[PROJECTION_BEGIN_INDEX]} < ?" +
//                " AND " +
//                // Do not show all day events
//                "${EVENT_PROJECTION[PROJECTION_ALL_DAY]} = 0" +
//                ""
        // time for query is needed in ms epoch UTC
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
        val timeScopeEnd = currentTime + timeScope.toMillis()
        // Argument array needed according to docs for caching
//        val selectionArgs: Array<String> = arrayOf(currentTime.toString(), timeScopeEnd.toString())
        val cur: Cursor? = contentResolver.query(uri, EVENT_PROJECTION, null, null, null)

        if(cur != null){
            val newCalendarEvents: MutableList<Event> = mutableListOf()

            while(cur.moveToNext()){
                // Get the field values
                val begin: Long = cur.getLong(PROJECTION_BEGIN_INDEX)
                val end: Long = cur.getLong(PROJECTION_END_INDEX)
                val allDay: Boolean = 0 < cur.getInt(PROJECTION_ALL_DAY)
                // Check if event should be shown (SQL where specified in selection of query seems not to work)
                val queryFit =
                        begin < timeScopeEnd &&  // Only show events that begin before the end of the time scope
                                end > currentTime &&  // Only show events that have not ended yet
                                !allDay // Do not show all day events
                if (queryFit) {
                    val event = Event(
                            cur.getLong(PROJECTION_ID_INDEX),
                            cur.getString(PROJECTION_TITLE_INDEX),
                            timeOfEpoch(begin / 1000),
                            timeOfEpoch(end / 1000),
                            cur.getString(PROJECTION_CALENDAR_COLOR_INDEX),
                            allDay,
                    )
                    // ToDo: handle how to display all day events
                    newCalendarEvents.add(event)
                }
            }
            cur.close()
            calendarEvents = newCalendarEvents
        }
        else
            println("Calendar query returned no cursor")

        // sort for a consistent handling in later rendering
        calendarEvents.sortWith(compareBy ({ it.title }, { it.begin.toEpochSecond(getTimeZoneOffset()) }))
    }

    /**
     * For debugging and testing this function yields a test event list
     * You can enable it in the preferences.xml
     */
    private fun getSimulatedCalendar () : MutableList<Event>{
        println("Using simulated calendar")
        return mutableListOf(
                Event(0, "test meeting with very long title text",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                        "blue",false),
                Event(1, "test1",
                        LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusMinutes(90),
                        "blue",false),
                Event(2, "test2",
                        LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(4),
                        "blue",false),
        )
    }
}
