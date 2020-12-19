package com.headsupwatchface

import android.content.res.Resources
import android.graphics.*
import java.time.Duration
import java.time.LocalDateTime

/**
 * Logic how to draw a timeline onto the canvas of the watch face
 */
class TimelineDrawer (
        val resources: Resources,
        var screenDimensions: ScreenDimensions = ScreenDimensions(0,0),
        var paintDefault: Paint,
        var paintTimelineText: Paint,
){
    private val mTimeScope = Duration.ofHours(resources.getInteger(R.integer.timeline_scope).toLong())

    private val mLength: Float = resources.getDimension(R.dimen.timeline_length)
    private val mArrowLength: Float = resources.getDimension(R.dimen.timeline_arrow_length)

    data class TimelineBar (val x:Float, val size: Float, val thickness:Float)
    private val mNowBar = TimelineBar(
            x = resources.getDimension(R.dimen.timeline_now_bar_x),
            size = resources.getDimension(R.dimen.timeline_now_bar_size),
            thickness = resources.getDimension(R.dimen.timeline_now_bar_thickness)
    )

    var mCenterX = screenDimensions.width / 2F
    var mCenterY = screenDimensions.height / 2F

    /**
     * Main function to draw the timeline on the watch face canvas
     */
    fun draw(canvas: Canvas, timeline: Timeline, ambient: Boolean){

        // The line itself
        canvas.drawLine(0F, mCenterY, mLength, mCenterY, paintDefault)

        // Arrow end
        canvas.drawLine(mLength, mCenterY,
                mLength - mArrowLength, mCenterY  + mArrowLength, paintDefault)
        canvas.drawLine(mLength, mCenterY,
                mLength - mArrowLength, mCenterY  - mArrowLength, paintDefault)

        // Bar indicating now
        drawBarOnTimeline(canvas, mNowBar)

        // Hour marks
        for (hourMark in timeline.hourMarks){
            val hourIndicatorBar = TimelineBar(
                    x = calculateCoordinateOfTime(hourMark),
                    size = resources.getDimension(R.dimen.hour_mark_size),
                    thickness = resources.getDimension(R.dimen.hour_mark_thickness)
            )
            drawBarOnTimeline(canvas, hourIndicatorBar)
            drawTimeOnMark(canvas, hourMark, resources.getDimension(R.dimen.hour_mark_text_offset))
        }

        // Calendar Events
        for (event in timeline.calendarEvents){
            val coordinateBegin = calculateCoordinateOfTime(event.begin)
            val coordinateEnd = calculateCoordinateOfTime(event.end)
            val eventBar = TimelineBar(
                    x = coordinateBegin,
                    size = resources.getDimension(R.dimen.event_bar_height),
                    thickness = coordinateEnd - coordinateBegin
            )
            drawEventBarOnTimeline(canvas, eventBar)
            drawTextOnMark(canvas, event.title, event.begin,
                    resources.getDimension(R.dimen.event_title_offset), centered = false)
        }
    }

    fun updateScreenDimensions(screenDimensions: ScreenDimensions){
        this.screenDimensions = screenDimensions

        mCenterX = screenDimensions.width / 2F;
        mCenterY = screenDimensions.height / 2F
    }

    /**
     * Calculating where on the timeline a point in time should be drawn
     */
    private fun calculateCoordinateOfTime(time: LocalDateTime): Float{
        val timeDistance = Duration.between(LocalDateTime.now(), time)
        val pixelDistFrom0 = timeDistance.toMillis().toFloat() / mTimeScope.toMillis().toFloat() * (mLength - mNowBar.x)
        return pixelDistFrom0 + mNowBar.x
    }

    /**
     * Draws a Bar on the timeline to mark something
     */
    private fun drawBarOnTimeline(canvas: Canvas, timelineBar: TimelineBar){
        val barRect = RectF(timelineBar.x - timelineBar.thickness / 2.0F,
                mCenterY - timelineBar.size / 2.0F,
                timelineBar.x + timelineBar.thickness / 2.0F,
                mCenterY + timelineBar.size / 2.0F)
        canvas.drawRect(barRect, paintDefault)
    }

    /**
     * Draws an event-box on the timeline
     */
    private fun drawEventBarOnTimeline(canvas: Canvas, timelineBar: TimelineBar){
        val barRect = RectF(timelineBar.x,
                mCenterY - timelineBar.size,
                timelineBar.x + timelineBar.thickness,
                mCenterY)
        canvas.drawRect(barRect, paintDefault)
    }

    /**
     * Draws a tiny time as number hint on the timeline
     * @param offset Offset above or below the timeline
     */
    private fun drawTimeOnMark(canvas: Canvas, time: LocalDateTime, offset: Float){
        val text = time.hour.toString()
        drawTextOnMark(canvas, text, time, offset)
    }

    /**
     * Draws a tiny time as number hint on the timeline
     * @param text Text to be written
     * @param time Where it should be written on the timeline
     * @param offset Offset above or below the timeline
     * @param centered If the text should be centered above the time
     */
    private fun drawTextOnMark(canvas: Canvas, text: String, time: LocalDateTime, offset: Float, centered: Boolean = true){
        var xPos = calculateCoordinateOfTime(time)
        if (centered)
            xPos -= text.length / 4F * paintTimelineText.textSize
        val yPos = mCenterY + offset
        canvas.drawText(text, xPos, yPos, paintTimelineText)
    }
}