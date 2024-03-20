package com.example.saferdriving.classes

import android.content.Context
import com.example.saferdriving.utils.FuelTypeCommand
import com.example.saferdriving.utils.ObdCommandType
import com.example.saferdriving.utils.SpeedCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.RegexPatterns
import com.github.eltonvs.obd.command.removeAll
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import kotlin.system.measureTimeMillis

/**
 * Abstract class for interacting with an OBD-II device. It provides functionality for
 * establishing a connection to an OBD-II device, retrieve data from it, and close the
 * connection when no longer needed.
 */
abstract class ObdConnection : Closeable {
    protected lateinit var socket: Closeable
    protected lateinit var obdDeviceConnection: ObdDeviceConnection

    protected abstract val commands: List<ObdCommand>

    protected abstract fun getInputStream(): InputStream
    protected abstract fun getOutputStream(): OutputStream

    /**
     * Establishes a connection to the OBD-II device.
     * This method should be implemented by concrete subclasses depending on the type of OBD-II
     * device, namely, WIFI or Bluetooth.
     *
     * @param context The context of the application or activity.
     */
    abstract suspend fun connect(context: Context)

    protected suspend fun run(
        command: ObdCommand,
        useCache: Boolean = false,
        delayTime: Long = 0,
        maxRetries: Int = 5
    ): ObdResponse {
        return obdDeviceConnection.run(command, useCache, delayTime, maxRetries)
    }

    private suspend fun runMultiple(
        commands: List<ObdCommand>,
        delayTime: Long = 0,
        maxRetries: Int = 5
    ): Map<String, ObdResponse> = runBlocking {
        val inputStream = getInputStream()
        val outputStream = getOutputStream()

        val concatinatedRawCommands = commands.joinToString(separator = "\r") { it.rawCommand } + "\r"

        var rawData: String

        val elapsedTime = measureTimeMillis {
            sendCommands(outputStream, concatinatedRawCommands, delayTime)
            rawData = readRawData(inputStream, maxRetries)
        }

        val rawResponses = rawData.split("\r")

        commands.foldIndexed(mutableMapOf()) { index, acc, command ->
            val rawResponse = if (rawResponses.size > index) rawResponses[index] else ""
            val obdRawResponse = ObdRawResponse(rawResponse, elapsedTime = elapsedTime)
            acc.apply { put(command.tag, command.handleResponse(obdRawResponse)) }
        }
    }

    private suspend fun getFromType(
        commandType: ObdCommandType
    ): ObdResponse {
        return when (commandType) {
            ObdCommandType.SPEED -> getSpeed()
            ObdCommandType.FUEL_TYPE -> getFuelType()
            ObdCommandType.RPM -> getRPM()
            ObdCommandType.LOAD -> getEngineLoad()
            ObdCommandType.FUEL_LEVEL -> getFuelLevel()
        }
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
     * Retrieves the vehicle fuel level from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the fuel level.
     */
    abstract suspend fun getFuelLevel(delayTime: Long = 0): ObdResponse

    /**
     * Retrieves the current vehicle engine load from the OBD-II device.
     *
     * @param delayTime The delay time, in milliseconds, to wait before executing the command.
     * Defaults to 0.
     * @return An [ObdResponse] object that includes value, unit, command and rawresponse of
     * the engine load.
     */
    abstract suspend fun getEngineLoad(delayTime: Long = 0): ObdResponse

    suspend fun getMultiple(
        commandTypes: List<ObdCommandType>,
        delayTime: Long = 0
    ): Map<String, ObdResponse> {
        val commandsToRequest = mutableListOf<ObdCommand>()
        val allTagResults = mutableMapOf<String, ObdResponse>()

        commandTypes.forEach { type ->
            val foundCommand = commands.find { cmd -> cmd.tag == type.tag }
            if (foundCommand != null)
                commandsToRequest.add(foundCommand)
            else {
                allTagResults[type.tag] = getFromType(type)
            }
        }

        val multiResponses = runMultiple(commandsToRequest, delayTime = delayTime)
        allTagResults.putAll(multiResponses)

        return allTagResults
    }

    private suspend fun sendCommands(
        outputStream: OutputStream,
        concatenatedRawCommands: String,
        delayTime: Long
    ) {
        withContext(Dispatchers.IO) {
            outputStream.write(concatenatedRawCommands.toByteArray())
            outputStream.flush()
            if (delayTime > 0) {
                delay(delayTime)
            }
        }
    }

    private suspend fun readRawData(
        inputStream: InputStream,
        maxRetries: Int
    ): String = runBlocking {
        var b: Byte
        var c: Char
        val res = StringBuffer()
        var retriesCount = 0

        withContext(Dispatchers.IO) {
            // read until '>' arrives OR end of stream reached (-1)
            while (retriesCount <= maxRetries) {
                if (inputStream.available() > 0) {
                    b = inputStream.read().toByte()
                    if (b < 0) {
                        break
                    }
                    c = b.toInt().toChar()
                    if (c == '>') {
                        break
                    }
                    res.append(c)
                } else {
                    retriesCount += 1
                    delay(500)
                }
            }
            removeAll(RegexPatterns.SEARCHING_PATTERN, res.toString()).trim()
        }
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