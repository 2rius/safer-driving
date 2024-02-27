package com.example.saferdriving.dataClasses

import com.example.saferdriving.enums.RoadType

data class LocationOfSpeeding(
    var id: Int? = null,
    var lat: Double? = null,
    var long: Double? = null,
    var roadName: String? = null,
    var topSpeed: Int? = null,
    var roadType: RoadType? = null
)
