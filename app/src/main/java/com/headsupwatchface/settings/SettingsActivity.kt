package com.headsupwatchface.settings

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.wearable.complications.ComplicationHelperActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.core.widget.addTextChangedListener
import com.headsupwatchface.R
import com.headsupwatchface.WatchFace

/**
 * Activity for the settings of the watch face
 *
 * Lets you choose which complications to show and change some preferences
 */
class SettingsActivity : Activity() {

    // Preference API object to sync settings with the watch face service
    private lateinit var mSharedPreferences : SharedPreferences

    // UI elements to work with
    private lateinit var mButtonComplicationLeft : Button
    private lateinit var mButtonComplicationRight : Button
    private lateinit var mSwitch12HourFormat: Switch
    private lateinit var mSwitchCalendarPermission: Switch
    private lateinit var mSwitchLocationPermission: Switch
    private lateinit var mEditTextWeatherApiKey: EditText

    private var mPermissionResultViewMap: MutableMap<String, Switch> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        mSharedPreferences = getSharedPreferences(getString(R.string.preferences_file_key),
                Context.MODE_PRIVATE)

        // Getting all needed UI elements
        mButtonComplicationLeft = findViewById(R.id.button_complication_left)
        mButtonComplicationRight = findViewById(R.id.button_complication_right)
        mEditTextWeatherApiKey = findViewById(R.id.weather_api_key)

        mSwitchCalendarPermission = preparePermissionSwitch(R.id.calendar_permission, Manifest.permission.READ_CALENDAR)
        mSwitchLocationPermission = preparePermissionSwitch(R.id.location_permission, Manifest.permission.ACCESS_COARSE_LOCATION)

        // ToDo: propper formatting with scrolling or submenus

        // For buttons we have to define what happens on a tap
        mButtonComplicationLeft.setOnClickListener {
            chooseComplicationDialog(resources.getInteger(R.integer.complication_left))
        }
        mButtonComplicationRight.setOnClickListener {
            chooseComplicationDialog(resources.getInteger(R.integer.complication_right))
        }

        // For the switch, we set the starting position and what happens on change
        mSwitch12HourFormat = findViewById(R.id.hour_format_12)
        mSwitch12HourFormat.isChecked = mSharedPreferences.getBoolean(
            getString(R.string.preference_hour_format_12_key), false)
        mSwitch12HourFormat.setOnCheckedChangeListener { _, isChecked ->
            with(mSharedPreferences.edit()){
                putBoolean(getString(R.string.preference_hour_format_12_key), isChecked)
                apply()
            }
        }

        // skipped fillining in the previous key so that the hint is used properly
        // mEditTextWeatherApiKey.setText(R.string.preference_weather_api_key)
        mEditTextWeatherApiKey.addTextChangedListener {
            with(mSharedPreferences.edit()){
                putString(getString(R.string.preference_weather_api_key), it.toString())
                apply()
            }
        }
    }

    /**
     * Starting the activity for the complication choosing intent (provided by wear os)
     */
    private fun chooseComplicationDialog(complication_id : Int) {
        // Start complication choosing intent
        val intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
                this, ComponentName(this, WatchFace::class.java),
                complication_id, *WatchFace.complicationAllowedTypes.toIntArray())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)
    }

    /**
     * For the switch, we set the starting position and what happens on change
     *
     * @param viewId: Resource id of the GUI element
     * @param permission: which permission it should request
     * @return switch view element
     */
    private fun preparePermissionSwitch(viewId: Int, permission: String): Switch {
        val switchView: Switch = findViewById(viewId)
        switchView.isChecked = checkSelfPermission(permission) ==
                PackageManager.PERMISSION_GRANTED
        switchView.setOnCheckedChangeListener { _, _ ->
            requestPermissions(arrayOf(permission), 1)
        }
        // add permission view pair to map so that it can be found when the permission dialog returns
        mPermissionResultViewMap[permission] = switchView
        return switchView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (index in permissions.indices){
            mPermissionResultViewMap[permissions[index]]?.isChecked = grantResults[index] == PackageManager.PERMISSION_GRANTED
        }
    }
}

