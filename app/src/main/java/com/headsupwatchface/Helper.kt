package com.headsupwatchface

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast

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