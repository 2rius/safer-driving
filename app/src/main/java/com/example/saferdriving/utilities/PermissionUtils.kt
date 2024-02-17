package com.example.saferdriving.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT
    )
} else {
    arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )
}

fun AppCompatActivity.getRequestPermission(
    permissions: Array<String>,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
): () -> Unit {
    val requestPermissions = permissionsToRequest(this, permissions)
    if (requestPermissions.isEmpty()) {
        return onGranted
    }

    // Code example from https://developer.android.com/training/permissions/requesting
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permsGranted ->
            val allGranted = permsGranted.all { it.value }
            if (allGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }

    return { requestPermissionLauncher.launch(requestPermissions.toTypedArray()) }
}

private fun permissionsToRequest(
    ctx: Context,
    permissions: Array<String>
): ArrayList<String> {
    val result: ArrayList<String> = ArrayList()

    for (permission in permissions)
        if (ContextCompat.checkSelfPermission(
                ctx,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        )
            result.add(permission)

    return result
}