package com.example.saferdriving.utilities

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.saferdriving.dataClasses.Road
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.services.LiveDataService.Companion.TAG
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Base URL for the Overpass API that should be extended with a given query.
 */
private const val BASE_URL = "https://overpass-api.de/api/interpreter?data="

/**
 * Radius that it should check for highways nearby.
 */
private const val RADIUS = 10

/**
 * Callback function to get the road of a specific latitude and longitude.
 *
 * @param queue The queue that should handle the API call
 * @param latitude The latitude to which a road should be found
 * @param longitude The longitude to which a road should be found
 * @param callback The callback function that will be called when the request is done
 */
fun getRoad(
    queue: RequestQueue,
    latitude: Double,
    longitude: Double,
    callback: (Road) -> Unit
) {
    val reqUrl = getUrl(latitude, longitude)
    var roadType: RoadType? = null
    var tags: JSONObject? = null
    var name = "Unknown"
    var speedLimit: Int? = null

    Log.i(TAG, reqUrl)

    val stringReq = StringRequest(Request.Method.GET, reqUrl, { response ->
        val fullObj = JSONObject(response)
        val elements = fullObj.getJSONArray("elements")

        if (elements.length() < 1) {
            callback(Road(latitude, longitude, name, RoadType.RURAL))
            return@StringRequest
        } else {
            for (i in 0 until elements.length()) {
                val roadJson = elements.getJSONObject(0)

                tags = roadJson.getJSONObject("tags")
                roadType = getRoadType(tags!!)

                if (roadType != null)
                    break
            }
        }

        if (roadType == null)
            roadType = RoadType.RURAL

        if (tags != null && tags!!.has("name"))
            name = tags!!.getString("name")

        if (tags != null && tags!!.has("maxspeed"))
            speedLimit = tags!!.getString("maxspeed").toIntOrNull()

        if (speedLimit == null)
            speedLimit = roadType!!.defaultSpeedLimit

        val road = Road(latitude, longitude, name, roadType!!, speedLimit!!)

        callback(road)
    }, { err ->
        Log.i(TAG, err.toString())
    })

    queue.add(stringReq)
}

/**
 * Gets the URL for an API request given a latitude and longitude.
 *
 * @param latitude The latitude of the location to be requested.
 * @param longitude The longitude of the location to be requested.
 * @return The full url that can be called in an API request.
 */
private fun getUrl(latitude: Double, longitude: Double): String {
    val query = """
            [out:json];
            way
              (around:$RADIUS,$latitude,$longitude)
              [highway]; 
            out center;
        """.trimIndent()

    val encodedQuery = URLEncoder.encode(query, "UTF-8")

    return BASE_URL + encodedQuery
}

/**
 * Gets the roadtype from the specified tags on the OSM object.
 * Does this from the specifications at https://wiki.openstreetmap.org/wiki/Default_speed_limits.
 *
 * @param tags The tags that should be used to determine the road type
 * @return
 */
private fun getRoadType(tags: JSONObject): RoadType? {
    return when {
        tags.has("has sidewalk") -> RoadType.CITY
        tags.has("has no sidewalk") -> RoadType.RURAL
        tags.has("living_street") && tags.getString("living_street") == "yes" -> RoadType.CITY
        tags.has("cyclestreet") && tags.getString("cyclestreet") == "yes" -> RoadType.CITY
        tags.has("motorroad") && tags.getString("motorroad") == "yes" -> RoadType.RURAL
        tags.has("lit") -> when (tags.getString("lit")) {
            "yes" -> RoadType.CITY
            "no" -> RoadType.RURAL
            else -> null
        }

        tags.has("rural") -> when (tags.getString("rural")) {
            "yes" -> RoadType.RURAL
            "no" -> RoadType.CITY
            else -> null
        }

        tags.has("highway") -> when (tags.getString("highway")) {
            "living_street", "residential" -> RoadType.CITY
            "motorway", "motorway_link" -> RoadType.MOTORWAY
            else -> null
        }

        else -> null
    }
}