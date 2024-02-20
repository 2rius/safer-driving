package com.example.saferdriving.dataClasses

import java.util.concurrent.locks.Condition

data class Driver(
    var id: Int? = null,
    var age: Int? = null,
    var drivingExperience: Int? = null,
    var residence: String? = null,
    var job: String? = null,
    var amountOfMinutesDriving: Int? = null,
    var amountOfSpeedings: Int? = null,
    var overallTopSpeed: Int? = null,
    var amountOfSpeedingsCountryRoad: Int? = null,
    var topSpeedCountryRoad: Int? = null,
    var amountOfSpeedingsHighway: Int? = null,
    var topSpeedHighway: Int? = null,
    var amountOfSpeedingsCity: Int? = null,
    var topSpeedCity: Int? = null,
    var averageAirTemperature: Int? = null,
    var averageFuelConsumption: Int? = null,
    var vehicleAcceleration: Int? = null,
    var totalAmountOfMinutesSpeeding: Int? = null,
    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherCondition: String? = null
)
