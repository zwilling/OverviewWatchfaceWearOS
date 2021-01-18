package com.headsupwatchface

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Small class for capturing screen dimensions
 */
class ScreenDimensions(
        val width: Int,
        val height: Int,
)

class PermissionChecker(){
        companion object{
                /**
                 * Check if the watch face has all permissions it needs for the timeline
                 * To grant the permission, the user has to go to the settings because this has to be done
                 * from and activity, and a watch face is only a service
                 *
                 * @param context: The context to check the permissions for
                 * @param notify: Whether the user should be notified about missing permissions
                 * @param permissionMessageMap: Map of required permissions to the message to be shown
                 * @return: If all permissions were granted
                 */
                fun checkPermissions(context: Context, notify: Boolean, permissionMessageMap: Map<String, Int>): Boolean{
                        for((permission, message) in permissionMessageMap){
                                if (context.checkSelfPermission(permission) ==
                                        PackageManager.PERMISSION_DENIED){
                                        if(notify)
                                                Toast.makeText(context.applicationContext, message,
                                                        Toast.LENGTH_LONG).show()
                                        return false
                                }
                        }
                        return true
                }
        }
}

/**
 * Small conversion function between epoch time and localdatetime using local timezone
 *
 * @param epoch: Epoch of the time to convert
 * @return: Time as LocalDateTime
 */
fun timeOfEpoch(epoch: Long) : LocalDateTime{
        return LocalDateTime.ofEpochSecond(epoch, 0, getTimeZoneOffset())
}

/**
 * Small helper function to get the current timezone (needed to convert epoch to LocalDateTime)
 *
 * @return: ZoneOffset of local timezone
 */
fun getTimeZoneOffset(): ZoneOffset {
        return ZoneOffset.ofTotalSeconds(
                TimeZone.getDefault().getOffset(LocalDateTime.now().toEpochSecond(
                                ZoneOffset.UTC) * 1000) / 1000)
}