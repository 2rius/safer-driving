package com.example.saferdriving.obd

import android.content.Context
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Socket

class WifiObdConnection(private val ip: String = "192.168.0.112", private val port: Int = 35000) : ObdConnection() {

    override suspend fun connect(context: Context) {
        withContext(Dispatchers.IO) {
            val wifiSocket = Socket(ip, port)
            val inputStream = wifiSocket.getInputStream()
            val outputStream = wifiSocket.getOutputStream()

            if (inputStream != null && outputStream != null) {
                socket = wifiSocket
                obdDeviceConnection = ObdDeviceConnection(inputStream, outputStream)
            } else {
                wifiSocket.close()
                throw IOException()
            }
        }
    }
}