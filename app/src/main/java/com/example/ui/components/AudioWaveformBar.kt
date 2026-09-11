package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.VoiceState

@Composable
fun AudioWaveformBar(
    voiceState: VoiceState,
    rmsLevel: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 42.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    // Generate staggering phase animations
    val animFactor1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val animFactor2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    val animFactor3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until barCount) {
            val baseScale = when (i % 3) {
                0 -> animFactor1
                1 -> animFactor2
                else -> animFactor3
            }

            // Curve height towards center
            val centerWeight = 1f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)) * 0.5f

            val heightMultiplier = when (voiceState) {
                VoiceState.SPEAKING -> (baseScale * centerWeight).coerceIn(0.15f, 1f)
                VoiceState.LISTENING -> ((rmsLevel * 1.5f + baseScale * 0.3f) * centerWeight).coerceIn(0.12f, 1f)
                VoiceState.PROCESSING -> ((baseScale * 0.5f) * centerWeight).coerceIn(0.1f, 0.6f)
                else -> 0.1f
            }

            val barHeight = maxHeight * heightMultiplier

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(primaryColor, secondaryColor)
                        )
                    )
            )
        }
    }
}
