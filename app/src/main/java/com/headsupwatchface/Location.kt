package com.headsupwatchface

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import java.util.*
import kotlin.concurrent.timerTask


/**
 * Class for obtaining the last known location
 */
class LocationService{
    var lastKnownLocation: Location? = null

    private val mLocationRequest: LocationRequest?
    private val mTimerLocationUpdate = Timer()
    private val mFusedLocationClient: FusedLocationProviderClient
    private val locationCallback: LocationCallback

    @SuppressLint("MissingPermission") // already checked with our helper function
    constructor (context: Context, resources: Resources) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // request location updates, otherwise we do not get the location if not triggered by other apps
        mLocationRequest = LocationRequest.create()?.apply {
            interval = resources.getInteger(R.integer.location_update_interval).toLong()
            fastestInterval = resources.getInteger(R.integer.location_update_fastest_interval).toLong()
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    lastKnownLocation = location
                }
            }
        }

        // Not sure if this is needed. Could be important if no other app requests location updates
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
            locationCallback,
            Looper.getMainLooper())

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