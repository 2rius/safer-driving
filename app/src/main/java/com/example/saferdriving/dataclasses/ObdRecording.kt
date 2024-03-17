package com.example.saferdriving.dataclasses

data class ObdRecording(
    //obd
    var speed: Int? = null,
    var acceleration: Double? = null,
    var rpm: Double? = null,
    var fuelType: String? = null,
    var fuelLevel: Double? = null,
    var engineLoadLevel: Double? = null,
    var recordedWithSound: Boolean = false,

    //basic
    var age: Int? = null,
    var drivingExperience: Int? = null,
    var residence: String? = null,
    var job: String? = null,

    //traffic
    var frc: String? = null,                       //RoadType
    var currentTrafficSpeed: Int? = null,          //The current average speed at the selected point in KMPH
    var freeTrafficFlowSpeed: Int? = null,         //The free flow speed expected under ideal conditions, in KMPH
    var currentTrafficTravelTime: Int? = null,     //Current travel time in seconds based on fused real-time measurements
    var freeTrafficFlowTravelTime: Int? = null,    //The travel time in seconds which would be expected under ideal free flow conditions.
    var trafficConfidence: Float? = null,          //The confidence is a measure of the quality of the provided travel time and speed. A value ranges between 0 and 1 where 1 means full confidence, meaning that the response contains the highest quality data. Lower values indicate the degree that the response may vary from the actual conditions on the road.
    var trafficRoadClosure: Boolean = false,        //This indicates if the road is closed to traffic or not.

    //weather
    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherDescription: String? = null,

    //speeding
    var lat: Double? = null,
    var long: Double? = null,
    var geohash: String? = null,
    var roadName: String? = null,
    var roadType: String? = null,
    var speedLimit: Int? = null
)
