package com.example.saferdriving.dataclasses

import com.example.saferdriving.enums.RoadType

/**
 * Data class representing a road collected from OSM API.
 *
 * @property latitude Latitude of location recorded
 * @property longitude Longitude of location recorded
 * @property name Name of the found road
 * @property type The type of the given road
 * @property speedLimit The speed limit of the road (defaults to the default speed limit of type)
 */
data class Road(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val type: RoadType,
    val speedLimit: Int = type.defaultSpeedLimit
)