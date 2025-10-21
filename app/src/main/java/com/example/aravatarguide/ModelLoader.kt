package com.example.aravatarguide

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class ModelLoader(private val context: Context) {

    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var program = 0
    private var positionHandle = 0
    private var mvpMatrixHandle = 0
    private var colorHandle = 0
    private var indexCount = 0

    fun loadModel(assetPath: String) {
        // For simplicity, we'll use a pre-defined arrow shape
        // GLB parsing requires complex libraries, so we'll create a nice arrow programmatically
        if (assetPath.contains("arrow")) {
            createArrowShape()
        } else if (assetPath.contains("avatar")) {
            createAvatarShape()
        }
    }

    private fun createArrowShape() {
        // 3D Arrow pointing forward (better visibility)
        val vertices = floatArrayOf(
            // Arrow head (large triangle)
            0.0f, 0.1f, 0.5f,      // tip
            -0.25f, 0.1f, 0.0f,    // left wing
            0.25f, 0.1f, 0.0f,     // right wing

            // Arrow shaft
            -0.1f, 0.1f, 0.0f,
            0.1f, 0.1f, 0.0f,
            -0.1f, 0.1f, -0.4f,
            0.1f, 0.1f, -0.4f,

            // Bottom layer (3D effect)
            0.0f, 0.05f, 0.5f,
            -0.25f, 0.05f, 0.0f,
            0.25f, 0.05f, 0.0f,
            -0.1f, 0.05f, 0.0f,
            0.1f, 0.05f, 0.0f,
            -0.1f, 0.05f, -0.4f,
            0.1f, 0.05f, -0.4f
        )

        val indices = shortArrayOf(
            // Top surface
            0, 1, 2,  // head
            3, 4, 5,  // shaft
            4, 6, 5,
            // Bottom surface
            7, 8, 9,
            10, 11, 12,
            11, 13, 12,
            // Sides (connect top and bottom)
            0, 7, 1,
            1, 7, 8,
            0, 2, 7,
            2, 9, 7
        )

        indexCount = indices.size

        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(vertices)
        vertexBuffer?.position(0)

        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer?.put(indices)
        indexBuffer?.position(0)
    }

    private fun createAvatarShape() {
        // Simple 3D humanoid avatar
        val vertices = floatArrayOf(
            // Head (cube)
            -0.15f, 1.8f, -0.15f,
            0.15f, 1.8f, -0.15f,
            0.15f, 1.8f, 0.15f,
            -0.15f, 1.8f, 0.15f,
            -0.15f, 1.5f, -0.15f,
            0.15f, 1.5f, -0.15f,
            0.15f, 1.5f, 0.15f,
            -0.15f, 1.5f, 0.15f,

            // Body (rectangular)
            -0.2f, 1.5f, -0.1f,
            0.2f, 1.5f, -0.1f,
            0.2f, 1.5f, 0.1f,
            -0.2f, 1.5f, 0.1f,
            -0.2f, 0.8f, -0.1f,
            0.2f, 0.8f, -0.1f,
            0.2f, 0.8f, 0.1f,
            -0.2f, 0.8f, 0.1f,

            // Arms
            -0.4f, 1.4f, 0.0f,
            -0.2f, 1.4f, 0.0f,
            -0.4f, 0.9f, 0.0f,
            -0.2f, 0.9f, 0.0f,
            0.2f, 1.4f, 0.0f,
            0.4f, 1.4f, 0.0f,
            0.2f, 0.9f, 0.0f,
            0.4f, 0.9f, 0.0f,

            // Legs
            -0.15f, 0.8f, 0.0f,
            -0.05f, 0.8f, 0.0f,
            -0.15f, 0.0f, 0.0f,
            -0.05f, 0.0f, 0.0f,
            0.05f, 0.8f, 0.0f,
            0.15f, 0.8f, 0.0f,
            0.05f, 0.0f, 0.0f,
            0.15f, 0.0f, 0.0f
        )

        val indices = shortArrayOf(
            // Head cube
            0, 1, 2, 0, 2, 3,  // top
            4, 5, 6, 4, 6, 7,  // bottom
            0, 1, 5, 0, 5, 4,  // front
            2, 3, 7, 2, 7, 6,  // back
            0, 3, 7, 0, 7, 4,  // left
            1, 2, 6, 1, 6, 5,  // right

            // Body
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            8, 9, 13, 8, 13, 12,
            10, 11, 15, 10, 15, 14,

            // Arms
            16, 17, 19, 16, 19, 18,
            20, 21, 23, 20, 23, 22,

            // Legs
            24, 25, 27, 24, 27, 26,
            28, 29, 31, 28, 31, 30
        )

        indexCount = indices.size

        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(vertices)
        vertexBuffer?.position(0)

        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer?.put(indices)
        indexBuffer?.position(0)
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

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        if (vertexBuffer == null || indexBuffer == null) return

        GLES20.glUseProgram(program)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}