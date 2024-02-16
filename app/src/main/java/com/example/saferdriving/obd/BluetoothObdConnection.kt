package com.example.saferdriving.obd

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothObdConnection(private val ip: String = "00:1D:A5:05:74:E0") : ObdConnection() {
    override suspend fun connect(context: Context) {
        return withContext(Dispatchers.IO) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothDevice = bluetoothManager.adapter.getRemoteDevice(ip)

            val bluetoothSocket = if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Bluetooth permission not granted")
            } else {
                bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            }

            bluetoothSocket.connect()
            val inputStream = bluetoothSocket.inputStream
            val outputStream = bluetoothSocket.outputStream

            if (inputStream != null && outputStream != null) {
                socket = bluetoothSocket
                obdDeviceConnection = ObdDeviceConnection(inputStream, outputStream)
            } else {
                bluetoothSocket.close()
                throw IOException("Input or output stream is null")
            }
        }
    }
}