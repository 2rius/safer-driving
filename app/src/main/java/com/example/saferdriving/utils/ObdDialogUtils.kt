package com.example.saferdriving.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.saferdriving.R
import com.example.saferdriving.dataclasses.ObdConnectionInfo
import java.util.concurrent.CompletableFuture

/**
 * Displays a dialog for selecting the type of connection (WIFI or Bluetooth) to the OBD device.
 *
 * @param context The context in which the dialog should be displayed.
 * @param getRequestBluetooth A function that requests permissions. Takes a callback function to
 * execute if permission is denied.
 * @return A CompletableFuture that will be completed with [ObdConnectionInfo].
 */
fun showConnectionTypeDialog(
    context: Context,
    getRequestBluetooth: (onDenied: () -> Unit) -> () -> Unit
): CompletableFuture<ObdConnectionInfo> {
    val future = CompletableFuture<ObdConnectionInfo>()

    val dialog = createConnectionTypeDialog(context, getRequestBluetooth, future)
    dialog.setCancelable(false)
    dialog.show()

    return future
}

/**
 * Creates the dialog for requesting the type of connection.
 *
 * @param context The context in which the dialog should be displayed.
 * @param getRequestBluetooth A function that requests permissions. Takes a callback function to
 * execute if permission is denied.
 * @param future The CompletableFuture to complete.
 * @return The created AlertDialog.
 */
private fun createConnectionTypeDialog(
    context: Context,
    getRequestBluetooth: (onDenied: () -> Unit) -> () -> Unit,
    future: CompletableFuture<ObdConnectionInfo>
): AlertDialog {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(context.getString(R.string.connection_type_dialog_title))

    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_connection_type, null)
    builder.setView(dialogView)

    val radioGroup: RadioGroup = dialogView.findViewById(R.id.radioGroup)
    val portEditText: EditText = dialogView.findViewById(R.id.portEditText)
    val wifiRadioButton: RadioButton = dialogView.findViewById(R.id.wifiRadioButton)
    val bluetoothRadioButton: RadioButton = dialogView.findViewById(R.id.bluetoothRadioButton)

    builder.setPositiveButton(context.getString(R.string.positive_button)) { _, _ ->
        processUserInput(dialogView, future)
    }

    // Set up request Bluetooth permissions
    val request = getRequestBluetooth {
        wifiRadioButton.isChecked = true
        bluetoothRadioButton.isChecked = false
        bluetoothRadioButton.isClickable = false
    }

    radioGroup.setOnCheckedChangeListener { _, checkedId ->
        portEditText.visibility = if (checkedId == R.id.wifiRadioButton) View.VISIBLE else View.GONE

        if (checkedId == R.id.bluetoothRadioButton)
            request()
    }

    return builder.create()
}

/**
 * Processes the user input from the connection type dialog and completes the future.
 *
 * @param dialogView The view containing UI elements of the connection type dialog.
 * @param future The CompletableFuture to complete.
 */
private fun processUserInput(
    dialogView: View,
    future: CompletableFuture<ObdConnectionInfo>
) {
    val radioGroup: RadioGroup = dialogView.findViewById(R.id.radioGroup)
    val ipEditText: EditText = dialogView.findViewById(R.id.ipEditText)
    val portEditText: EditText = dialogView.findViewById(R.id.portEditText)
    val wifiRadioButton: RadioButton = dialogView.findViewById(R.id.wifiRadioButton)
    val bluetoothRadioButton: RadioButton = dialogView.findViewById(R.id.bluetoothRadioButton)

    val selectedRadioButtonId = radioGroup.checkedRadioButtonId

    if (selectedRadioButtonId != -1) {
        val ip = ipEditText.text.toString()

        val portString = portEditText.text.toString()
        val port = if (portString.isNotEmpty()) portString.toInt() else -1

        val isWifi = when (selectedRadioButtonId) {
            bluetoothRadioButton.id -> false
            wifiRadioButton.id -> true
            else -> false
        }

        val obdConnectionInfo = ObdConnectionInfo(
            address = ip,
            port = (if (port != -1) port else null),
            isWifi = isWifi
        )

        future.complete(obdConnectionInfo)
    }
}