package com.example.aravatarguide

import android.opengl.Matrix
import kotlin.math.atan2
import kotlin.math.sqrt

class NavigationHelper {

    data class NavigationInfo(
        val distance: Float,
        val direction: Float,
        val nextWaypoint: Waypoint?,
        val arrowPosition: FloatArray,
        val arrowRotation: Float
    )

    fun calculateNavigation(
        cameraPos: FloatArray,
        cameraRotation: FloatArray,
        destination: Waypoint
    ): NavigationInfo {
        val dx = destination.x - cameraPos[0]
        val dy = destination.y - cameraPos[1]
        val dz = destination.z - cameraPos[2]

        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        val angleToTarget = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

        // Arrow position: 1.5m in front of camera, at ground level
        val arrowDistance = 1.5f
        val arrowX = cameraPos[0] + dx / distance * arrowDistance
        val arrowY = cameraPos[1] - 1.4f // Ground level
        val arrowZ = cameraPos[2] + dz / distance * arrowDistance

        return NavigationInfo(
            distance = distance,
            direction = angleToTarget,
            nextWaypoint = destination,
            arrowPosition = floatArrayOf(arrowX, arrowY, arrowZ),
            arrowRotation = angleToTarget
        )
    }

    fun createArrowMatrix(
        position: FloatArray,
        rotationY: Float,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): FloatArray {
        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2])
        Matrix.rotateM(modelMatrix, 0, -rotationY, 0f, 1f, 0f) // Negative for correct direction
        Matrix.scaleM(modelMatrix, 0, 0.8f, 0.8f, 0.8f) // Scale arrow

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        return mvpMatrix
    }

    fun getDirectionText(distance: Float): String {
        return when {
            distance < 1.0f -> "You have arrived!"
            distance < 2.0f -> "Almost there - ${String.format("%.1f", distance)}m"
            distance < 5.0f -> "Continue forward - ${String.format("%.1f", distance)}m"
            distance < 10.0f -> "Keep walking - ${String.format("%.1f", distance)}m"
            else -> "Destination ahead - ${String.format("%.0f", distance)}m"
        }
    }
}