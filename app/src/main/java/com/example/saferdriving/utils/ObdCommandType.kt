package com.example.saferdriving.utils

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

/**
 * Represents a fixed OBD command for retrieving vehicle speed. The same functionality as
 * [com.github.eltonvs.obd.command.engine.SpeedCommand] but fixed for our specific needs.
 */

enum class ObdCommandType(val tag: String) {
    SPEED("SPEED"),
    FUEL_TYPE("FUEL_TYPE"),
    RPM("ENGINE_RPM"),
    LOAD("ENGINE_LOAD"),
    FUEL_LEVEL("FUEL_LEVEL")
}

class SpeedCommand : ObdCommand() {
    override val tag = ObdCommandType.SPEED.tag
    override val name = "Vehicle Speed"
    override val mode = "01"
    override val pid = "0D"

    override val defaultUnit = "Km/h"
    override val handler = { it: ObdRawResponse -> it.bufferedValue.last().toString() }
}

class FuelTypeCommand : ObdCommand() {
    override val tag = ObdCommandType.FUEL_TYPE.tag
    override val name = "Fuel Type"
    override val mode = "01"
    override val pid = "51"

    override val handler = { it: ObdRawResponse -> getFuelType(it.bufferedValue.last()) }

    private fun getFuelType(code: Int): String = when (code) {
        0x00 -> "Not Available"
        0x01 -> "Gasoline"
        0x02 -> "Methanol"
        0x03 -> "Ethanol"
        0x04 -> "Diesel"
        0x05 -> "GPL/LGP"
        0x06 -> "Natural Gas"
        0x07 -> "Propane"
        0x08 -> "Electric"
        0x09 -> "Biodiesel + Gasoline"
        0x0A -> "Biodiesel + Methanol"
        0x0B -> "Biodiesel + Ethanol"
        0x0C -> "Biodiesel + GPL/LGP"
        0x0D -> "Biodiesel + Natural Gas"
        0x0E -> "Biodiesel + Propane"
        0x0F -> "Biodiesel + Electric"
        0x10 -> "Biodiesel + Gasoline/Electric"
        0x11 -> "Hybrid Gasoline"
        0x12 -> "Hybrid Ethanol"
        0x13 -> "Hybrid Diesel"
        0x14 -> "Hybrid Electric"
        0x15 -> "Hybrid Mixed"
        0x16 -> "Hybrid Regenerative"
        else -> "Unknown"
    }
}

class BluetoothRPMCommand : ObdCommand() {
    override val tag = ObdCommandType.RPM.tag
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse -> ((256 * it.bufferedValue[11] + it.bufferedValue[10]) / 4).toString() }
}

class WifiRPMCommand : ObdCommand() {
    override val tag = ObdCommandType.RPM.tag
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse -> ((256 * it.bufferedValue[it.bufferedValue.size - 2] + it.bufferedValue.last()) / 4).toString() }
}

class BluetoothLoadCommand : ObdCommand() {
    override val tag = ObdCommandType.LOAD.tag
    override val name = "Engine Load"
    override val mode = "01"
    override val pid = "04"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(100.0 / 255 * it.bufferedValue[5]) }
}

class WifiLoadCommand : ObdCommand() {
    override val tag = ObdCommandType.LOAD.tag
    override val name = "Engine Load"
    override val mode = "01"
    override val pid = "04"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(100.0 / 255 * it.bufferedValue.last()) }
}

class BluetoothFuelLevelCommand : ObdCommand() {
    override val tag = ObdCommandType.FUEL_LEVEL.tag
    override val name = "Fuel Level"
    override val mode = "01"
    override val pid = "2F"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(100.0 / 255 * it.bufferedValue[it.bufferedValue.size - 2]) }
}