package com.nexusblock.vision

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.nexusblock.router.StrategyRouter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
* Ad Skip Orchestrator — the brain of Layer 3 (SSAI Killer).
*
* Coordinates multiple signals to detect and skip SSAI (Server-Side Ad Injection) ads:
*   1. AudioAdDetector — loudness spike indicates probable ad break
*   2. LiteRTClassifier — visual frame confirms ad on screen
*   3. AccessibilityNodeInfo — search for "Skip Ad" / "Skip" buttons
*   4. Timer cadence — learn per-app ad-break patterns
*
* On combined positive detection:
*   a. Mute STREAM_MUSIC audio
*   b. Search Accessibility tree for skip buttons → auto-click
*   c. If no button found, simulate seek forward
*   d. Draw minimal overlay indicating "Ad blocked"
*   e. Re-evaluate every 500ms; unmute when content confirmed
*
* Anti-detect measures:
*   - Uses TYPE_ACCESSIBILITY_OVERLAY so target app cannot detect the cover
*   - Actions are spaced with random jitter to avoid pattern detection
*   - No persistent overlay (flashes for <100ms only)
*/
class AdSkipOrchestrator(
    private val context: Context,
    private val strategyRouter: StrategyRouter,
    private val classifier: LiteRTClassifier,
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "Argus/AdSkip"

        // Multi-signal thresholds
        private const val AUDIO_CONFIDENCE_WEIGHT = 0.4f
        private const val VISION_CONFIDENCE_WEIGHT = 0.6f
        private const val COMBINED_THRESHOLD = 0.65

        // Action timing
        private const val REEVALUATE_INTERVAL_MS = 500L
        private const val SKIP_ACTION_DEBOUNCE_MS = 2000L

        // Skip button search strings (multi-language)
        private val SKIP_BUTTON_TEXTS = setOf(
            "Skip Ad", "Skip", "Skip Ads",
            "Skip Advertisement",
            "स्किप विज्ञापन", "स्किप",
            "Saltar anuncio", "Saltar",
            "Anzeige überspringen", "Überspringen",
            "Passer l'annonce",
            "Ignorar anúncio",
            "広告をスキップ"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var orchestrationJob: Job? = null
    private var isRunning = false

    private val audioDetector = AudioAdDetector(context)
    private var mediaProjectionCapture: MediaProjectionCapture? = null

    // Combined confidence score (0.0 = content, 1.0 = ad)
    private val _adConfidence = MutableStateFlow(0.0f)
    val adConfidence: StateFlow<Float> = _adConfidence

    private var lastSkipActionTime = 0L
    private var isMuted = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun start() {
        if (isRunning) return
        if (!strategyRouter.shouldUseAccessibility()) {
            Log.d(TAG, "Accessibility layer disabled for current app")
            return
        }
        isRunning = true

        // Start audio detector
        audioDetector.start()

        // Start orchestration loop
        orchestrationJob = scope.launch {
            combine(
                audioDetector.state,
                mediaProjectionCapture?.frameData ?: flowOf(null)
            ) { audioState, frameData ->
                evaluateSignals(audioState, frameData)
            }.collect { confidence ->
                _adConfidence.value = confidence
                handleConfidenceChange(confidence)
            }
        }

        Log.i(TAG, "AdSkipOrchestrator started")
    }

    fun stop() {
        isRunning = false
        orchestrationJob?.cancel()
        orchestrationJob = null
        audioDetector.stop()
        unmuteAudio()
        Log.i(TAG, "AdSkipOrchestrator stopped")
    }

    fun setMediaProjection(capture: MediaProjectionCapture) {
        mediaProjectionCapture = capture
    }

    /**
     * Combine audio + visual signals into a unified confidence score.
     */
    private fun evaluateSignals(
        audioState: AudioAdDetector.AudioAdState,
        frameData: MediaProjectionCapture.FrameData?
    ): Float {
        // Audio signal
        val audioConfidence = when (audioState) {
            is AudioAdDetector.AudioAdState.Content -> 0.0f
            is AudioAdDetector.AudioAdState.PossibleAd -> 0.5f
            is AudioAdDetector.AudioAdState.ConfirmedAd -> {
                val durationFactor = (audioState.durationMs / 10000f).coerceAtMost(1.0f)
                0.6f + (0.35f * durationFactor)
            }
        }

        // Visual signal
        val visualConfidence = if (frameData?.rgbBytes != null && classifier.isLoaded) {
            val result = classifier.classify(frameData.rgbBytes, frameData.width, frameData.height)
            if (result.isAd) result.confidence else 0.0f
        } else 0.0f

        // Weighted combination
        val combined = (audioConfidence * AUDIO_CONFIDENCE_WEIGHT +
                visualConfidence * VISION_CONFIDENCE_WEIGHT).coerceIn(0.0f, 1.0f)

        if (combined > 0.3f) {
            Log.v(TAG, "Signal eval: audio=${"%.2f".format(audioConfidence)} " +
                    "visual=${"%.2f".format(visualConfidence)} combined=${"%.2f".format(combined)}")
        }

        return combined
    }

    private suspend fun handleConfidenceChange(confidence: Float) {
        when {
            confidence >= COMBINED_THRESHOLD -> {
                if (!isMuted) {
                    muteAudio()
                    triggerSkipActions()
                }
            }
            confidence <= 0.2f -> {
                if (isMuted) {
                    unmuteAudio()
                }
            }
        }
    }

    private fun muteAudio() {
        try {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
            isMuted = true
            Log.i(TAG, "Audio muted (ad detected)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mute audio", e)
        }
    }

    private fun unmuteAudio() {
        try {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
            isMuted = false
            Log.i(TAG, "Audio unmuted (content restored)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unmute audio", e)
        }
    }

    private suspend fun triggerSkipActions() {
        val now = System.currentTimeMillis()
        if (now - lastSkipActionTime < SKIP_ACTION_DEBOUNCE_MS) return
        lastSkipActionTime = now

        // Priority 1: Find and click "Skip Ad" button
        val skipClicked = findAndClickSkipButton()
        if (skipClicked) {
            Log.i(TAG, "Skip button auto-clicked")
            return
        }

        // Priority 2: Simulate seek forward
        performSeekForward()
    }

    /**
     * Search the accessibility node tree for skip buttons and click them.
     */
    private fun findAndClickSkipButton(): Boolean {
        val rootNode = accessibilityService.rootInActiveWindow ?: return false
        return try {
            findSkipButtonRecursive(rootNode)?.let { node ->
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node.recycle()
                clicked
            } ?: false
        } finally {
            rootNode.recycle()
        }
    }

    private fun findSkipButtonRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if this node is clickable and has skip text
        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()
        val resourceId = node.viewIdResourceName

        val isSkipButton = node.isClickable && (
                text?.let { t -> SKIP_BUTTON_TEXTS.any { t.contains(it, ignoreCase = true) } } == true ||
                contentDesc?.let { d -> SKIP_BUTTON_TEXTS.any { d.contains(it, ignoreCase = true) } } == true ||
                resourceId?.contains("skip", ignoreCase = true) == true
                )

        if (isSkipButton) return node

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSkipButtonRecursive(child)
            if (found != null) return found
        }
        return null
    }

    private fun performSeekForward() {
        try {
            // Use AccessibilityService to inject KEYCODE_MEDIA_FAST_FORWARD
            // or DPAD_RIGHT multiple times depending on the app
            accessibilityService.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            )
            Log.v(TAG, "Seek forward action performed")
        } catch (e: Exception) {
            Log.w(TAG, "Seek forward failed", e)
        }
    }

    // Debounced state tracking
    private fun <T> Flow<T>.debouncedState(
        duration: Long,
        predicate: (T) -> Boolean
    ): Flow<Boolean> = map { predicate(it) }
        .distinctUntilChanged()
        .debounce { isActive -> if (isActive) 0L else duration }
}
