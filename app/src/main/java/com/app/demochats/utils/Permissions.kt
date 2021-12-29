package com.app.demochats.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
object Permissions {

    fun locPermissionCheck(activity: Activity) {

        if (Build.VERSION.SDK_INT >= 23) {

            Log.e("checkPERMISSIONSFGFGH","HXZXGHGS")

            val hasReadPermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val hasWritePermission = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            val hasNetworkStatePermission = activity.checkSelfPermission(Manifest.permission.CAMERA)
            val hasRecordAudio = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)

            val permissionList = ArrayList<String>()

            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (hasNetworkStatePermission != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA)
            }
            if (hasRecordAudio != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO)
            }

            Log.e("PermissionsSize","${permissionList.size}")

            if (permissionList.isNotEmpty()) {
                activity.requestPermissions(permissionList.toTypedArray(), 2)
            } else {
                return
            }
        }
        else
        return
    }
    fun recordPermissionCheck(activity: Activity): Boolean {

        if (Build.VERSION.SDK_INT >= 23) {

            Log.e("checkPERMISSIONSFGFGH","Record")

            val hasNetworkStatePermission = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)

            val permissionList = ArrayList<String>()

            if (hasNetworkStatePermission != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO)
            }

            Log.e("PermissionsSize","${permissionList.size}")

            if (permissionList.isNotEmpty()) {
                activity.requestPermissions(permissionList.toTypedArray(), 3)
            } else {
                return true
            }
        }
        else if (Build.VERSION.SDK_INT < 23) {

            Log.e("checkPERMISSIONS","Record")

            return true
        }
        return false
    }
}