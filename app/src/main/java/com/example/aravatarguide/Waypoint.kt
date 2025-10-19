package com.example.aravatarguide

import com.google.ar.core.Pose

data class Waypoint(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Constructor from ARCore Pose
    constructor(id: String, name: String, pose: Pose) : this(
        id = id,
        name = name,
        x = pose.tx(),
        y = pose.ty(),
        z = pose.tz()
    )

    // Convert back to string for saving
    fun toSaveString(): String {
        return "$id|$name|$x|$y|$z|$timestamp"
    }

    companion object {
        // Create from saved string
        fun fromSaveString(data: String): Waypoint? {
            return try {
                val parts = data.split("|")
                Waypoint(
                    id = parts[0],
                    name = parts[1],
                    x = parts[2].toFloat(),
                    y = parts[3].toFloat(),
                    z = parts[4].toFloat(),
                    timestamp = parts[5].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}