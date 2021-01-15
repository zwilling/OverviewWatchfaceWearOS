package com.headsupwatchface

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.roundToInt

/**
 * Logic how to draw a timeline onto the canvas of the watch face
 */
class TimelineDrawer (
        val resources: Resources,
        val context: Context,
        var screenDimensions: ScreenDimensions = ScreenDimensions(0,0),
        var paintDefault: Paint,
        var paintTimelineText: Paint,
){
    private val mTimeScope = Duration.ofHours(resources.getInteger(R.integer.timeline_scope).toLong())

    private var mAmbient = false

    private val mLength: Float = resources.getDimension(R.dimen.timeline_length)
    private val mArrowLength: Float = resources.getDimension(R.dimen.timeline_arrow_length)

    data class TimelineBar (val x:Float, val size: Float, val thickness:Float)
    private val mNowBar = TimelineBar(
            x = resources.getDimension(R.dimen.timeline_now_bar_x),
            size = resources.getDimension(R.dimen.timeline_now_bar_size),
            thickness = resources.getDimension(R.dimen.timeline_now_bar_thickness)
    )

    private var mCenterX = screenDimensions.width / 2F
    private var mCenterY = screenDimensions.height / 2F

    private var mTimeZoneOffset = TimeZone.getDefault()
    private var mTemperaturePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.temperature)
        textSize = resources.getDimension(R.dimen.hour_mark_font_size)
    }
    private var mPrecipicationPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.precipitation)
        textSize = resources.getDimension(R.dimen.hour_mark_font_size)
    }

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

        // Weather data
        val weatherData = timeline.weather.weather
        if (weatherData != null){
            if (weatherData.hourly != null)
                drawTemperature(canvas, weatherData)
            if (weatherData.minutely != null) {
                drawProbabliltyOfPrecipitation(canvas, weatherData)
                if (!mAmbient)
                    drawMinutelyPrecipitation(canvas, weatherData)
            }
        }
    }

    fun updateScreenDimensions(screenDimensions: ScreenDimensions){
        this.screenDimensions = screenDimensions

        mCenterX = screenDimensions.width / 2F;
        mCenterY = screenDimensions.height / 2F
    }

    /**
     * On ambient mode change of the watch face, we need to adjust color and antialiasing
     */
    fun onAmbientModeChanged(inAmbientMode: Boolean) {
        mAmbient = inAmbientMode

        // adjust paint antialiasing
        for (paint in listOf(mTemperaturePaint, mPrecipicationPaint)){
            paint.isAntiAlias = !inAmbientMode
        }

        // change color to save battery in ambient mode
        mTemperaturePaint.color = if (!mAmbient) ContextCompat.getColor(context, R.color.temperature)
            else ContextCompat.getColor(context, R.color.temperature_ambient)
        mPrecipicationPaint.color = if (!mAmbient) ContextCompat.getColor(context, R.color.precipitation)
            else ContextCompat.getColor(context, R.color.precipitation_ambient)
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

    /**
     * Draws a temperature line indicating the weather from the current temp and the hourly forecast
     */
    private fun drawTemperature(canvas: Canvas, weather: WeatherModel.Result){
        // get start point for current time
        val nowX = mNowBar.x
        val nowY = getTempYPos(weather.current.temp)
        val nowText = weather.current.temp.roundToInt().toString() + context.getString(R.string.weather_units_display)

        // text next to current temp for scale
        val nowTextX = nowX - nowText.length / 4F * mTemperaturePaint.textSize
        canvas.drawText(nowText, nowTextX, nowY, mTemperaturePaint)

        // temp graph
        var points = mutableListOf(PointF(nowX, nowY))
        for (hourlyWeather in weather.hourly!!){
            points.add(PointF(calculateCoordinateOfTime(timeOfEpoch(hourlyWeather.dt)), getTempYPos(hourlyWeather.temp)))
            if (hourlyWeather.dt - weather.current.dt > resources.getInteger(R.integer.timeline_scope) * 3600L)
                break  // we do not need to draw outside of time scope
        }
        for (i in 0 until points.size - 1){
            canvas.drawLine(points[i].x, points[i].y, points[i+1].x, points[i+1].y, mTemperaturePaint)
            // ToDo: A spline would look better
        }
    }

    /**
     * Draws a precipitation graph based on hourly data
     */
    private fun drawProbabliltyOfPrecipitation(canvas: Canvas, weather: WeatherModel.Result){
        // pop graph
        var points = mutableListOf<PointF>()
        var maxPop = 0.0f
        var maxPopX = 0.0f
        var maxPopY = 0.0f
        var popText = ""

        for (hourlyWeather in weather.hourly!!){
            val point = PointF(calculateCoordinateOfTime(timeOfEpoch(hourlyWeather.dt)), getPopYPos(hourlyWeather.pop))
            points.add(point)

            // find place where to indicate max pop
            if (hourlyWeather.pop > maxPop){
                maxPop = hourlyWeather.pop
                popText = "${(maxPop * 100.0).toInt()}%"
                maxPopX = point.x - popText.length / 4F * mPrecipicationPaint.textSize
                maxPopY = point.y + mPrecipicationPaint.textSize
            }

            if (hourlyWeather.dt - weather.current.dt > resources.getInteger(R.integer.timeline_scope) * 3600L)
                break  // we do not need to draw outside of time scope
        }

        // draw just a bar
        val barSize = resources.getDimension(R.dimen.precipitation_level_width)
        for (point in points){
            canvas.drawLine(point.x - barSize/2, point.y, point.x + barSize/2, point.y, mPrecipicationPaint)
        }

        // write pop on highest value for reference
        canvas.drawText(popText, maxPopX, maxPopY, mPrecipicationPaint)
    }

    /**
     * Draws a minutely precipitation chart
     */
    private fun drawMinutelyPrecipitation(canvas: Canvas, weather: WeatherModel.Result){
        var maxPrecipitation = 0.0f
        var maxX = 0.0f
        var maxY = 0.0f
        var maxText = ""

        for (minutelyWeather in weather.minutely!!){
            val barX = calculateCoordinateOfTime(timeOfEpoch(minutelyWeather.dt))
            val barHeight = minutelyWeather.precipitation * resources.getDimension(R.dimen.precipitation_minutely_scale_1mm)

            // draw as minutely lines like a detailed bar chart
            canvas.drawLine(barX, mCenterY, barX, mCenterY + barHeight, mPrecipicationPaint)

            // find place where to indicate max
            if (minutelyWeather.precipitation > maxPrecipitation){
                maxPrecipitation = minutelyWeather.precipitation
                maxText = "${maxPrecipitation.toInt()}mm"
                maxX = barX - maxText.length / 4F * mPrecipicationPaint.textSize
                maxY = mCenterY + barHeight + mPrecipicationPaint.textSize
            }
        }

        // write highest value for reference
        canvas.drawText(maxText, maxX, maxY, mPrecipicationPaint)
    }

    /**
     * Getting the y coordinate corresponding to temperature values
     * @param temp: Temperature to find the coordinate for
     * @return: Y coordinate
     */
    private fun getTempYPos(temp: Float): Float{
        // calculate y pos from scale using 40° as reference
        return mCenterY - resources.getDimension(R.dimen.temp_scale_40_degrees) * temp / 40.0f
    }

    /**
     * Getting the y coordinate corresponding to pop values
     * @param pop: Probability of precipitation to find the coordinate for
     * @return: Y coordinate
     */
    private fun getPopYPos(pop: Float): Float{
        // calculate y pos from scale
        return mCenterY - resources.getDimension(R.dimen.precipitation_scale_100_percent) * pop
    }

}