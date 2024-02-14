package com.example.saferdriving.obd

import android.content.Context
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Socket

class WifiObdConnection(private val ip: String, private val port: Int): ObdConnection {
    override suspend fun connect(context: Context): ObdDeviceConnection {
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
}