package com.headsupwatchface

import android.content.res.Resources


/**
 * Model class for a timeline and its content
 */
class Timeline (
        val resources: Resources,
        val timeScope : Int = resources.getInteger(R.integer.timeline_scope),
) {

}