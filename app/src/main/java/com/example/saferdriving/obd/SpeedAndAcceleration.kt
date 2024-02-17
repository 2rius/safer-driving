package com.example.saferdriving.obd

import com.github.eltonvs.obd.command.ObdResponse

data class SpeedAndAcceleration(
    val speed: ObdResponse,
    val acceleration: Acceleration,
    val timeCaptured: Long
)

data class Acceleration(
    val value: Double,
    val unit: String = "m/s^2"
) {
    val formattedValue: String get() = value.toString() + unit
}