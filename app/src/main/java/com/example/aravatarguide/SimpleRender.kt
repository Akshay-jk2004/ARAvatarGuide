package com.example.aravatarguide

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SimpleRenderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    private val vertexBuffer: FloatBuffer

    // Circle vertices
    private val circleCoords = FloatArray(CIRCLE_SEGMENTS * 3)

    companion object {
        private const val COORDS_PER_VERTEX = 3
        private const val CIRCLE_SEGMENTS = 36
        private const val CIRCLE_RADIUS = 0.1f
    }

    init {
        // Generate circle coordinates
        for (i in 0 until CIRCLE_SEGMENTS) {
            val angle = 2.0f * Math.PI.toFloat() * i / CIRCLE_SEGMENTS
            circleCoords[i * 3] = CIRCLE_RADIUS * kotlin.math.cos(angle)
            circleCoords[i * 3 + 1] = 0f
            circleCoords[i * 3 + 2] = CIRCLE_RADIUS * kotlin.math.sin(angle)
        }

        val bb = ByteBuffer.allocateDirect(circleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(circleCoords)
        vertexBuffer.position(0)
    }

    fun createOnGlThread() {
        val vertexShader = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
               gl_Position = u_MvpMatrix * a_Position;
            }
        """.trimIndent()

        val fragmentShader = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
               gl_FragColor = u_Color;
            }
        """.trimIndent()

        val vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShaderHandle)
        GLES20.glAttachShader(program, fragmentShaderHandle)
        GLES20.glLinkProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MvpMatrix")
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray, x: Float, y: Float, z: Float, color: FloatArray) {
        GLES20.glUseProgram(program)

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)

        val mvpMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glLineWidth(5f)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, CIRCLE_SEGMENTS)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}