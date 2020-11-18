package com.headsupwatchface

import android.app.Activity
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
    }
}