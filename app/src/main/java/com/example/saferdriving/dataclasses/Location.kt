package com.example.saferdriving.dataclasses

import ch.hsr.geohash.GeoHash

const val GEOHASH_PRECISION = 7

data class Location(
    val latitude: Double,
    val longitude: Double,
    val geohash: String = GeoHash.geoHashStringWithCharacterPrecision(
        latitude,
        longitude,
        GEOHASH_PRECISION
    )
)
