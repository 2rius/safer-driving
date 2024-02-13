package com.example.saferdriving.obd

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.bytesToInt

class SpeedCommand : ObdCommand() {
    override val tag = "SPEED"
    override val name = "Vehicle Speed"
    override val mode = "01"
    override val pid = "0D"

    override val defaultUnit = "Km/h"
    // Have to remove index 2 and 3 to make it work
    override val handler = { it: ObdRawResponse -> bytesToInt(it.bufferedValue.filterIndexed { index, _ -> index != 2 && index != 3 }.toIntArray(), bytesToProcess = 1).toString() }
}