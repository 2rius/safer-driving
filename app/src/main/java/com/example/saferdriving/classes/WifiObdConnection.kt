package com.example.saferdriving.classes

import android.content.Context
import com.example.saferdriving.utils.FuelLevelCommand
import com.example.saferdriving.utils.LoadCommand
import com.example.saferdriving.utils.WifiLoadCommand
import com.example.saferdriving.utils.WifiRPMCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * OBD-II connection that is established over a WIFI network. It provides functionality for
 *  * establishing a connection to an OBD-II device, retrieve data from it, and close the
 *  * connection when no longer needed.
 */
class WifiObdConnection(private val ip: String = "192.168.0.112", private val port: Int = 35000) : ObdConnection() {
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
        return ObdResponse(
            command = FuelLevelCommand(),
            rawResponse = ObdRawResponse("", 0),
            "42.0"
        )
    }
}