package com.headsupwatchface.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.wearable.complications.ComplicationHelperActivity
import android.widget.Button
import com.headsupwatchface.R
import com.headsupwatchface.WatchFace

/**
 * Activity for the settings of the watch face
 *
 * Lets you choose which complications to show and change some preferences
 */
class SettingsActivity : Activity() {

    private lateinit var mButtonComplicationLeft : Button
    private lateinit var mButtonComplicationRight : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        mButtonComplicationLeft = findViewById(R.id.button_complication_left)
        mButtonComplicationRight = findViewById(R.id.button_complication_right)

        mButtonComplicationLeft.setOnClickListener {
            // Start complication choosing intent
            val intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
                this, ComponentName(this, WatchFace.javaClass),
                0, *WatchFace.complicationAllowedTypes.toIntArray())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        mButtonComplicationRight.setOnClickListener {
            // Start complication choosing intent
            val intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
                this, ComponentName(this, WatchFace.javaClass),
                1, *WatchFace.complicationAllowedTypes.toIntArray())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}

