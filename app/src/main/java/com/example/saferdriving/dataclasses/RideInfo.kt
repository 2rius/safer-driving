package com.example.saferdriving.dataclasses

data class RideInfo(
    var userAge: Int? = null,
    var userDrivingExperience: Int? = null,
    var userResidenceCity: String? = null,
    var userJob: String? = null,
    var recordedWithSound: Boolean = false,

    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherDescription: String? = null,

    var amountOfMinutesDriving: Int? = null,
    var fuelType: String? = null,

    var amountOfSpeedings: Int? = null,
    var amountOfSpeedingsHighway: Int? = null,
    var amountOfSpeedingsCountryRoad: Int? = null,
    var amountOfSpeedingsCity: Int? = null,

    var overallTopSpeed: Int? = null,
    var topSpeedHighway: Int? = null,
    var topSpeedCountryRoad: Int? = null,
    var topSpeedCity: Int? = null,

    var totalAmountOfSecondsSpeeding: Int? = null,
    var amountOfSecondsSpeedingInHighway: Int? = null,
    var amountOfSecondsSpeedingInCountryRoad: Int? = null,
    var amountOfSecondsSpeedingInCity: Int? = null,

    var totalAverageSecondsSpeeding: Int? = null,
    var averageSecondsSpeedingInHighway: Int? = null,
    var averageSecondsSpeedingInCountryRoad: Int? = null,
    var averageSecondsSpeedingInCity: Int? = null,
)
