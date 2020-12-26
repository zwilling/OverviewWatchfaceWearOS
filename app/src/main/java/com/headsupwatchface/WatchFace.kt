package com.headsupwatchface

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.SystemProviders
import android.support.wearable.complications.rendering.ComplicationDrawable
import androidx.core.content.ContextCompat
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.widget.Toast

import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Heads Up watch face showing the time in digital and a heads up of what is going to happen soon
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class WatchFace : CanvasWatchFaceService() {

    companion object {
        private val NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 1000

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0

        val complicationAllowedTypes = listOf(
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SMALL_IMAGE,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_EMPTY,
        )
    }

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: WatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<WatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }


    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar

        private val mSharedPreferences = getSharedPreferences(
                getString(R.string.preferences_file_key), Context.MODE_PRIVATE)

        private var mRegisteredTimeZoneReceiver = false

        private var mCenterX = 0F
        private var mCenterY = 0F

        private val mDigitalOffset = PointF(resources.getDimension(R.dimen.digital_x_offset),
            resources.getDimension(R.dimen.digital_y_offset))
        private val mMinutesOffset = PointF(resources.getDimension(R.dimen.minutes_seconds_x_offset),
            resources.getDimension(R.dimen.minutes_y_offset))
        private val mSecondsOffset = PointF(resources.getDimension(R.dimen.minutes_seconds_x_offset),
            resources.getDimension(R.dimen.seconds_y_offset))


        private lateinit var mBackgroundPaint: Paint
        private lateinit var mHourPaint: Paint
        private lateinit var mMinutePaint: Paint
        private lateinit var mSecondPaint: Paint
        private lateinit var mTimeLineTextPaint: Paint

        private lateinit var mTimelineDrawer: TimelineDrawer

        private lateinit var mTimerCalendarUpdate: Timer
        private lateinit var mTimerWeatherUpdate: Timer
//        private lateinit var mTimerLocationUpdate: Timer

        /**
         * Complication setup
         */
        inner class ComplicationSetup(
            val defaultProvider : Int,
            val defaultType : Int,
            var drawable : ComplicationDrawable,
            var offset : PointF,
        )
        private var mComplications = mapOf(
            resources.getInteger(R.integer.complication_left) to ComplicationSetup(
                SystemProviders.WATCH_BATTERY,
                ComplicationData.TYPE_SHORT_TEXT,
                resources.getDrawable(R.drawable.complication_layout, null) as ComplicationDrawable,
                PointF(-resources.getDimension(R.dimen.complication_offset_x),
                    resources.getDimension(R.dimen.complication_offset_y)),
            ),
            resources.getInteger(R.integer.complication_right) to ComplicationSetup(
                SystemProviders.NEXT_EVENT,
                ComplicationData.TYPE_SHORT_TEXT,
                resources.getDrawable(R.drawable.complication_layout, null) as ComplicationDrawable,
                PointF(resources.getDimension(R.dimen.complication_offset_x),
                    resources.getDimension(R.dimen.complication_offset_y)),
            ),
        )

        private val mTimeline = Timeline(resources, contentResolver, this@WatchFace,
            mSharedPreferences)

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false

        private val mUpdateTimeHandler: Handler = EngineHandler(this)

        private val mTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@WatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            mCalendar = Calendar.getInstance()

            populateDefaultPreferences()

            val resources = this@WatchFace.resources
            mDigitalOffset.y = resources.getDimension(R.dimen.digital_y_offset)

            // Initializes background.
            mBackgroundPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.background)
            }

            // Initializes Watch Face.
            mHourPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.hour_text)
                textSize = resources.getDimension(R.dimen.digital_text_size_hour)
            }
            mMinutePaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.minute_text)
                textSize = resources.getDimension(R.dimen.digital_text_size_min_sec)
            }
            mSecondPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.second_text)
                textSize = resources.getDimension(R.dimen.digital_text_size_min_sec)
            }
            mTimeLineTextPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.second_text)
                textSize = resources.getDimension(R.dimen.hour_mark_font_size)
            }

            for (paint in arrayOf(mHourPaint, mMinutePaint, mSecondPaint, mTimeLineTextPaint)){
                paint.apply {
                    typeface = NORMAL_TYPEFACE
                    isAntiAlias = true
                }
            }

            for ((id, complicationSetup) in mComplications) {
                setDefaultSystemComplicationProvider(id, complicationSetup.defaultProvider,
                    complicationSetup.defaultType)
                complicationSetup.drawable.setContext(this@WatchFace)
            }
            setActiveComplications(*mComplications.keys.toIntArray())

            mTimeline.checkPermissions(true, mapOf(
                    Manifest.permission.INTERNET to R.string.permission_internet_missing,
                    Manifest.permission.READ_CALENDAR to R.string.permission_calendar_missing,
            ))
            mTimelineDrawer = TimelineDrawer(resources, paintDefault = mMinutePaint,
                    paintTimelineText = mTimeLineTextPaint)

            // create timer to periodically update background stuff (calendar, weather, location)
            mTimerCalendarUpdate = Timer()
            mTimerCalendarUpdate.schedule(timerTask{
                if (mTimeline.checkPermissions(false, mapOf(Manifest.permission.READ_CALENDAR to R.string.permission_calendar_missing)))
                    mTimeline.updateCalendar()
            },
                resources.getInteger(R.integer.calendar_update_delay).toLong(),
                resources.getInteger(R.integer.calendar_update_interval).toLong()
            )
            mTimerWeatherUpdate = Timer()
            mTimerWeatherUpdate.schedule(timerTask{
                mTimeline.updateWeather()
            },
                resources.getInteger(R.integer.weather_update_delay).toLong(),
                resources.getInteger(R.integer.weather_update_interval).toLong()
            )
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )

            mComplications.values.forEach {
                it.drawable.setBurnInProtection(mBurnInProtection)
            }
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            // adjust paint antialiasing
            if (mLowBitAmbient) {
                for (paint in listOf(mHourPaint, mMinutePaint, mTimeLineTextPaint)){
                    paint.isAntiAlias = !inAmbientMode
                }
            }

            // draw only borders in ambient mode
            for (paint in listOf(mHourPaint, mMinutePaint)){
                paint.style = if (mAmbient) Paint.Style.STROKE else Paint.Style.FILL_AND_STROKE
            }

            mComplications.values.forEach {
                it.drawable.setInAmbientMode(inAmbientMode)
            }

            // Whether the timer should be running depends on whether we"re visible (as well as
            // whether we"re in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
