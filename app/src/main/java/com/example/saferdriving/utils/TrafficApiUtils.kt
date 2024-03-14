package com.example.saferdriving.utils

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.saferdriving.BuildConfig
import com.example.saferdriving.dataclasses.Location
import com.example.saferdriving.dataclasses.TrafficInfo
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
    Log.e("Traffic url", url)

    val stringReq = StringRequest(
        Request.Method.GET, url,
        { response ->
            // Log for debugging
            Log.e("HERE", "Traffic works")

            // Parse the JSON response
            val obj = JSONObject(response)
            val flowSegmentData = obj.getJSONObject("flowSegmentData")

            val frc = flowSegmentData.getString("frc")
            val currentSpeed = flowSegmentData.getInt("currentSpeed")
            val freeFlowSpeed = flowSegmentData.getInt("freeFlowSpeed")
            val currentTravelTime = flowSegmentData.getInt("currentTravelTime")
            val freeFlowTravelTime = flowSegmentData.getInt("freeFlowTravelTime")
            val confidence = flowSegmentData.getDouble("confidence").toFloat()
            val roadClosure = flowSegmentData.getBoolean("roadClosure")

            // Construct TrafficInfo object
            val trafficInfo = TrafficInfo(
                frc = frc,
                currentTrafficSpeed = currentSpeed,
                freeTrafficFlowSpeed = freeFlowSpeed,
                currentTrafficTravelTime = currentTravelTime,
                freeTrafficFlowTravelTime = freeFlowTravelTime,
                trafficConfidence = confidence,
                trafficRoadClosure = roadClosure
            )

            // Invoke callback with the constructed object
            callback(trafficInfo)
        }, { err ->
            Log.e("HERE", "Traffic info doesn't work")
            // TODO: Handle error
        })
    queue.add(stringReq)
}

private fun getUrl(location: Location): String {
    //val query = "point=${location.latitude}%2C&${location.longitude}&unit=KMPH&openLr=false&key=$API_KEY"
    //return BASE_URL + query

    val query = "point=${location.latitude},${location.longitude}&unit=KMPH&openLr=false&key=$API_KEY"
    return BASE_URL + query // Ensure BASE_URL ends with a '/' if '?' is not included
}
