package com.example.saferdriving.dataclasses

data class TrafficInfo(
    var frc: String? = null,                        //RoadType
    var currentTrafficSpeed: Int? = null,          //The current average speed at the selected point in KMPH
    var freeTrafficFlowSpeed: Int? = null,         //The free flow speed expected under ideal conditions, in KMPH
    var currentTrafficTravelTime: Int? = null,     //Current travel time in seconds based on fused real-time measurements
    var freeTrafficFlowTravelTime: Int? = null,    //The travel time in seconds which would be expected under ideal free flow conditions.
    var trafficConfidence: Float? = null,          //The confidence is a measure of the quality of the provided travel time and speed. A value ranges between 0 and 1 where 1 means full confidence, meaning that the response contains the highest quality data. Lower values indicate the degree that the response may vary from the actual conditions on the road.
    var trafficRoadClosure: Boolean = false        //This indicates if the road is closed to traffic or not.
)
