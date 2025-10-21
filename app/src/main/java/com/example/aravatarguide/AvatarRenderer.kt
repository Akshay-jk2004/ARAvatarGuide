package com.example.aravatarguide

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class AvatarRenderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    private var vertexBuffer: FloatBuffer
    private var indexBuffer: FloatBuffer

    // Simple 3D humanoid shape (stick figure style)
    private val avatarVertices = floatArrayOf(
        // Head (sphere approximation)
        0.0f, 1.8f, 0.0f,
        0.1f, 1.75f, 0.0f,
        -0.1f, 1.75f, 0.0f,
        0.0f, 1.75f, 0.1f,
        0.0f, 1.75f, -0.1f,

        // Body
        0.0f, 1.6f, 0.0f,  // neck
        0.0f, 1.0f, 0.0f,  // waist

        // Arms
        -0.3f, 1.4f, 0.0f, // left hand
        0.3f, 1.4f, 0.0f,  // right hand

        // Legs
        -0.15f, 0.0f, 0.0f, // left foot
        0.15f, 0.0f, 0.0f   // right foot
    )

    init {
        val bb = ByteBuffer.allocateDirect(avatarVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(avatarVertices)
        vertexBuffer.position(0)

        val indices = floatArrayOf(
            0f, 1f, 2f, 3f, 4f, // head points
            5f, 6f,             // body line
            5f, 7f,             // left arm
            5f, 8f,             // right arm
            6f, 9f,             // left leg
            6f, 10f             // right leg
        )

        val ib = ByteBuffer.allocateDirect(indices.size * 4)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asFloatBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)
    }

    fun createOnGlThread() {
        val vertexShader = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
               gl_Position = u_MvpMatrix * a_Position;
               gl_PointSize = 15.0;
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

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray, x: Float, y: Float, z: Float) {
        GLES20.glUseProgram(program)

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, 0.3f, 0.3f, 0.3f)

        val mvpMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Avatar color (blue)
        val avatarColor = floatArrayOf(0.2f, 0.5f, 1.0f, 1.0f)
        GLES20.glUniform4fv(colorHandle, 1, avatarColor, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // Draw head as points
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 5)

        // Draw body lines
        GLES20.glLineWidth(8f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 5, 6)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}