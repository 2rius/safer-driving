package com.example.saferdriving.dataClasses

data class WeatherInfo(
    var airPressure: Int? = null,
    var airTemperature: Int? = null,
    var windSpeed: Int? = null,
    var weatherDescription: String? = null
)
