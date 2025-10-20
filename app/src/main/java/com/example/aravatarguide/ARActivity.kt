package com.example.aravatarguide

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private var arSession: Session? = null
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var tvInstruction: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvWaypointCount: TextView
    private lateinit var etWaypointName: EditText
    private lateinit var btnClearLast: Button
    private lateinit var btnSavePath: Button

    private var installRequested = false
    private val waypoints = mutableListOf<Waypoint>()
    private var renderer: SimpleRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null

    private val waypointColor = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aractivity)

        surfaceView = findViewById(R.id.surfaceView)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvInfo = findViewById(R.id.tvInfo)
        tvWaypointCount = findViewById(R.id.tvWaypointCount)
        etWaypointName = findViewById(R.id.etWaypointName)
        btnClearLast = findViewById(R.id.btnClearLast)
        btnSavePath = findViewById(R.id.btnSavePath)

        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        surfaceView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onTap(event.x, event.y)
                true
            } else {
                false
            }
        }

        btnClearLast.setOnClickListener { clearLastWaypoint() }
        btnSavePath.setOnClickListener { savePath() }

        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    private fun onTap(x: Float, y: Float) {
        val session = arSession ?: return

        try {
            val frame = session.update()

            if (frame.camera.trackingState != TrackingState.TRACKING) {
                runOnUiThread {
                    Toast.makeText(this, "Wait for tracking", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val waypointName = etWaypointName.text.toString().trim()
            if (waypointName.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "Enter location name first", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val hits: List<HitResult> = frame.hitTest(x, y)

            for (hit in hits) {
                val trackable = hit.trackable

                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    val waypointId = "WP${waypoints.size + 1}"
                    val waypoint = Waypoint(waypointId, waypointName, hit.hitPose)
                    waypoints.add(waypoint)

                    runOnUiThread {
                        updateWaypointCount()
                        etWaypointName.text.clear()
                        hideKeyboard()
                        Toast.makeText(this, "✓ $waypointName placed!", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearLastWaypoint() {
        if (waypoints.isNotEmpty()) {
            val removed = waypoints.removeLast()
            Toast.makeText(this, "Removed: ${removed.name}", Toast.LENGTH_SHORT).show()
            updateWaypointCount()
        }
    }

    private fun savePath() {
        if (waypoints.isEmpty()) {
            Toast.makeText(this, "No waypoints to save", Toast.LENGTH_SHORT).show()
            return
        }

        val pathManager = PathManager(this)
        if (pathManager.savePath(waypoints)) {
            Toast.makeText(this, "✓ Saved ${waypoints.size} waypoints!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed to save", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateWaypointCount() {
        tvWaypointCount.text = "Waypoints placed: ${waypoints.size}"
        btnClearLast.isEnabled = waypoints.isNotEmpty()
        btnSavePath.isEnabled = waypoints.isNotEmpty()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etWaypointName.windowToken, 0)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            return
        }

        if (arSession == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {}
                    else -> return
                }

                arSession = Session(this)
                val config = Config(arSession)
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                arSession?.configure(config)

            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }

        try {
            arSession?.resume()
            surfaceView.onResume()
        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            arSession = null
        }
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
        arSession = null
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        backgroundRenderer = BackgroundRenderer()
        backgroundRenderer?.createOnGlThread(this)

        renderer = SimpleRenderer()
        renderer?.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        arSession?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = arSession ?: return

        try {
            session.setCameraTextureName(backgroundRenderer?.getTextureId() ?: 0)
            val frame: Frame = session.update()
            val camera = frame.camera

            // Draw camera background
            backgroundRenderer?.draw(frame)

            if (camera.trackingState != TrackingState.TRACKING) {
                return
            }

            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

            val planesDetected = session.getAllTrackables(Plane::class.java).any {
                it.trackingState == TrackingState.TRACKING
            }

            runOnUiThread {
                if (planesDetected) {
                    tvInstruction.text = "Surface detected! Tap to place waypoint"
                    tvInfo.text = "Enter name and tap on surface"
                } else {
                    tvInstruction.text = "Move phone slowly"
                    tvInfo.text = "Point camera at floor"
                }
            }

            renderer?.let { r ->
                waypoints.forEach { waypoint ->
                    r.draw(viewMatrix, projectionMatrix, waypoint.x, waypoint.y, waypoint.z, waypointColor)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}