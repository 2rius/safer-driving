package com.example.saferdriving.classes

import android.content.Context
import com.example.saferdriving.utils.BluetoothFuelLevelCommand
import com.example.saferdriving.utils.FuelTypeCommand
import com.example.saferdriving.utils.SpeedCommand
import com.example.saferdriving.utils.WifiLoadCommand
import com.example.saferdriving.utils.WifiRPMCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * OBD-II connection that is established over a WIFI network. It provides functionality for
 *  * establishing a connection to an OBD-II device, retrieve data from it, and close the
 *  * connection when no longer needed.
 */
class WifiObdConnection(private val ip: String = "192.168.0.109", private val port: Int = 35000) : ObdConnection() {
    override val commands = listOf(
        SpeedCommand(),
        FuelTypeCommand(),
        WifiRPMCommand(),
        LoadCommand(),
    )

    override fun getInputStream(): InputStream {
        return (socket as Socket).inputStream
    }
    override fun getOutputStream(): OutputStream {
        return (socket as Socket).outputStream
    }

    /**
     * Establishes a WIFI connection with the OBD-II device.
     * It is executed asynchronously in IO dispatcher to avoid blocking the main thread.
     * It creates a socket to the specified IP and port of the OBD-II device and initializes
     * input and output streams for a connection
     *
     * @param context The context of the application or activity, not used to establish WIFI
     * connection but necessary to establish Bluetooth connection.
     * @throws IOException If the input or output stream is null.
     */
    override suspend fun connect(
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            val wifiSocket = Socket()
            wifiSocket.connect(InetSocketAddress(ip,port), 5000)

            val inputStream = wifiSocket.getInputStream()
            val outputStream = wifiSocket.getOutputStream()

            if (inputStream != null && outputStream != null) {
                socket = wifiSocket
                obdDeviceConnection = ObdDeviceConnection(inputStream, outputStream)
            } else {
                wifiSocket.close()
                throw IOException("Input or output stream is null")
            }
        }
    }

    override suspend fun getRPM(
        delayTime: Long
    ): ObdResponse {
        return run(WifiRPMCommand(), delayTime = delayTime)
    }

    override suspend fun getEngineLoad(
        delayTime: Long
    ): ObdResponse {
        return run(WifiLoadCommand(), delayTime = delayTime)
    }

    override suspend fun getFuelLevel(
        delayTime: Long
    ): ObdResponse {
        /**
         * FuelLevel is not supported on the emulator we are using,
         * hence we create our own ObdResponse.
         */
        return ObdResponse(
            command = BluetoothFuelLevelCommand(),
            rawResponse = ObdRawResponse("", 0),
            "42.0"
        )
    }
}