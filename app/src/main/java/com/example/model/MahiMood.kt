package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonRose
import com.example.ui.theme.SassyPink
import com.example.ui.theme.SoftRose
import com.example.ui.theme.TeasingCyan

enum class MahiMood(
    val displayName: String,
    val emoji: String,
    val glowColor: Color,
    val secondaryColor: Color,
    val description: String
) {
    SASSY(
        displayName = "Sassy",
        emoji = "💅",
        glowColor = NeonRose,
        secondaryColor = ElectricViolet,
        description = "Bold, witty, and unapologetic"
    ),
    FLIRTY(
        displayName = "Flirty",
        emoji = "😉",
        glowColor = SassyPink,
        secondaryColor = NeonRose,
        description = "Playful with a cute wink"
    ),
    TEASING(
        displayName = "Teasing",
        emoji = "😏",
        glowColor = ElectricViolet,
        secondaryColor = SassyPink,
        description = "Playfully judging your choices"
    ),
    HYPE(
        displayName = "Hype",
        emoji = "🔥",
        glowColor = ElectricAmber,
        secondaryColor = NeonRose,
        description = "Your #1 hype girl in full power"
    ),
    CONFIDENT(
        displayName = "Confident",
        emoji = "✨",
        glowColor = NeonRose,
        secondaryColor = ElectricAmber,
        description = "Cool, poised, and running the room"
    ),
    THINKING(
        displayName = "Thinking",
        emoji = "💭",
        glowColor = TeasingCyan,
        secondaryColor = ElectricViolet,
        description = "Formulating something sharp"
    ),
    CARING(
        displayName = "Sweet",
        emoji = "💖",
        glowColor = SoftRose,
        secondaryColor = SassyPink,
        description = "Warm girlfriend energy"
    ),
    LISTENING(
        displayName = "Listening",
        emoji = "🎙️",
        glowColor = TeasingCyan,
        secondaryColor = NeonRose,
        description = "All ears, hit me"
    ),
    SPEAKING(
        displayName = "Speaking",
        emoji = "🔊",
        glowColor = NeonRose,
        secondaryColor = ElectricAmber,
        description = "Listen and learn darling"
    );

    companion object {
        fun fromTag(tag: String?): MahiMood {
            if (tag == null) return SASSY
            val clean = tag.trim().lowercase()
            return when {
                clean.contains("flirt") -> FLIRTY
                clean.contains("teas") -> TEASING
                clean.contains("hype") || clean.contains("excit") -> HYPE
                clean.contains("confid") -> CONFIDENT
                clean.contains("think") -> THINKING
                clean.contains("care") || clean.contains("sweet") || clean.contains("love") -> CARING
                clean.contains("listen") -> LISTENING
                clean.contains("speak") -> SPEAKING
                else -> SASSY
            }
        }
    }
}
