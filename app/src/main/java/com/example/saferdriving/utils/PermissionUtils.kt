package com.example.saferdriving.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Returns a function that requests the specified permissions and executes the provided
 * callbacks based on the permission grant status.
 *
 * @param permissions The array of permissions requested.
 * @param onGranted A callback function that executes if all the permissions are granted.
 * Defaults to empty lambda.
 * @param onDenied A callback function that executes if any of the permissions are denied.
 * Defaults to empty lambda.
 * @return A function that launches the permission request.
 */
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

/**
 * Returns a list of permissions that need to be requested from the user.
 * Only permissions that are not already granted will be returned.
 *
 * @param context The context in which the permissions are checked.
 * @param permissions The array of permissions that should be checked.
 * @return A list of permissions that need to be requested.
 */
private fun permissionsToRequest(
    context: Context,
    permissions: Array<String>
): ArrayList<String> {
    val result: ArrayList<String> = ArrayList()

    for (permission in permissions)
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        )
            result.add(permission)

    return result
}