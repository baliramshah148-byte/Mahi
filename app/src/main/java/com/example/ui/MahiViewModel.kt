package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.MahiVoiceManager
import com.example.model.ChatMessage
import com.example.model.MahiMood
import com.example.model.MessageSender
import com.example.model.VoiceState
import com.example.network.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MahiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val voiceState: VoiceState = VoiceState.IDLE,
    val currentMood: MahiMood = MahiMood.SASSY,
    val rmsLevel: Float = 0f,
    val liveTranscription: String = "",
    val activeSpokenMahiText: String = "",
    val isHandsFreeMode: Boolean = false,
    val isVoiceAutoSpeak: Boolean = true,
    val voicePitch: Float = 1.18f,
    val voiceSpeed: Float = 1.05f,
    val personalityVibe: String = "Balanced",
    val isApiKeyConfigured: Boolean = false,
    val activeViewMode: ViewMode = ViewMode.STAGE // STAGE or CHAT
)

enum class ViewMode {
    STAGE,
    CHAT
}

class MahiViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MahiUiState())
    val uiState: StateFlow<MahiUiState> = _uiState.asStateFlow()

    private var voiceManager: MahiVoiceManager? = null

    init {
        checkApiKeyStatus()
        initVoiceManager()
        addWelcomeMessage()
    }

    private fun checkApiKeyStatus() {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        val isConfigured = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "null" &&
                !apiKey.startsWith("YOUR_")

        _uiState.update { it.copy(isApiKeyConfigured = isConfigured) }
    }

    private fun initVoiceManager() {
        voiceManager = MahiVoiceManager(
            context = getApplication(),
            onSpeakingStateChanged = { isSpeaking ->
                _uiState.update { current ->
                    if (isSpeaking) {
                        current.copy(
                            voiceState = VoiceState.SPEAKING,
                            currentMood = MahiMood.SPEAKING
                        )
                    } else {
                        val nextVoiceState = if (current.isHandsFreeMode && current.voiceState == VoiceState.SPEAKING) {
                            VoiceState.LISTENING
                        } else {
                            VoiceState.IDLE
                        }
                        current.copy(
                            voiceState = nextVoiceState,
                            currentMood = MahiMood.SASSY,
                            activeSpokenMahiText = ""
                        )
                    }
                }

                // If hands-free mode is enabled and speaking just finished, automatically start listening!
                if (!isSpeaking && _uiState.value.isHandsFreeMode) {
                    startVoiceListeningInternal()
                }
            },
            onListeningStateChanged = { isListening ->
                _uiState.update { current ->
                    current.copy(
                        voiceState = if (isListening) VoiceState.LISTENING else VoiceState.IDLE,
                        currentMood = if (isListening) MahiMood.LISTENING else current.currentMood
                    )
                }
            },
            onRmsLevelChanged = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            }
        )
    }

    private fun addWelcomeMessage() {
        val welcomeText = "Hey there, gorgeous! I'm Mahi. Your AI bestie, confident co-pilot, and full-time hype girl. Tap the mic or drop a message—don't be shy! 😉"
        val welcomeMsg = ChatMessage(
            sender = MessageSender.MAHI,
            text = welcomeText,
            mood = MahiMood.FLIRTY
        )
        _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
    }

    fun toggleVoiceListening() {
        if (_uiState.value.voiceState == VoiceState.LISTENING) {
            voiceManager?.stopListening()
        } else {
            startVoiceListeningInternal()
        }
    }

    private fun startVoiceListeningInternal() {
        voiceManager?.stopSpeaking()
        _uiState.update {
            it.copy(
                voiceState = VoiceState.LISTENING,
                currentMood = MahiMood.LISTENING,
                liveTranscription = ""
            )
        }

        voiceManager?.startListening(
            onResult = { spokenText ->
                _uiState.update { it.copy(liveTranscription = spokenText) }
                sendMessage(spokenText, isVoice = true)
            },
            onPartial = { partial ->
                _uiState.update { it.copy(liveTranscription = partial) }
            }
        )
    }

    fun stopVoiceListening() {
        voiceManager?.stopListening()
    }

    fun sendMessage(text: String, isVoice: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        voiceManager?.stopSpeaking()

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed,
            isVoice = isVoice
        )

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                voiceState = VoiceState.PROCESSING,
                currentMood = MahiMood.THINKING,
                liveTranscription = ""
            )
        }

        viewModelScope.launch {
            val response = GeminiApiClient.sendMessage(
                history = _uiState.value.messages,
                userMessage = trimmed,
                personalityVibe = _uiState.value.personalityVibe
            )

            val mahiMessage = ChatMessage(
                sender = MessageSender.MAHI,
                text = response.cleanText,
                mood = response.mood,
                isVoice = false
            )

            _uiState.update { current ->
                current.copy(
                    messages = current.messages + mahiMessage,
                    currentMood = response.mood,
                    activeSpokenMahiText = response.cleanText
                )
            }

            if (_uiState.value.isVoiceAutoSpeak) {
                voiceManager?.speak(response.cleanText)
            } else {
                _uiState.update { it.copy(voiceState = VoiceState.IDLE) }
            }
        }
    }

    fun speakText(text: String) {
        voiceManager?.stopSpeaking()
        _uiState.update {
            it.copy(
                activeSpokenMahiText = text,
                currentMood = MahiMood.SPEAKING
            )
        }
        voiceManager?.speak(text)
    }

    fun stopSpeaking() {
        voiceManager?.stopSpeaking()
    }

    fun onAvatarPoke() {
        val pokeReplies = listOf(
            "Hey! Did you just poke me? Flattery will get you everywhere, babe~ 😉" to MahiMood.FLIRTY,
            "Careful now, you might catch feelings if you keep touching my holographic aura! 💅" to MahiMood.SASSY,
            "I'm all ears! Spill the tea, what's on your mind? ✨" to MahiMood.CONFIDENT,
            "Ticklish! Okay, you're cute. Now tell me what you actually need~ 😏" to MahiMood.TEASING,
            "Are you testing my reflexes? Don't worry, my wit is faster than light! 🔥" to MahiMood.HYPE
        )
        val (reply, mood) = pokeReplies.random()

        val pokeMessage = ChatMessage(
            sender = MessageSender.MAHI,
            text = reply,
            mood = mood
        )

        _uiState.update { current ->
            current.copy(
                messages = current.messages + pokeMessage,
                currentMood = mood,
                activeSpokenMahiText = reply
            )
        }

        if (_uiState.value.isVoiceAutoSpeak) {
            voiceManager?.speak(reply)
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(activeViewMode = mode) }
    }

    fun setHandsFreeMode(enabled: Boolean) {
        _uiState.update { it.copy(isHandsFreeMode = enabled) }
    }

    fun setVoiceAutoSpeak(enabled: Boolean) {
        _uiState.update { it.copy(isVoiceAutoSpeak = enabled) }
        voiceManager?.isMuted = !enabled
        if (!enabled) {
            voiceManager?.stopSpeaking()
        }
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.update { it.copy(voicePitch = pitch) }
        voiceManager?.speechPitch = pitch
    }

    fun setVoiceSpeed(speed: Float) {
        _uiState.update { it.copy(voiceSpeed = speed) }
        voiceManager?.speechRate = speed
    }

    fun setPersonalityVibe(vibe: String) {
        _uiState.update { it.copy(personalityVibe = vibe) }
    }

    fun clearChat() {
        voiceManager?.stopSpeaking()
        _uiState.update { it.copy(messages = emptyList(), liveTranscription = "", activeSpokenMahiText = "") }
        addWelcomeMessage()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.destroy()
    }
}
