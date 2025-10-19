package com.example.aravatarguide

import android.content.Context
import android.opengl.GLES20
import java.io.IOException
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
        try {
            // For simplicity, we'll create a simple arrow shape programmatically
            // This avoids complex GLB parsing which requires additional libraries
            createSimpleArrow()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createSimpleArrow() {
        // Create a simple 3D arrow pointing forward
        // Arrow points in +Z direction
        val vertices = floatArrayOf(
            // Arrow head (triangle)
            0.0f, 0.1f, 0.3f,   // top
            -0.15f, 0.0f, 0.0f,  // left
            0.15f, 0.0f, 0.0f,   // right

            // Arrow shaft (rectangle)
            -0.05f, 0.0f, 0.0f,  // shaft left front
            0.05f, 0.0f, 0.0f,   // shaft right front
            -0.05f, 0.0f, -0.3f, // shaft left back
            0.05f, 0.0f, -0.3f   // shaft right back
        )

        val indices = shortArrayOf(
            // Head triangle
            0, 1, 2,
            // Shaft quad (two triangles)
            3, 4, 5,
            4, 6, 5
        )

        indexCount = indices.size

        // Create buffers
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