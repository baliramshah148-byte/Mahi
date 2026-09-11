package com.example.model

import java.util.UUID

enum class MessageSender {
    USER,
    MAHI
}

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: MahiMood = MahiMood.SASSY,
    val isVoice: Boolean = false
)
