package com.example.streetpay.gesture

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.math.hypot

class GestureRecognizerHelper(
    val context: Context,
    val gestureListener: GestureRecognizerListener
) {
    private var gestureRecognizer: GestureRecognizer? = null
    
    // Debouncing and State tracking
    private var lastActionTime = 0L
    private var lastDetectedGesture: String? = null
    private val COOLDOWN_MS = 800L // 0.8 seconds between actions

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task")

        val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinHandDetectionConfidence(0.7f)
            .setMinHandPresenceConfidence(0.7f)
            .setMinTrackingConfidence(0.7f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)

        try {
            gestureRecognizer = GestureRecognizer.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            gestureListener.onError("Gesture recognizer failed to initialize. See error logs for details")
            Log.e(TAG, "MediaPipe Error: ${e.message}")
        }
    }

    fun recognizeLiveStream(bitmap: Bitmap) {
        val frameTime = SystemClock.uptimeMillis()
        val mpImage = BitmapImageBuilder(bitmap).build()
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: GestureRecognizerResult,
        input: MPImage
    ) {
        val gestures = result.gestures()
        val landmarks = result.landmarks()
        val handednesses = result.handedness()

        if (gestures.isNotEmpty() && landmarks.isNotEmpty() && handednesses.isNotEmpty()) {
            val handLandmarks = landmarks[0]
            val gesture = gestures[0][0]
            val handedness = handednesses[0][0].categoryName() // "Left" or "Right"
            
            val imgWidth = input.width
            val imgHeight = input.height

            // Calculate pixel coordinates for pinch detection
            fun getPx(idx: Int): Pair<Float, Float> {
                val lm = handLandmarks[idx]
                return Pair(lm.x() * imgWidth, lm.y() * imgHeight)
            }

            val thumbTip = getPx(4)
            val indexTip = getPx(8)

            val pinchThreshold = imgWidth * 0.05f
            val isIndexPinch = hypot(thumbTip.first - indexTip.first, thumbTip.second - indexTip.second) < pinchThreshold

            // Mapping:
            // Left Hand Index Pinch -> NEXT
            // Right Hand Index Pinch -> PREV
            // Open Palm -> SEEN
            // Victory (Peace) -> UNSEEN
            
            val detectedAction = when {
                isIndexPinch && handedness == "Left" -> "NEXT"
                isIndexPinch && handedness == "Right" -> "PREV"
                gesture.categoryName() == "Open_Palm" -> "SEEN"
                gesture.categoryName() == "Victory" -> "UNSEEN"
                else -> null
            }

            if (detectedAction != null) {
                val currentTime = SystemClock.uptimeMillis()
                
                // Only trigger if we are past the cooldown AND the gesture has changed
                // OR it's been a long time since the last action
                val isNewGesture = detectedAction != lastDetectedGesture
                val isCooldownOver = (currentTime - lastActionTime) > COOLDOWN_MS

                if (isNewGesture || isCooldownOver) {
                    gestureListener.onResults(detectedAction)
                    lastActionTime = currentTime
                }
            }
            
            // Track the current gesture for the next frame
            lastDetectedGesture = detectedAction
        } else {
            // No hand detected, reset last gesture
            lastDetectedGesture = null
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        gestureListener.onError(error.message ?: "An unknown error has occurred")
    }

    interface GestureRecognizerListener {
        fun onError(error: String)
        fun onResults(action: String)
    }

    companion object {
        private const val TAG = "GestureRecognizerHelper"
    }
}
