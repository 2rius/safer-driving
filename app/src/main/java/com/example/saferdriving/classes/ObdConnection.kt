package com.example.saferdriving.classes

import android.content.Context
import com.example.saferdriving.dataclasses.Acceleration
import com.example.saferdriving.dataclasses.SpeedAndAcceleration
import com.example.saferdriving.utils.FuelTypeCommand
import com.example.saferdriving.utils.SpeedCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import java.io.Closeable

/**
 * Abstract class for interacting with an OBD-II device. It provides functionality for
 * establishing a connection to an OBD-II device, retrieve data from it, and close the
 * connection when no longer needed.
 */
abstract class ObdConnection : Closeable {
    protected lateinit var socket: Closeable
    protected lateinit var obdDeviceConnection: ObdDeviceConnection

    /**
     * Establishes a connection to the OBD-II device.
     * This method should be implemented by concrete subclasses depending on the type of OBD-II
     * device, namely, WIFI or Bluetooth.
     *
     * @param context The context of the application or activity.
     */
    abstract suspend fun connect(context: Context)

    suspend fun run(
        command: ObdCommand,
        useCache: Boolean = false,
        delayTime: Long = 0,
        maxRetries: Int = 5
    ): ObdResponse {
        return obdDeviceConnection.run(command, useCache, delayTime, maxRetries)
    }

    /**
     * Retrieves the current vehicle speed from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the speed.
     */
    suspend fun getSpeed(
        delayTime: Long = 0
    ): ObdResponse {
        return run(SpeedCommand(), delayTime = delayTime)
    }

    /**
     * Retrieves the vehicle fuel type from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the fuel type.
     */
    suspend fun getFuelType(
        delayTime: Long = 0
    ): ObdResponse {
        return run(FuelTypeCommand(), delayTime = delayTime)
    }

    /**
     * Retrieves the current vehicle RPM from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the RPM.
     */
    abstract suspend fun getRPM(delayTime: Long = 0): ObdResponse

    /**
     * Retrieves the current vehicle engine load from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the engine load.
     */
    abstract suspend fun getEngineLoad(delayTime: Long = 0): ObdResponse

    /**
     * Retrieves the current vehicle speed from the OBD-II device and calculated the acceleration.
     *
     * @param previousSpeed The previous recorded vehicle speed.
     * @param previousTime The time at which the previous speed was recorded.
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * @return A [SpeedAndAcceleration] object representing the current speed, acceleration and
     * the time captured.
     */
    suspend fun getSpeedAndAcceleration(
        previousSpeed: Int,
        previousTime: Long,
        delayTime: Long = 0
    ): SpeedAndAcceleration {
        val currentTime = System.currentTimeMillis()
        val currentSpeed = getSpeed(delayTime)

        val timeDifference = (currentTime - previousTime) / 1000.0

        // * 1000 / 3600 to convert km/h to m/s, / timedifference to calculate acceleration in m/s^2
        val acceleration = ((currentSpeed.value.toInt() - previousSpeed) * 1000.0 / 3600.0) / timeDifference

        return SpeedAndAcceleration(currentSpeed, Acceleration(acceleration), currentTime)
    }

    /**
     * Closes the connection to the OBD-II device.
     * Should be called when the connection is no longer needed.
     */
    override fun close() {
        socket.close()
    }

    /**
     * Sample code for implementing ObdConnection:
     *         var speed: ObdResponse? = null
    var acceleration: Acceleration? = null

    val requestPermission: (() -> Unit) -> () -> Unit = { onDenied -> getRequestPermission(BLUETOOTH_PERMISSIONS, onDenied = onDenied) }

    val futureConnection = showConnectionTypeDialog(this, requestPermission)

    // futureConnection.thenAccept will run concurrently, so code beneath this will run at the same time
    futureConnection.thenAccept { connection ->
    GlobalScope.launch(Dispatchers.IO) {
    try {
    connection.connect(this@MainActivity)

    var prevTime = System.currentTimeMillis()
    var prevSpeed = connection.getSpeed()

    while (true) {
    val response = connection.getSpeedAndAcceleration(prevSpeed, prevTime, 500)

    speed = response.speed
    acceleration = response.acceleration
    prevTime = response.timeCaptured
    prevSpeed = response.speed

    }
    } catch (e: Exception) {
    // Handle error, usually problem with connecting to OBD device
    }
    }
    }
     */
}