package com.example.saferdriving.dataclasses

import com.example.saferdriving.enums.RoadType

data class SpeedingRecording(
    var age: Int? = null,
    var drivingExperience: Int? = null,
    var residence: String? = null,
    var job: String? = null,

    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherDescription: String? = null,

    //traffic
    var frc: String? = null,                       //RoadType
    var currentTrafficSpeed: Int? = null,          //The current average speed at the selected point in KMPH
    var freeTrafficFlowSpeed: Int? = null,         //The free flow speed expected under ideal conditions, in KMPH
    var currentTrafficTravelTime: Int? = null,     //Current travel time in seconds based on fused real-time measurements
    var freeTrafficFlowTravelTime: Int? = null,    //The travel time in seconds which would be expected under ideal free flow conditions.
    var trafficConfidence: Float? = null,          //The confidence is a measure of the quality of the provided travel time and speed. A value ranges between 0 and 1 where 1 means full confidence, meaning that the response contains the highest quality data. Lower values indicate the degree that the response may vary from the actual conditions on the road.
    var trafficRoadClosure: Boolean = false,        //This indicates if the road is closed to traffic or not.

    var lat: Double? = null,
    var long: Double? = null,
    var roadName: String? = null,
    var topSpeed: Int? = null,
    var roadType: RoadType? = null,
    var amountOfSecondsSpeeding: Int? = null,
    var speedLimit: Int? = null,
    var geohash: String? = null
)
