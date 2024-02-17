package com.example.saferdriving.obd

import android.content.Context
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import java.io.Closeable

abstract class ObdConnection : Closeable {
    protected lateinit var socket: Closeable
    protected lateinit var obdDeviceConnection: ObdDeviceConnection

    abstract suspend fun connect(context: Context)

    suspend fun getSpeed(
        delayTime: Long = 0
    ): ObdResponse {
        return obdDeviceConnection.run(SpeedCommand(), delayTime = delayTime)
    }

    suspend fun getSpeedAndAcceleration(
        previousSpeed: ObdResponse,
        previousTime: Long,
        delayTime: Long = 0
    ): SpeedAndAcceleration {
        val currentTime = System.currentTimeMillis()
        val currentSpeed = getSpeed(delayTime)

        val timeDifference = (currentTime - previousTime) / 1000.0

        // * 1000 / 3600 to convert km/h to m/s, / timedifference to calculate acceleration in m/s^2
        val acceleration = ((currentSpeed.value.toInt() - previousSpeed.value.toInt()) * 1000.0 / 3600.0) / timeDifference

        return SpeedAndAcceleration(currentSpeed, Acceleration(acceleration), currentTime)
    }

    suspend fun getFuelConsumption() {
        // TODO
    }

    override fun close() {
        socket.close()
    }
}