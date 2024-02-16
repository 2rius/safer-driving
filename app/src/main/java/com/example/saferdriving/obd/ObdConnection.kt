package com.example.saferdriving.obd

import android.content.Context
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import java.io.Closeable

abstract class ObdConnection : Closeable {
    protected lateinit var socket: Closeable
    protected lateinit var obdDeviceConnection: ObdDeviceConnection

    abstract suspend fun connect(context: Context)
    suspend fun run(
        command: ObdCommand
    ) : ObdResponse {
        return obdDeviceConnection.run(command)
    }

    override fun close() {
        socket.close()
    }
}