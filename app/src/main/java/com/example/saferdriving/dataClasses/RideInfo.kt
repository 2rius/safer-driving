package com.example.saferdriving.dataClasses

data class RideInfo(
    var amountOfMinutesDriving: Int? = null,
    var averageFuelConsumption: Int? = null,

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
