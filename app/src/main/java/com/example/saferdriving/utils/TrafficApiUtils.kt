package com.example.saferdriving.utils

import android.location.Location
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.saferdriving.BuildConfig
import com.example.saferdriving.dataclasses.TrafficInfo
import com.example.saferdriving.dataclasses.WeatherInfo
import org.json.JSONObject

/**
 * Base URL for the Tomtom traffic API that should be extended with a given query.
 */
private const val BASE_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/relative0/10/json?"

/**
 * API Key for the Tomtom Traffic API.
 */
private const val API_KEY = BuildConfig.TRAFFIC_API_KEY

/**
 * Callback function to get the traffic flow info of a specific location.
 *
 * @param queue The queue that should handle the API call
 * @param location The location to which the traffic flow info should be found
 * @param callback The callback function that will be called when the request is done
 */
fun getTrafficInfo(
    queue: RequestQueue,
    location: Location,
    callback: (TrafficInfo) -> Unit
){
    val url = getUrl(location)

    val stringReq = StringRequest(
        Request.Method.GET, url,
        { response ->

            Log.e("HERE", "Traffic works")

            // get the JSON object
            val obj = JSONObject(response)

            // get the Array from obj of name - "data"
            val arr = obj.getJSONArray("flowSegmentData")

            // get the JSON object from the
            // array at index position 0
            val obj2 = arr.getJSONObject(0)

            // set the temperature and the city
            // name using getString() function
            val frc = obj2.getString("frc")
            val currentSpeed = obj2.getInt("currentSpeed")
            val freeFlowSpeed = obj2.getInt("freeFlowSpeed")
            val currentTravelTime = obj2.getInt("currentTravelTime")
            val freeFlowTravelTime = obj2.getInt("freeFlowTravelTime")
            val confidence = obj2.getDouble("confidence").toFloat()
            val roadClosure = obj2.getBoolean("roadClosure")

            val trafficInfo = TrafficInfo(
                frc = frc,
                currentSpeed = currentSpeed,
                freeFlowSpeed = freeFlowSpeed,
                currentTravelTime = currentTravelTime,
                freeFlowTravelTime = freeFlowTravelTime,
                confidence = confidence,
                roadClosure = roadClosure
            )

            callback(trafficInfo)
        }, { err ->
            Log.e("HERE", "Weather doesn't work")
            // TODO: Handle error
        })
    queue.add(stringReq)
}

private fun getUrl(location: Location): String {
    val query = "point=${location.latitude}%2C&${location.longitude}&unit=KMPH&openLr=false&key=$API_KEY"
    return BASE_URL + query
}
