package com.example.saferdriving.dataClasses

data class RideInfo(
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
    var totalAmountOfMinutesSpeeding: Int? = null,
    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherDescription: String? = null,
    var amountOfMinutesSpeedingInHighway: Int? = null,
    var amountOfMinutesSpeedingInCountryRoad: Int? = null,
    var amountOfMinutesSpeedingInCity: Int? = null,
    var averageMinutesSpeedingInHighway: Int? = null,
    var averageMinutesSpeedingInCountryRoad: Int? = null,
    var averageMinutesSpeedingInCity: Int? = null,
)