package com.example.aravatarguide

import kotlin.math.sqrt

class PathFinder {

    fun findPath(
        currentPosition: FloatArray,
        destination: Waypoint,
        allWaypoints: List<Waypoint>
    ): List<Waypoint> {
        if (allWaypoints.isEmpty()) return listOf(destination)

        // Simple path: find waypoints along the way
        val path = mutableListOf<Waypoint>()

        // Find nearest waypoint to current position
        val nearest = allWaypoints.minByOrNull { waypoint ->
            distance(currentPosition[0], currentPosition[2], waypoint.x, waypoint.z)
        }

        if (nearest != null && nearest != destination) {
            path.add(nearest)
        }

        // Add destination
        path.add(destination)

        return path
    }

    private fun distance(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dz = z2 - z1
        return sqrt(dx * dx + dz * dz)
    }

    fun getNextWaypoint(
        currentPosition: FloatArray,
        path: List<Waypoint>
    ): Waypoint? {
        if (path.isEmpty()) return null

        // Return first waypoint in path that's more than 0.5m away
        return path.firstOrNull { waypoint ->
            distance(currentPosition[0], currentPosition[2], waypoint.x, waypoint.z) > 0.5f
        } ?: path.last()
    }
}