//                    Toast.makeText(applicationContext, R.string.message, Toast.LENGTH_SHORT)
//                        .show()
                    // TODO: handle tap on complication

                }
            }
            invalidate()
        }

        override fun onComplicationDataUpdate(watchFaceComplicationId: Int,
                                              data: ComplicationData?) {
            super.onComplicationDataUpdate(watchFaceComplicationId, data)

            mComplications[watchFaceComplicationId]?.drawable?.setComplicationData(data)
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            // Draw the background.
            if (mAmbient) {
                canvas.drawColor(Color.BLACK)
            } else {
                canvas.drawRect(
                    0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), mBackgroundPaint
                )
            }

            mTimelineDrawer.draw(canvas, mTimeline, mAmbient)

            // draw complications
            mComplications.values.forEach {
                it.drawable.draw(canvas)
            }

            drawDigitalDisplay(canvas)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            mCenterX = width / 2F
            mCenterY = height / 2F
            mTimelineDrawer.updateScreenDimensions(ScreenDimensions(width, height))

            val complicationSize = resources.getDimension(R.dimen.complication_size)
            mComplications.values.forEach {
                it.drawable.bounds = Rect(
                    (mCenterX - complicationSize/2 + it.offset.x).toInt(),
                    (mCenterY - complicationSize/2 + it.offset.y).toInt(),
                    (mCenterX + complicationSize/2 + it.offset.x).toInt(),
                    (mCenterY + complicationSize/2 + it.offset.y).toInt(),
                )
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                // Update time zone in case it changed while we weren"t visible.
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            // Whether the timer should be running depends on whether we"re visible (as well as
            // whether we"re in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@WatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@WatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

//        override fun onApplyWindowInsets(insets: WindowInsets) {
//            super.onApplyWindowInsets(insets)
//
//            // Load resources that have alternate values for round watches here
////            val resources = this@WatchFace.resources
////            val isRound = insets.isRound
//        }

        /**
         * Starts the [.mUpdateTimeHandler] timer if it should be running and isn"t currently
         * or stops it if it shouldn"t be running but currently is.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer should
         * only run when we"re visible and in interactive mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !isInAmbientMode
        }

        /**
         * Draws the time on the display canvas
         */
        private fun drawDigitalDisplay(canvas: Canvas) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            val hours = if (mSharedPreferences.getBoolean(getString(R.string.preference_hour_format_12_key), false))
                String.format("%d", mCalendar.get(Calendar.HOUR))
                else String.format("%d", mCalendar.get(Calendar.HOUR_OF_DAY))
            val minutes = String.format("%02d", mCalendar.get(Calendar.MINUTE))

            // if hours have two digits we have to draw from further left
            val hourXOffset =
                    if (hours.length > 1) mDigitalOffset.x -
                            resources.getDimension(R.dimen.digital_x_two_digit_correction)
                    else mDigitalOffset.x
            // Hours are written large on the left
            canvas.drawText(hours, mCenterX + hourXOffset, mCenterY + mDigitalOffset.y, mHourPaint)
            // Minutes written small on the right top
            canvas.drawText(minutes, mCenterX + mDigitalOffset.x + mMinutesOffset.x,
                    mCenterY + mDigitalOffset.y + mMinutesOffset.y, mMinutePaint)

            // Only ambient mode refreshes often enough for seconds
            if (!mAmbient) {
                val seconds = String.format("%02d", mCalendar.get(Calendar.SECOND))
                canvas.drawText(seconds, mCenterX + mDigitalOffset.x + mSecondsOffset.x,
                        mCenterY + mDigitalOffset.y + mSecondsOffset.y, mSecondPaint
                )
            }
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        /**
         * Checking the preferences if all required values are there and set default values if not
         */
        private fun populateDefaultPreferences() {
            with (mSharedPreferences.edit()){
                val hourFormat = mSharedPreferences.getBoolean(getString(R.string.preference_hour_format_12_key)
                        , resources.getBoolean(R.bool.hour_format_12_default))
                putBoolean(getString(R.string.preference_hour_format_12_key), hourFormat)

                apply()
            }
        }
    }
}