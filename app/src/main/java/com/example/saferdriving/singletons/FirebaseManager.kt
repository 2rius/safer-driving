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
        location: Location,
        rpm: Double,
        loadLevel: Double,
        fuelType: String
    ): ObdRecording {
        val obdRecording = ObdRecording(
            speed = speedAndAcceleration.speed.value.toInt(),
            acceleration = speedAndAcceleration.acceleration.value,
            rpm = rpm,
            fuelType = fuelType,
            engineLoadLevel = loadLevel,
            recordedWithSound = withSound,

            userAge = basicInfo.age,
            userDrivingExperience = basicInfo.drivingExperience,
            userResidenceCity = basicInfo.residenceCity,
            userJob = basicInfo.job,

            frc = trafficInfo.frc,
            currentTrafficSpeed = trafficInfo.currentTrafficSpeed,
            freeTrafficFlowSpeed = trafficInfo.freeTrafficFlowSpeed,
            currentTrafficTravelTime = trafficInfo.currentTrafficTravelTime,
            freeTrafficFlowTravelTime = trafficInfo.freeTrafficFlowTravelTime,
            trafficConfidence = trafficInfo.trafficConfidence,
            trafficRoadClosure = trafficInfo.trafficRoadClosure,

            airPressure = weatherInfo.airPressure,
            airTemperature = weatherInfo.airTemperature,
            windSpeed = weatherInfo.windSpeed,
            weatherDescription = weatherInfo.weatherDescription,

            long = location.longitude,
            lat = location.latitude,
            geohash = location.geohash,

            roadName = road.name,
            roadType = road.type.toString(),
            speedLimit = road.speedLimit
        )

        getFirebaseReferenceObd().child(timeOfRecording.toString()).setValue(obdRecording)

        return obdRecording
    }

    fun addSpeedingRecording(
        timeOfRecording: Long,
        location: Location,
        road: Road,
        trafficInfo: TrafficInfo,
        topSpeed: Int,
        topRPM: Double,
        topEngineLevel: Double,
        fuelType: String,
        secondsSpeeding: Int
    ): SpeedingRecording {
        val speedingRecording = SpeedingRecording(
            userAge = basicInfo.age,
            userDrivingExperience = basicInfo.drivingExperience,
            userResidenceCity = basicInfo.residenceCity,
            userJob = basicInfo.job,
            recordedWithSound = withSound,

            airPressure = weatherInfo.airPressure,
            airTemperature = weatherInfo.airTemperature,
            windSpeed = weatherInfo.windSpeed,
            weatherDescription = weatherInfo.weatherDescription,

            frc = trafficInfo.frc,
            currentTrafficSpeed = trafficInfo.currentTrafficSpeed,
            freeTrafficFlowSpeed = trafficInfo.freeTrafficFlowSpeed,
            currentTrafficTravelTime = trafficInfo.currentTrafficTravelTime,
            freeTrafficFlowTravelTime = trafficInfo.freeTrafficFlowTravelTime,
            trafficConfidence = trafficInfo.trafficConfidence,
            trafficRoadClosure = trafficInfo.trafficRoadClosure,

            lat = location.latitude,
            long = location.longitude,
            geohash = location.geohash,
            roadName = road.name,
            topSpeed = topSpeed,
            topRPM = topRPM,
            topEngineLoadLevel = topEngineLevel,
            roadType = road.type,
            amountOfSecondsSpeeding = secondsSpeeding,
            speedLimit = road.speedLimit,
            fuelType = fuelType
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
            age = age,
            drivingExperience = drivingExperience,
            residenceCity = residence,
            job = job
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
        fuelType: String,
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
            userAge = basicInfo.age,
            userDrivingExperience = basicInfo.drivingExperience,
            userResidenceCity = basicInfo.residenceCity,
            userJob = basicInfo.job,
            recordedWithSound = withSound,

            airPressure = weatherInfo.airPressure,
            airTemperature = weatherInfo.airTemperature,
            windSpeed = weatherInfo.windSpeed,
            weatherDescription = weatherInfo.weatherDescription,

            amountOfMinutesDriving = ((System.currentTimeMillis() - startTime) / 60000).toInt(),
            fuelType = fuelType,

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