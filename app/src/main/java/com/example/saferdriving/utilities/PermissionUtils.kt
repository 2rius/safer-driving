package com.example.saferdriving.utilities

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun AppCompatActivity.requestPermission(
    permissions: Array<String>,
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val requestPermissions = permissionsToRequest(this, permissions)
    if (requestPermissions.isEmpty()) {
        onGranted()
        return
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

    requestPermissionLauncher.launch(requestPermissions.toTypedArray())
}

private fun permissionsToRequest(ctx: Context, permissions: Array<String>): ArrayList<String> {
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