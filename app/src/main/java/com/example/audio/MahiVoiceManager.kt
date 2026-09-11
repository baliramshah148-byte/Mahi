package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class MahiVoiceManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onRmsLevelChanged: (Float) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "MahiVoiceManager"

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    var speechPitch: Float = 1.18f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var speechRate: Float = 1.05f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var isMuted: Boolean = false

    private var currentUtteranceCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer: onReadyForSpeech")
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize roughly between 0f and 1f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsLevelChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: onEndOfSpeech")
                        onListeningStateChanged(false)
                        onRmsLevelChanged(0f)
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        onListeningStateChanged(false)
                        onRmsLevelChanged(0f)
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningStateChanged(false)
                        onRmsLevelChanged(0f)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()
                        if (!spokenText.isNullOrBlank()) {
                            activeVoiceResultCallback?.invoke(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            activePartialResultCallback?.invoke(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private var activeVoiceResultCallback: ((String) -> Unit)? = null
    private var activePartialResultCallback: ((String) -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            // Pick a smooth, young, female sounding voice if available in voice list
            try {
                val voices = tts?.voices
                val femaleVoice = voices?.firstOrNull {
                    it.locale.language == Locale.US.language &&
                            (it.name.contains("female", ignoreCase = true) ||
                             it.name.contains("en-us-x-sfg", ignoreCase = true) ||
                             it.name.contains("en-us-x-iol", ignoreCase = true))
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not filter voice: ${e.message}")
            }

            tts?.setPitch(speechPitch)
            tts?.setSpeechRate(speechRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                    currentUtteranceCallback?.invoke()
                    currentUtteranceCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                    currentUtteranceCallback = null
                }
            })

            isTtsInitialized = true
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (isMuted) {
            onFinished?.invoke()
            return
        }

        // Clean speech text for clean TTS (remove markdown emojis or formatting)
        val cleanForVoice = text
            .replace(Regex("""[*_~`#>]"""), "")
            .replace(Regex("""\[.*?\]"""), "")
            .trim()

        if (cleanForVoice.isBlank()) {
            onFinished?.invoke()
            return
        }

        currentUtteranceCallback = onFinished
        val utteranceId = UUID.randomUUID().toString()

        if (isTtsInitialized) {
            tts?.speak(cleanForVoice, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            onFinished?.invoke()
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        onSpeakingStateChanged(false)
        currentUtteranceCallback = null
    }

    fun startListening(
        onResult: (String) -> Unit,
        onPartial: ((String) -> Unit)? = null
    ) {
        stopSpeaking()
        activeVoiceResultCallback = onResult
        activePartialResultCallback = onPartial

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Mahi...")
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer: ${e.message}")
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop speech recognizer: ${e.message}")
        }
        onListeningStateChanged(false)
        onRmsLevelChanged(0f)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
