package com.headsupwatchface

import android.content.res.Resources
import android.graphics.*

/**
 * Logic how to draw a timeline onto the canvas of the watch face
 */
class TimelineDrawer (
        val resources: Resources,
        var screenDimensions: ScreenDimensions = ScreenDimensions(0,0),
        var paint: Paint = Paint(),
){
    private val mLength: Float = resources.getDimension(R.dimen.timeline_length)
    private val mArrowLength: Float = resources.getDimension(R.dimen.timeline_arrow_length)
    private val mNowBarX: Float = resources.getDimension(R.dimen.timeline_now_bar_x)
    private val mNowBarSize: Float = resources.getDimension(R.dimen.timeline_now_bar_size)
    private val mNowBarThickness: Float = resources.getDimension(R.dimen.timeline_now_bar_thickness)

    var mCenterX = screenDimensions.width / 2F
    var mCenterY = screenDimensions.height / 2F

    /**
     * Main function to draw the timeline on the watch face canvas
     */
    fun draw(canvas: Canvas, timeline: Timeline, ambient: Boolean){

        canvas.drawLine(0F, mCenterY, mLength, mCenterY, paint)
        // Arrow end
        canvas.drawLine(mLength, mCenterY,
                mLength - mArrowLength, mCenterY  + mArrowLength, paint)
        canvas.drawLine(mLength, mCenterY,
                mLength - mArrowLength, mCenterY  - mArrowLength, paint)
        // Bar indicating now
        val nowBar = RectF(mNowBarX - mNowBarThickness,
                mCenterY - mNowBarSize / 2.0F,
                mNowBarX,
                mCenterY + mNowBarSize / 2.0F)
        canvas.drawRect(nowBar, paint)
    }

    fun updateScreenDimensions(screenDimensions: ScreenDimensions){
        this.screenDimensions = screenDimensions

        mCenterX = screenDimensions.width / 2F;
        mCenterY = screenDimensions.height / 2F;
    }
}
