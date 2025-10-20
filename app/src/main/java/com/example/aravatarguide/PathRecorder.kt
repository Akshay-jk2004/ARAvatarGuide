package com.example.aravatarguide

import com.google.ar.core.Pose
import kotlin.math.sqrt

class PathRecorder {

    private val recordedPoints = mutableListOf<Waypoint>()
    private var isRecording = false
    private var lastRecordedPosition: FloatArray? = null
    private var pathIndex = 0
    private var startPointName = ""
    private var destinationName = ""

    companion object {
        private const val MIN_DISTANCE_BETWEEN_POINTS = 0.3f // 30cm between waypoints
    }

    fun startRecording(startName: String) {
        recordedPoints.clear()
        isRecording = true
        pathIndex = 0
        startPointName = startName
        lastRecordedPosition = null
    }

    fun recordPoint(pose: Pose): Boolean {
        if (!isRecording) return false

        val currentPos = floatArrayOf(pose.tx(), pose.ty(), pose.tz())

        // Check if we should record this point
        val shouldRecord = lastRecordedPosition?.let { lastPos ->
            val distance = calculateDistance(lastPos, currentPos)
            distance >= MIN_DISTANCE_BETWEEN_POINTS
        } ?: true // Always record first point

        if (shouldRecord) {
            val isStart = recordedPoints.isEmpty()
            val name = if (isStart) startPointName else "Path Point ${pathIndex}"

            val waypoint = Waypoint(
                id = "WP$pathIndex",
                name = name,
                pose = pose,
                isStart = isStart,
                isEnd = false,
                index = pathIndex
            )

            recordedPoints.add(waypoint)
            lastRecordedPosition = currentPos
            pathIndex++
            return true
        }

        return false
    }

    fun stopRecording(destinationName: String): List<Waypoint> {
        isRecording = false
        this.destinationName = destinationName

        // Mark last point as endpoint
        if (recordedPoints.isNotEmpty()) {
            val lastPoint = recordedPoints.last()
            val endPoint = lastPoint.copy(
                name = destinationName,
                isEndPoint = true
            )
            recordedPoints[recordedPoints.lastIndex] = endPoint
        }

        return recordedPoints.toList()
    }

    fun isCurrentlyRecording() = isRecording

    fun getRecordedPointsCount() = recordedPoints.size

    fun getRecordedPoints() = recordedPoints.toList()

    private fun calculateDistance(pos1: FloatArray, pos2: FloatArray): Float {
        val dx = pos1[0] - pos2[0]
        val dy = pos1[1] - pos2[1]
        val dz = pos1[2] - pos2[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}