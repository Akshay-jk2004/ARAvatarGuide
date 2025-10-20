package com.example.aravatarguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
    private var renderer: SimpleRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null
    private val navigationHelper = NavigationHelper()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var isTtsReady = false

    private val waypointColor = floatArrayOf(0.0f, 0.5f, 1.0f, 1.0f) // Blue
    private val destinationColor = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f) // Red
    private var arrowModel: ModelLoader? = null
    private val arrowColor = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f) // Gold
    private var showArrow = false

    companion object {
        private const val PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor)

        // Initialize views
        surfaceView = findViewById(R.id.surfaceView)
        tvStatus = findViewById(R.id.tvStatus)
        tvDestination = findViewById(R.id.tvDestination)
        tvDirection = findViewById(R.id.tvDirection)
        tvAvatarStatus = findViewById(R.id.tvAvatarStatus)
        tvSpeechInput = findViewById(R.id.tvSpeechInput)
        btnMicrophone = findViewById(R.id.btnMicrophone)
        tvAvailableLocations = findViewById(R.id.tvAvailableLocations)

        // Setup OpenGL
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // Setup microphone button
        btnMicrophone.setOnClickListener { toggleSpeechRecognition() }

        // Initialize TTS
        textToSpeech = TextToSpeech(this, this)

        // Load saved waypoints
        loadWaypoints()

        // Check permissions
        checkPermissions()
    }

    private fun loadWaypoints() {
        val pathManager = PathManager(this)
        waypoints.clear()
        waypoints.addAll(pathManager.loadPath())

        if (waypoints.isEmpty()) {
            tvAvailableLocations.text = "⚠ No locations mapped. Use Host mode first."
            tvSpeechInput.text = "No destinations available"
            btnMicrophone.isEnabled = false
        } else {
            val locationList = waypoints.joinToString(", ") { it.name }
            tvAvailableLocations.text = "Available: $locationList"
            tvSpeechInput.text = "Tap 🎤 and say: 'Take me to ${waypoints.first().name}'"
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (!hasCameraPermission()) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (!hasAudioPermission()) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_CODE
            )
        } else {
            setupSpeechRecognition()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                setupSpeechRecognition()
            } else {
                Toast.makeText(this, "Permissions required for full functionality", Toast.LENGTH_LONG).show()
            }
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
                runOnUiThread {
                    btnMicrophone.text = "🎤"
                }
            }

            override fun onError(error: Int) {
                isListening = false
                runOnUiThread {
                    tvSpeechInput.text = "Tap 🎤 to try again"
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

        if (speechRecognizer == null) {
            setupSpeechRecognition()
        }

        if (isListening) {
            speechRecognizer?.stopListening()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Take me to [location name]")
            }
            speechRecognizer?.startListening(intent)
        }
    }

    private fun processVoiceCommand(command: String) {
        runOnUiThread {
            tvSpeechInput.text = "You said: \"$command\""
        }

        // Find matching waypoint
        val matchedWaypoint = waypoints.find { waypoint ->
            command.contains(waypoint.name, ignoreCase = true)
        }

        if (matchedWaypoint != null) {
            currentDestination = matchedWaypoint
            showArrow = true
            speak("Taking you to ${matchedWaypoint.name}. Follow the golden arrow.")

            runOnUiThread {
                tvDestination.text = "→ ${matchedWaypoint.name}"
                tvDestination.visibility = TextView.VISIBLE
                tvDirection.visibility = TextView.VISIBLE
                tvAvatarStatus.text = "🧭 Follow the arrow"
                tvAvatarStatus.visibility = TextView.VISIBLE
            }
        } else {
            val availableLocations = waypoints.joinToString(", ") { it.name }
            speak("I didn't find that location. Available places are: $availableLocations")

            runOnUiThread {
                tvSpeechInput.text = "Try: ${waypoints.joinToString(" or ") { it.name }}"
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

            if (waypoints.isNotEmpty()) {
                speak("AR Guide ready. Where would you like to go?")
            }
        }
    }

    // AR Session Management
    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            tvStatus.text = "Camera permission required"
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
                    else -> {
                        Toast.makeText(this, "ARCore installation failed", Toast.LENGTH_LONG).show()
                        return
                    }
                }

                arSession = Session(this)

                val config = Config(arSession)
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                arSession?.configure(config)

            } catch (e: Exception) {
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                return
            }
        }

        try {
            arSession?.resume()
            surfaceView.onResume()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
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

    // OpenGL Renderer Methods
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Initialize background renderer (camera feed)
        backgroundRenderer = BackgroundRenderer()
        backgroundRenderer?.createOnGlThread(this)

        // Initialize waypoint marker renderer
        renderer = SimpleRenderer()
        renderer?.createOnGlThread()

        // Initialize arrow model
        arrowModel = ModelLoader(this)
        arrowModel?.loadModel("arrow.glb")
        arrowModel?.createOnGlThread()
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
                runOnUiThread {
                    tvStatus.text = "Initializing AR tracking..."
                }
                return
            }

            runOnUiThread {
                tvStatus.text = "AR Guide Active"
            }

            // Get camera matrices
            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

            // Get camera position and rotation
            val cameraPos = camera.pose.translation
            val cameraRotation = camera.pose.rotationQuaternion

            // Draw all waypoints
            renderer?.let { r ->
                waypoints.forEach { waypoint ->
                    val color = if (waypoint == currentDestination) destinationColor else waypointColor
                    r.draw(viewMatrix, projectionMatrix, waypoint.x, waypoint.y, waypoint.z, color)
                }
            }

            // Draw navigation arrow to destination
            currentDestination?.let { dest ->
                val navInfo = navigationHelper.calculateNavigation(cameraPos, cameraRotation, dest)

                // Update UI with navigation info
                runOnUiThread {
                    val directionText = navigationHelper.getDirectionText(navInfo.distance)
                    tvDestination.text = "→ ${dest.name}"
                    tvDirection.text = directionText
                    tvDirection.visibility = TextView.VISIBLE

                    // Check if arrived
                    if (navInfo.distance < 1.0f) {
                        showArrow = false
                        speak("You have arrived at ${dest.name}")
                        tvAvatarStatus.text = "✓ Arrived!"
                        tvDirection.visibility = TextView.GONE
                        currentDestination = null
                    }
                }

                // Draw arrow if navigation is active
                if (showArrow && arrowModel != null) {
                    val arrowMvp = navigationHelper.createArrowMatrix(
                        navInfo.arrowPosition,
                        navInfo.arrowRotation,
                        viewMatrix,
                        projectionMatrix
                    )
                    arrowModel?.draw(arrowMvp, arrowColor)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}