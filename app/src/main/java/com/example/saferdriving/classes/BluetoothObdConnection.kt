package com.example.saferdriving.classes

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.saferdriving.utils.BluetoothFuelLevelCommand
import com.example.saferdriving.utils.BluetoothLoadCommand
import com.example.saferdriving.utils.BluetoothRPMCommand
import com.example.saferdriving.utils.FuelTypeCommand
import com.example.saferdriving.utils.SpeedCommand
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * OBD-II connection that is established over Bluetooth. It provides functionality for
 * establishing a connection to an OBD-II device, retrieve data from it, and close the
 * connection when no longer needed.
 */
class BluetoothObdConnection(private val ip: String = "00:1D:A5:05:74:E0") : ObdConnection() {
    /**
     * Companion object defining constants for the class, namely DEFAULT_RFCOMM_UUID.
     */
    companion object {
        const val DEFAULT_RFCOMM_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    override val commands = listOf(
        SpeedCommand(),
        FuelTypeCommand(),
        BluetoothRPMCommand(),
        BluetoothFuelLevelCommand(),
        BluetoothLoadCommand()
    )

    override fun getInputStream(): InputStream {
        return (socket as BluetoothSocket).inputStream
    }
    override fun getOutputStream(): OutputStream {
        return (socket as BluetoothSocket).outputStream
    }

    /**
     * Establishes a Bluetooth connection with the OBD-II device.
     * It is executed asynchronously in IO dispatcher to avoid blocking the main thread.
     * It creates a socket to the specified bluetooth address of the OBD-II device and initializes
     * input and output streams for a connection
     *
     * @param context The context of the application or activity.
     * @throws SecurityException If bluetooth permission is not granted.
     * @throws IOException If the input or output stream is null.
     */
    override suspend fun connect(
        context: Context
    ) {
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
                bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEFAULT_RFCOMM_UUID))
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

    override suspend fun getRPM(
        delayTime: Long
    ): ObdResponse {
        return run(BluetoothRPMCommand(), delayTime = delayTime)
    }

    override suspend fun getEngineLoad(
        delayTime: Long
    ): ObdResponse {
        return run(BluetoothLoadCommand(), delayTime = delayTime)
    }

    override suspend fun getFuelLevel(
        delayTime: Long
    ): ObdResponse {
        return run(BluetoothFuelLevelCommand(), delayTime = delayTime)
    }
}