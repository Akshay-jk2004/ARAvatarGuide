package com.example.aravatarguide

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class PathManager(private val context: Context) {

    private val pathFile = File(context.filesDir, "campus_path.txt")

    fun savePath(waypoints: List<Waypoint>): Boolean {
        return try {
            val writer = FileWriter(pathFile)
            waypoints.forEach { waypoint ->
                writer.write(waypoint.toSaveString() + "\n")
            }
            writer.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadPath(): List<Waypoint> {
        val waypoints = mutableListOf<Waypoint>()

        if (!pathFile.exists()) {
            return waypoints
        }

        try {
            val reader = BufferedReader(FileReader(pathFile))
            reader.useLines { lines ->
                lines.forEach { line ->
                    Waypoint.fromSaveString(line)?.let { waypoint ->
                        waypoints.add(waypoint)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return waypoints
    }

    fun hasPath(): Boolean {
        return pathFile.exists() && pathFile.length() > 0
    }

    fun clearPath(): Boolean {
        return try {
            if (pathFile.exists()) {
                pathFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPathSummary(): String {
        val waypoints = loadPath()
        return if (waypoints.isEmpty()) {
            "No path saved"
        } else {
            "Saved path with ${waypoints.size} waypoints:\n" +
                    waypoints.joinToString("\n") { "• ${it.name}" }
        }
    }
}