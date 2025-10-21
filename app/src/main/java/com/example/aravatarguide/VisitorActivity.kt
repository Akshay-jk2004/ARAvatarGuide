package com.example.aravatarguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt

class VisitorActivity : AppCompatActivity(), GLSurfaceView.Renderer, TextToSpeech.OnInitListener {

    private var arSession: Session? = null
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var tvStatus: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvDirection: TextView
    private lateinit var tvAvatarStatus: TextView
    private lateinit var tvSpeechInput: TextView
    private lateinit var btnMicrophone: Button
    private lateinit var tvAvailableLocations: TextView

    private var installRequested = false
    private val waypoints = mutableListOf<Waypoint>()
    private var currentDestination: Waypoint? = null
    private var pathToFollow = listOf<Waypoint>()
    private var currentWaypointIndex = 0

    private var renderer: SimpleRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null
    private val navigationHelper = NavigationHelper()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var isTtsReady = false
    private var hasAskedInitialQuestion = false
    private var isNavigating = false
    private var hasAnnouncedFollowPath = false

    private var arrowModel: ModelLoader? = null
    private var avatarModel: ModelLoader? = null

    private val destinationColor = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    private val pathColor = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)

    companion object {
        private const val PERMISSION_CODE = 100
        private const val WAYPOINT_REACHED_DISTANCE = 0.8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor)

        surfaceView = findViewById(R.id.surfaceView)
        tvStatus = findViewById(R.id.tvStatus)
        tvDestination = findViewById(R.id.tvDestination)
        tvDirection = findViewById(R.id.tvDirection)
        tvAvatarStatus = findViewById(R.id.tvAvatarStatus)
        tvSpeechInput = findViewById(R.id.tvSpeechInput)
        btnMicrophone = findViewById(R.id.btnMicrophone)
        tvAvailableLocations = findViewById(R.id.tvAvailableLocations)

        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        btnMicrophone.setOnClickListener { toggleSpeechRecognition() }

        textToSpeech = TextToSpeech(this, this)
        loadWaypoints()
        checkPermissions()
    }

    private fun loadWaypoints() {
        val pathManager = PathManager(this)
        waypoints.clear()
        waypoints.addAll(pathManager.loadPath())

        if (waypoints.isEmpty()) {
            tvAvailableLocations.text = "⚠ No paths mapped. Use Host mode first."
            tvSpeechInput.text = "No destinations available"
            btnMicrophone.isEnabled = false
        } else {
            val destinations = waypoints.filter { it.isEndPoint }.map { it.name }.distinct()
            tvAvailableLocations.text = "Available: ${destinations.joinToString(", ")}"
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (!hasCameraPermission()) permissionsNeeded.add(Manifest.permission.CAMERA)
        if (!hasAudioPermission()) permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_CODE)
        } else {
            setupSpeechRecognition()
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun hasAudioPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupSpeechRecognition()
        }
    }

    private fun setupSpeechRecognition() {
        if (!hasAudioPermission()) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                runOnUiThread {
                    tvSpeechInput.text = "🎤 Listening..."
                    btnMicrophone.text = "⏺"
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                runOnUiThread { btnMicrophone.text = "🎤" }
            }
            override fun onError(error: Int) {
                isListening = false
                runOnUiThread {
                    tvSpeechInput.text = "Tap 🎤 to speak"
                    btnMicrophone.text = "🎤"
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processVoiceCommand(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun toggleSpeechRecognition() {
        if (!hasAudioPermission()) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }

        if (waypoints.isEmpty()) {
            Toast.makeText(this, "No locations available. Use Host mode first.", Toast.LENGTH_LONG).show()
            return
        }

        if (speechRecognizer == null) setupSpeechRecognition()

        if (isListening) {
            speechRecognizer?.stopListening()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Where would you like to go?")
            }
            speechRecognizer?.startListening(intent)
        }
    }

    private fun processVoiceCommand(command: String) {
        runOnUiThread {
            tvSpeechInput.text = "You said: \"$command\""
        }

        val matchedDestination = waypoints.filter { it.isEndPoint }.find { waypoint ->
            command.contains(waypoint.name, ignoreCase = true)
        }

        if (matchedDestination != null) {
            currentDestination = matchedDestination
            currentWaypointIndex = 0
            isNavigating = true
            hasAnnouncedFollowPath = false

            runOnUiThread {
                tvDestination.text = "→ ${matchedDestination.name}"
                tvDestination.visibility = TextView.VISIBLE
                tvDirection.visibility = TextView.VISIBLE
                tvAvatarStatus.text = "🧭 Navigating..."
                tvAvatarStatus.visibility = TextView.VISIBLE
            }
        } else {
            val destinations = waypoints.filter { it.isEndPoint }.map { it.name }.distinct()
            speak("I couldn't find that location. Available destinations are: ${destinations.joinToString(", ")}")
            runOnUiThread {
                tvSpeechInput.text = "Try saying: ${destinations.joinToString(" or ")}"
            }
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasCameraPermission()) return

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
        speechRecognizer?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
        arSession = null
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        backgroundRenderer = BackgroundRenderer()
        backgroundRenderer?.createOnGlThread(this)

        renderer = SimpleRenderer()
        renderer?.createOnGlThread()

        arrowModel = ModelLoader(this)
        arrowModel?.loadModel("arrow.glb")
        arrowModel?.createOnGlThread()

        avatarModel = ModelLoader(this)
        avatarModel?.loadModel("avatar.glb")
        avatarModel?.createOnGlThread()
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

            backgroundRenderer?.draw(frame)

            if (camera.trackingState != TrackingState.TRACKING) {
                runOnUiThread {
                    tvStatus.text = "Initializing AR tracking..."
                }
                return
            }

            runOnUiThread {
                tvStatus.text = "AR Guide Active"
            }

            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

            val cameraPos = camera.pose.translation
            val cameraPose = camera.pose

            // Ask initial question once
            if (!hasAskedInitialQuestion && isTtsReady && waypoints.isNotEmpty()) {
                hasAskedInitialQuestion = true
                val destinations = waypoints.filter { it.isEndPoint }.map { it.name }.distinct()
                speak("Hello! Where would you like to go? Available destinations are: ${destinations.joinToString(", ")}")
                runOnUiThread {
                    tvAvatarStatus.text = "👋 Avatar Guide"
                    tvAvatarStatus.visibility = TextView.VISIBLE
                }
            }

            // Calculate avatar position (2 meters in front of camera)
            val forward = cameraPose.zAxis
            val avatarX = cameraPos[0] - forward[0] * 2.0f
            val avatarY = cameraPos[1] - 1.3f
            val avatarZ = cameraPos[2] - forward[2] * 2.0f

            // Draw 3D avatar
            if (avatarModel != null) {
                val avatarModelMatrix = FloatArray(16)
                Matrix.setIdentityM(avatarModelMatrix, 0)
                Matrix.translateM(avatarModelMatrix, 0, avatarX, avatarY, avatarZ)
                Matrix.scaleM(avatarModelMatrix, 0, 0.5f, 0.5f, 0.5f)

                val avatarMvp = FloatArray(16)
                val tempMatrix = FloatArray(16)
                Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, avatarModelMatrix, 0)
                Matrix.multiplyMM(avatarMvp, 0, projectionMatrix, 0, tempMatrix, 0)

                val avatarColor = floatArrayOf(0.2f, 0.6f, 1.0f, 1.0f)
                avatarModel?.draw(avatarMvp, avatarColor)
            }

            // Navigation logic
            if (isNavigating && currentDestination != null) {
                val dest = currentDestination!!

                // Build path on first frame
                if (pathToFollow.isEmpty()) {
                    val userNearestWaypoint = waypoints.minByOrNull { waypoint ->
                        val dx = waypoint.x - cameraPos[0]
                        val dz = waypoint.z - cameraPos[2]
                        sqrt(dx * dx + dz * dz)
                    }
                    pathToFollow = buildPathToDestination(userNearestWaypoint, dest)

                    if (pathToFollow.isNotEmpty() && !hasAnnouncedFollowPath) {
                        speak("Follow the path")
                        hasAnnouncedFollowPath = true
                    }
                }

                // Draw path as GREEN CIRCLES
                renderer?.let { r ->
                    pathToFollow.forEach { waypoint ->
                        val color = if (waypoint == dest) destinationColor else pathColor
                        r.draw(viewMatrix, projectionMatrix, waypoint.x, waypoint.y, waypoint.z, color)
                    }
                }

                if (pathToFollow.isNotEmpty() && currentWaypointIndex < pathToFollow.size) {
                    val nextWaypoint = pathToFollow[currentWaypointIndex]

                    val dx = nextWaypoint.x - cameraPos[0]
                    val dz = nextWaypoint.z - cameraPos[2]
                    val distanceToNext = sqrt(dx * dx + dz * dz)

                    if (distanceToNext < WAYPOINT_REACHED_DISTANCE) {
                        currentWaypointIndex++

                        if (currentWaypointIndex >= pathToFollow.size) {
                            isNavigating = false
                            speak("Destination arrived")

                            runOnUiThread {
                                tvAvatarStatus.text = "✅ Destination Arrived!"
                                tvDirection.text = "You have reached ${dest.name}"
                            }

                            currentDestination = null
                            pathToFollow = emptyList()
                            currentWaypointIndex = 0
                            hasAnnouncedFollowPath = false
                            return
                        }
                    }

                    val angleToTarget = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

                    runOnUiThread {
                        tvDirection.text = String.format("%.1f", distanceToNext) + "m to next point"
                    }

                    // Draw arrow
                    if (arrowModel != null && distanceToNext > 0.5f) {
                        val arrowDistance = 1.5f
                        val normalizedDx = dx / distanceToNext
                        val normalizedDz = dz / distanceToNext

                        val arrowX = cameraPos[0] + normalizedDx * arrowDistance
                        val arrowY = cameraPos[1] - 1.4f
                        val arrowZ = cameraPos[2] + normalizedDz * arrowDistance

                        val arrowPosition = floatArrayOf(arrowX, arrowY, arrowZ)
                        val arrowMvp = navigationHelper.createArrowMatrix(arrowPosition, angleToTarget, viewMatrix, projectionMatrix)
                        val arrowColor = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f)
                        arrowModel?.draw(arrowMvp, arrowColor)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildPathToDestination(start: Waypoint?, destination: Waypoint?): List<Waypoint> {
        if (start == null || destination == null) return emptyList()

        val startIndex = start.pathIndex
        val destIndex = destination.pathIndex

        return if (startIndex <= destIndex) {
            waypoints.filter { it.pathIndex in startIndex..destIndex }.sortedBy { it.pathIndex }
        } else {
            waypoints.filter { it.pathIndex in destIndex..startIndex }.sortedByDescending { it.pathIndex }
        }
    }
}