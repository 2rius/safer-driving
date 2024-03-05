package com.example.saferdriving.dataclasses

import com.github.eltonvs.obd.command.ObdResponse

/**
 * A data class that represents speed, acceleration, and the time it is captured.
 *
 * @property speed The speed, which is obtained from an OBD-II device response.
 * @property acceleration The acceleration, which is calculated with the difference between 2 captures speeds.
 * @property timeCaptured The timestamp this data was captured, in milliseconds.
 */
data class SpeedAndAcceleration(
    val speed: ObdResponse,
    val acceleration: Acceleration,
    val timeCaptured: Long
)

