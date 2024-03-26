package com.example.saferdriving.utils

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

/**
 * Represents a fixed OBD command for retrieving vehicle speed. The same functionality as
 * [com.github.eltonvs.obd.command.engine.SpeedCommand] but fixed for our specific needs.
 */
class SpeedCommand : ObdCommand() {
    override val tag = "SPEED"
    override val name = "Vehicle Speed"
    override val mode = "01"
    override val pid = "0D"

    override val defaultUnit = "Km/h"
    override val handler = { it: ObdRawResponse -> it.bufferedValue.last().toString() }
}

class BluetoothRPMCommand : ObdCommand() {
    override val tag = "ENGINE_RPM"
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse ->
        if (it.bufferedValue.size > 11)
            ((256 * it.bufferedValue[11] + it.bufferedValue[10]) / 4).toString()
        else if (it.value.length % 2 == 1) {
            val realBufferedValue = it.processedValue.takeLast(4).chunked(2) { cs -> cs.toString().toInt(radix = 16) }.toIntArray()
            ((256 * realBufferedValue[0] + realBufferedValue[1]) / 4).toString()
        } else
            ((256 * it.bufferedValue[it.bufferedValue.size - 2] + it.bufferedValue.last()) / 4).toString()
    }
}

class WifiRPMCommand : ObdCommand() {
    override val tag = "ENGINE_RPM"
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse -> ((256 * it.bufferedValue[it.bufferedValue.size - 2] + it.bufferedValue.last()) / 4).toString() }
}

class FuelTypeCommand : ObdCommand() {
    override val tag = "FUEL_TYPE"
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

class BluetoothLoadCommand : ObdCommand() {
    override val tag = "ENGINE_LOAD"
    override val name = "Engine Load"
    override val mode = "01"
    override val pid = "04"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse ->
        if (it.value.length % 2 == 0)
            "%.1f".format(((it.bufferedValue.last()) / 255.0) * 100)
        else {
            "%.1f".format(((it.processedValue.takeLast(2).toInt(radix = 16)) / 255.0) * 100)
        }
    }
}

class WifiLoadCommand : ObdCommand() {
    override val tag = "ENGINE_LOAD"
    override val name = "Engine Load"
    override val mode = "01"
    override val pid = "04"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(100.0 / 255 * it.bufferedValue.last()) }
}

class BluetoothFuelRateCommand : ObdCommand() {
    override val tag = "FUEL_RATE"
    override val name = "Fuel Rate"
    override val mode = "01"
    override val pid = "5E"

    override val defaultUnit = "L/h"
    override val handler = { it: ObdRawResponse ->
        if (it.bufferedValue.size > 11)
            "%.2f".format(((256.0 * it.bufferedValue[11].toFloat() + it.bufferedValue[10].toFloat()) / 20.0))
        else if (it.value.length % 2 == 1) {
            val realBufferedValue = it.processedValue.takeLast(4).chunked(2) { cs -> cs.toString().toInt(radix = 16) }.toIntArray()
            "%.2f".format(((256.0 * realBufferedValue[0].toFloat() + realBufferedValue[1].toFloat()) / 20.0))
        } else
            "%.2f".format(((256.0 * it.bufferedValue[it.bufferedValue.size - 2].toFloat() + it.bufferedValue.last().toFloat()) / 20.0))
    }
}