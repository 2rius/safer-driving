package com.example.saferdriving.dataClasses

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