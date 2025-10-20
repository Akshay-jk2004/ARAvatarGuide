package com.example.aravatarguide

import com.google.ar.core.Pose

data class Waypoint(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isStartPoint: Boolean = false,
    val isEndPoint: Boolean = false,
    val pathIndex: Int = 0  // Order in the path
) {
    constructor(id: String, name: String, pose: Pose, isStart: Boolean = false, isEnd: Boolean = false, index: Int = 0) : this(
        id = id,
        name = name,
        x = pose.tx(),
        y = pose.ty(),
        z = pose.tz(),
        isStartPoint = isStart,
        isEndPoint = isEnd,
        pathIndex = index
    )

    fun toSaveString(): String {
        return "$id|$name|$x|$y|$z|$timestamp|$isStartPoint|$isEndPoint|$pathIndex"
    }

    fun distanceTo(other: Waypoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    companion object {
        fun fromSaveString(data: String): Waypoint? {
            return try {
                val parts = data.split("|")
                Waypoint(
                    id = parts[0],
                    name = parts[1],
                    x = parts[2].toFloat(),
                    y = parts[3].toFloat(),
                    z = parts[4].toFloat(),
                    timestamp = parts[5].toLong(),
                    isStartPoint = parts[6].toBoolean(),
                    isEndPoint = parts[7].toBoolean(),
                    pathIndex = parts[8].toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}