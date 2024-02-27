package com.example.saferdriving.enums

/**
 * Enum class for differentiating between the 3 types of roads.
 *
 * @property defaultSpeed The default speeds on the different roads (in Denmark).
 */
enum class RoadType(val defaultSpeedLimit: Int) {
    CITY(50),
    RURAL(80),
    MOTORWAY(130);

    override fun toString(): String {
        return when (this) {
            CITY -> "By"
            RURAL -> "Landevej"
            MOTORWAY -> "Moterway"
        }
    }
}