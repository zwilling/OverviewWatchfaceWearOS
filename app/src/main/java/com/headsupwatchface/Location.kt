package com.headsupwatchface

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import kotlin.concurrent.timerTask


/**
 * Class for obtaining the last known location
 */
class LocationService{
    var lastKnownLocation: Location? = null

    private val mTimerLocationUpdate = Timer()
    private var mFusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission") // already checked with our helper function
    constructor (context: Context, resources: Resources) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // setup timer for keeping location up to date
        mTimerLocationUpdate.schedule(timerTask{
            if (PermissionChecker.checkPermissions(context, false, mapOf(android.Manifest.permission.ACCESS_COARSE_LOCATION to R.string.permission_location_missing))){
                mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    lastKnownLocation = location
                    println("Location updated to $lastKnownLocation")
                }
            }
        },
            resources.getInteger(R.integer.location_update_delay).toLong(),
            resources.getInteger(R.integer.location_update_interval).toLong()
        )
    }
}