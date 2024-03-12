package com.example.saferdriving.utils

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.saferdriving.BuildConfig
import com.example.saferdriving.dataclasses.Location
import com.example.saferdriving.dataclasses.WeatherInfo
import org.json.JSONObject

/**
 * Base URL for the Weatherbit API that should be extended with a given query.
 */
private const val BASE_URL = "https://api.weatherbit.io/v2.0/current?"

/**
 * API Key for the Weather API.
 */
private const val API_KEY = BuildConfig.WEATHER_API_KEY

/**
 * Callback function to get the weather info of a specific location.
 *
 * @param queue The queue that should handle the API call
 * @param location The location to which the weather should be found
 * @param callback The callback function that will be called when the request is done
 */
fun getWeatherInfo(
    queue: RequestQueue,
    location: Location,
    callback: (WeatherInfo) -> Unit
) {
    val url = getUrl(location)

    // Request a string response
    // from the provided URL.
    val stringReq = StringRequest(
        Request.Method.GET, url,
        { response ->

            Log.e("HERE", "Weather works")

            // get the JSON object
            val obj = JSONObject(response)

            // get the Array from obj of name - "data"
            val arr = obj.getJSONArray("data")

            // get the JSON object from the
            // array at index position 0
            val obj2 = arr.getJSONObject(0)

            // set the temperature and the city
            // name using getString() function
            val temperature = obj2.getString("temp").toFloatOrNull()?.toInt()
            val pressure = obj2.getString("pres").toFloatOrNull()?.toInt()
            val windSpeed = obj2.getString("wind_spd").toFloatOrNull()?.toInt()
            val weatherDescriptionFromJSON = obj2.getJSONObject("weather").getString("description")

            val weatherInfo = WeatherInfo(
                temperature,
                pressure,
                windSpeed,
                weatherDescriptionFromJSON
            )

            callback(weatherInfo)
        }, { err ->
            Log.e("HERE", "Weather doesn't work")
            // TODO: Handle error
        })
    queue.add(stringReq)
}

/**
 * Gets the URL for an API request given a latitude and longitude.
 *
 * @param location The location to be requested.
 * @return The full url that can be called in an API request.
 */
private fun getUrl(location: Location): String {
    val query = "lat=${location.latitude}&lon=${location.longitude}&key=$API_KEY"

    return BASE_URL + query
}