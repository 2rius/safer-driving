package com.example.saferdriving.enums

import android.Manifest
import android.os.Build

enum class Permission(val permissions: Array<String>) {
    /**
     * Bluetooth permissions will vary depending on the Android SDK version.
     * In SDK version 23 and above, only [Manifest.permission.BLUETOOTH_CONNECT]
     * permission is needed. On older versions, [Manifest.permission.BLUETOOTH] and
     * [Manifest.permission.BLUETOOTH_ADMIN] permissions are required.
     */
    BLUETOOTH(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    ),

    /**
     * Location permissions required to get the location of the phone include
     * [Manifest.permission.ACCESS_FINE_LOCATION] for precise location with GPS and
     * [Manifest.permission.ACCESS_COARSE_LOCATION] for more rough location with (typically) WiFI.
     */
    LOCATION(
        arrayOf (
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}