package com.headsupwatchface


/**
 * Class for representing a location and containing code obtaining the last known location
 */
class Location (
    val latitude: Float,
    val longitude: Float,
)
{
    companion object {
        fun getLastKnown(): Location {
            return Location(0.0f, 0.0f)
        }
    }
}