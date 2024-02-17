package com.example.saferdriving.obd

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

/**
 * Data class that represents acceleration.
 *
 * @property value The value of the acceleration.
 * @property unit The unit in which the acceleration is measured.
 */
data class Acceleration(
    val value: Double,
    val unit: String = "m/s^2"
) {
    /**
     * Returns a string representation of the acceleration value along with its unit.
     */
    val formattedValue: String get() = value.toString() + unit
}