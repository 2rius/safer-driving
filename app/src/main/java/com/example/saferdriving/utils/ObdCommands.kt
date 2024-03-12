package com.example.saferdriving.utils

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.bytesToInt

/**
 * Represents a fixed OBD command for retrieving vehicle speed for our Wi-Fi OBD. The same
 * functionality as [com.github.eltonvs.obd.command.engine.SpeedCommand], but removes 2 indexes
 * from the buffered value to function correctly for our specific OBD devices.
 */
class WifiSpeedCommand : ObdCommand() {
    override val tag = "SPEED"
    override val name = "Vehicle Speed"
    override val mode = "01"
    override val pid = "0D"

    override val defaultUnit = "Km/h"
    // Have to remove index 2 and 3 to make it work
    override val handler = { it: ObdRawResponse -> bytesToInt(it.bufferedValue.filterIndexed { index, _ -> index != 2 && index != 3 }.toIntArray(), bytesToProcess = 1).toString() }
}

/**
 * Represents a fixed OBD command for retrieving vehicle speed for our Bluetooth OBD. The same
 * functionality as [com.github.eltonvs.obd.command.engine.SpeedCommand], but removes 2 indexes
 * from the buffered value to function correctly for our specific OBD devices.
 */
class BluetoothSpeedCommand : ObdCommand() {
    override val tag = "SPEED"
    override val name = "Vehicle Speed"
    override val mode = "01"
    override val pid = "0D"

    override val defaultUnit = "Km/h"
    // Have to remove index 2 and 3 to make it work
    override val handler = { it: ObdRawResponse -> it.bufferedValue.last().toString() }
}