package com.example.saferdriving.singletons

import com.android.volley.RequestQueue
import com.example.saferdriving.dataclasses.*
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.utils.getWeatherInfo
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database


class FirebaseManager private constructor() {
    companion object {
        const val DB_URL = "https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/"
        const val DEFAULT_DRIVERID = "Unknown"

        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
    }

    private val db = Firebase.database(DB_URL).reference

    private var driverId = DEFAULT_DRIVERID
    private var withSound = false
    private var basicInfo = BasicInfo()
    private var weatherInfo = WeatherInfo()
    private var traffic = TrafficInfo()

    private fun getFirebaseReferenceObd(): DatabaseReference {
        return db.child("obd").child(driverId)
    }

    private fun getFirebaseReferenceRideInfo(): DatabaseReference {
        return db.child("rideInfo").child(driverId)
    }

    private fun getFirebaseReferenceSpeedings(): DatabaseReference {
        return db.child("speedings").child(driverId)
    }



    fun addDriver() {
        driverId = db.child("rideInfo").push().key ?: "Unknown"
    }

    fun setWithSound(withSound: Boolean) {
        this.withSound = withSound
    }

    fun getWithSound(): Boolean{
        return withSound
    }

    fun addObdRecording(
        timeOfRecording: Long,
        speedAndAcceleration: SpeedAndAcceleration,
        trafficInfo: TrafficInfo,
        road: Road,
        location: Location
    ): ObdRecording {
        val obdRecording = ObdRecording(
            speedAndAcceleration.speed.value.toInt(),
            speedAndAcceleration.acceleration.value,
            null,
            withSound,

            basicInfo.age,
            basicInfo.drivingExperience,
            basicInfo.residence,
            basicInfo.job,

            trafficInfo.frc,
            trafficInfo.currentTrafficSpeed,
            trafficInfo.freeTrafficFlowSpeed,
            trafficInfo.currentTrafficSpeed,
            trafficInfo.freeTrafficFlowTravelTime,
            trafficInfo.trafficConfidence,
            trafficInfo.trafficRoadClosure,

            weatherInfo.airPressure,
            weatherInfo.airTemperature,
            weatherInfo.windSpeed,
            weatherInfo.weatherDescription,

            location.longitude,
            location.latitude,

            road.name,
            road.type.toString(),
            road.speedLimit
        )

        getFirebaseReferenceObd().child(timeOfRecording.toString()).setValue(obdRecording)

        return obdRecording
    }

    fun addSpeedingRecording(
        timeOfRecording: Long,
        location: Location,
        road: Road,
        topSpeed: Int,
        secondsSpeeding: Int
    ): SpeedingRecording {
        val speedingRecording = SpeedingRecording(
            location.latitude,
            location.longitude,
            road.name,
            topSpeed,
            road.type,
            secondsSpeeding
        )

        getFirebaseReferenceSpeedings().child(timeOfRecording.toString()).setValue(speedingRecording)

        return speedingRecording
    }

    fun setBasicInfo(
        age: Int,
        drivingExperience: Int,
        residence: String,
        job: String
    ) {
        val basicInfo = BasicInfo(
            age,
            drivingExperience,
            residence,
            job
        )
        this.basicInfo = basicInfo
    }

    fun setWeatherInfo(
        requestQueue: RequestQueue,
        location: Location
    ) {
        getWeatherInfo(requestQueue, location) { weatherInfo ->
            this.weatherInfo = weatherInfo
        }

    }
    fun addRideInfo(
        startTime: Long,
        topSpeed: Int,
        topSpeedHighway: Int,
        topSpeedCountryRoad: Int,
        topSpeedCity: Int,
        speedingList: List<SpeedingRecording>
    ) {
        val highwaySpeedingList = speedingList.filter { speeding -> speeding.roadType == RoadType.MOTORWAY }
        val countryRoadSpeedingList = speedingList.filter { speeding -> speeding.roadType == RoadType.RURAL }
        val citySpeedingList = speedingList.filter { speeding -> speeding.roadType == RoadType.CITY }

        val totalSecondsSpeeding = speedingList.fold(0) { acc, speeding -> acc + speeding.amountOfSecondsSpeeding!! }
        val totalSecondsSpeedingHighWay = highwaySpeedingList.fold(0) { acc, speeding -> acc + speeding.amountOfSecondsSpeeding!! }
        val totalSecondsSpeedingCountryRoad = countryRoadSpeedingList.fold(0) { acc, speeding -> acc + speeding.amountOfSecondsSpeeding!! }
        val totalSecondsSpeedingCity = citySpeedingList.fold(0) { acc, speeding -> acc + speeding.amountOfSecondsSpeeding!! }

        val totalAverageSecondsSpeeding = if (speedingList.isNotEmpty()) (totalSecondsSpeeding / speedingList.size) else 0
        val averageSecondsSpeedingInHighway = if (highwaySpeedingList.isNotEmpty()) (totalSecondsSpeedingHighWay / highwaySpeedingList.size) else 0
        val averageSecondsSpeedingInCountryRoad = if (countryRoadSpeedingList.isNotEmpty()) (totalSecondsSpeedingCountryRoad / countryRoadSpeedingList.size) else 0
        val averageSecondsSpeedingInCity = if (citySpeedingList.isNotEmpty()) (totalSecondsSpeedingCity / citySpeedingList.size) else 0

        val rideInfo = RideInfo(
            basicInfo.age,
            basicInfo.drivingExperience,
            basicInfo.residence,
            basicInfo.job,

            amountOfMinutesDriving = ((System.currentTimeMillis() - startTime) / 60000).toInt(),
            averageFuelConsumption = 0,

            amountOfSpeedings = speedingList.size,
            amountOfSpeedingsHighway = highwaySpeedingList.size,
            amountOfSpeedingsCountryRoad = countryRoadSpeedingList.size,
            amountOfSpeedingsCity = citySpeedingList.size,

            overallTopSpeed = topSpeed,
            topSpeedHighway = topSpeedHighway,
            topSpeedCountryRoad = topSpeedCountryRoad,
            topSpeedCity = topSpeedCity,

            totalAmountOfSecondsSpeeding = totalSecondsSpeeding,
            amountOfSecondsSpeedingInHighway = totalSecondsSpeedingHighWay,
            amountOfSecondsSpeedingInCountryRoad = totalSecondsSpeedingCountryRoad,
            amountOfSecondsSpeedingInCity = totalSecondsSpeedingCity,

            totalAverageSecondsSpeeding = totalAverageSecondsSpeeding,
            averageSecondsSpeedingInHighway = averageSecondsSpeedingInHighway,
            averageSecondsSpeedingInCountryRoad = averageSecondsSpeedingInCountryRoad,
            averageSecondsSpeedingInCity = averageSecondsSpeedingInCity
        )

        getFirebaseReferenceRideInfo().setValue(rideInfo)
    }
}