package com.example.saferdriving.dataclasses

data class TrafficInfo(
    var frc: String? = null,                //RoadType
    var currentSpeed: Int? = null,          //The current average speed at the selected point in KMPH
    var freeFlowSpeed: Int? = null,         //The free flow speed expected under ideal conditions, in KMPH
    var currentTravelTime: Int? = null,     //Current travel time in seconds based on fused real-time measurements
    var freeFlowTravelTime: Int? = null,    //The travel time in seconds which would be expected under ideal free flow conditions.
    var confidence: Float? = null,          //The confidence is a measure of the quality of the provided travel time and speed. A value ranges between 0 and 1 where 1 means full confidence, meaning that the response contains the highest quality data. Lower values indicate the degree that the response may vary from the actual conditions on the road.
    var roadClosure: Boolean = false        //This indicates if the road is closed to traffic or not.
)
