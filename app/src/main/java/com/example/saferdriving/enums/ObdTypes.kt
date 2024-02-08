package com.example.saferdriving.enums

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Socket
import java.util.*

enum class ObdTypes {
    WIFI {
        override suspend fun connect(context: Context, ip: String, port: Int): ObdDeviceConnection {
            return withContext(Dispatchers.IO) {
                val socket = Socket(ip, port)
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                if (inputStream != null && outputStream != null) {
                    ObdDeviceConnection(inputStream, outputStream)
                } else {
                    throw IOException("Input or output stream is null")
                }
            }
        }
    },
    BLUETOOTH {
        override suspend fun connect(context: Context, ip: String, port: Int): ObdDeviceConnection {
            return withContext(Dispatchers.IO) {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothDevice = bluetoothManager.adapter.getRemoteDevice(ip)
                val socket = if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Handle permissions if needed
                    throw IOException("Bluetooth permission not granted")
                } else {
                    bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                }

                socket.connect()
                val inputStream = socket.inputStream
                val outputStream = socket.outputStream

                if (inputStream != null && outputStream != null) {
                    ObdDeviceConnection(inputStream, outputStream)
                } else {
                    throw IOException("Input or output stream is null")
                }
            }
        }
//        ("11:22:33:44:55:66", -1)
    };

    abstract suspend fun connect(
        context: Context, ip: String = "192.168.0.10", port: Int = 3500
    ): ObdDeviceConnection
}