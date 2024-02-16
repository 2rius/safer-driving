package com.example.saferdriving.utilities

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.saferdriving.R
import com.example.saferdriving.obd.BluetoothObdConnection
import com.example.saferdriving.obd.ObdConnection
import com.example.saferdriving.obd.WifiObdConnection
import java.util.concurrent.CompletableFuture

fun showConnectionTypeDialog(
    context: Context,
    getRequestBluetooth: (onDenied: () -> Unit) -> () -> Unit
): CompletableFuture<ObdConnection> {
    val future = CompletableFuture<ObdConnection>()

    val dialog = createConnectionTypeDialog(context, getRequestBluetooth, future)
    dialog.setCancelable(false)
    dialog.show()

    return future
}

private fun createConnectionTypeDialog(
    context: Context,
    getRequestBluetooth: (onDenied: () -> Unit) -> () -> Unit,
    future: CompletableFuture<ObdConnection>
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

private fun processUserInput(
    dialogView: View,
    future: CompletableFuture<ObdConnection>
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

        val selectedConnection = when (selectedRadioButtonId) {
            bluetoothRadioButton.id -> {
                if (ip.isEmpty()) BluetoothObdConnection() else BluetoothObdConnection(ip)
            }
            wifiRadioButton.id -> when {
                ip.isEmpty() && port == -1 -> WifiObdConnection()
                port == -1 -> WifiObdConnection(ip)
                ip.isEmpty() -> WifiObdConnection(port = port)
                else -> WifiObdConnection(ip, port)
            }
            else -> null
        }

        future.complete(selectedConnection)
    }
}