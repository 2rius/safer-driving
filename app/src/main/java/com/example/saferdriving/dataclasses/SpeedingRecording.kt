package com.example.saferdriving.dataclasses

import com.example.saferdriving.enums.RoadType

data class SpeedingRecording(
    var lat: Double? = null,
    var long: Double? = null,
    var roadName: String? = null,
    var topSpeed: Int? = null,
    var roadType: RoadType? = null,
    var amountOfSecondsSpeeding: Int? = null
)
