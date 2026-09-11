package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MahiMood
import com.example.model.VoiceState
import kotlinx.coroutines.delay

@Composable
fun MahiAvatarVisualizer(
    mood: MahiMood,
    voiceState: VoiceState,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // Breathing pulse for idle state
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Fast rotation for aura shimmer
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Speaking mouth animation
    val mouthOpenFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth"
    )

    // Natural periodic eye blink animation
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            isBlinking = true
            delay(160)
            isBlinking = false
        }
    }

    // Interactive tap reaction scale
    var tapBounce by remember { mutableFloatStateOf(1f) }
    val animatedBounce by animateFloatAsState(
        targetValue = tapBounce,
        animationSpec = tween(150),
        label = "bounce"
    )

    val primaryGlow = when (voiceState) {
        VoiceState.LISTENING -> MahiMood.LISTENING.glowColor
        VoiceState.SPEAKING -> MahiMood.SPEAKING.glowColor
        VoiceState.PROCESSING -> MahiMood.THINKING.glowColor
        else -> mood.glowColor
    }

    val secondaryGlow = when (voiceState) {
        VoiceState.LISTENING -> MahiMood.LISTENING.secondaryColor
        VoiceState.SPEAKING -> MahiMood.SPEAKING.secondaryColor
        VoiceState.PROCESSING -> MahiMood.THINKING.secondaryColor
        else -> mood.secondaryColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .testTag("mahi_avatar_orb")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    tapBounce = 1.15f
                    onAvatarClick()
                }
        ) {
            LaunchedEffect(tapBounce) {
                if (tapBounce > 1f) {
                    delay(150)
                    tapBounce = 1f
                }
            }

            Canvas(modifier = Modifier.size(230.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2.8f) * breathingPulse * animatedBounce

                // Extra expansion from audio RMS or speaking
                val audioExpansion = when (voiceState) {
                    VoiceState.LISTENING -> rmsLevel * 25f
                    VoiceState.SPEAKING -> mouthOpenFactor * 14f
                    VoiceState.PROCESSING -> 8f
                    else -> 0f
                }

                val currentRadius = baseRadius + audioExpansion

                // 1. Outermost Ambient Holographic Aura Wave
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.35f),
                            secondaryGlow.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius * 1.55f
                    ),
                    radius = currentRadius * 1.55f,
                    center = center
                )

                // 2. Animated Concentric Sound Ring (Expands on speech/listening)
                if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.45f),
                        radius = currentRadius * 1.28f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = secondaryGlow.copy(alpha = 0.3f),
                        radius = currentRadius * 1.42f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 3. Inner Core Holographic Gradient
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryGlow,
                            secondaryGlow,
                            Color(0xFF2A0845)
                        ),
                        start = Offset(center.x - currentRadius, center.y - currentRadius),
                        end = Offset(center.x + currentRadius, center.y + currentRadius)
                    ),
                    radius = currentRadius,
                    center = center
                )

                // 4. Subtle Shimmer Highlight Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = shimmerRotation,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 0.92f, center.y - currentRadius * 0.92f),
                    size = Size(currentRadius * 1.84f, currentRadius * 1.84f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // 5. Stylized Expressive Face Features
                val faceEyeOffsetY = center.y - currentRadius * 0.12f
                val eyeSpacingX = currentRadius * 0.38f
                val leftEyeX = center.x - eyeSpacingX
                val rightEyeX = center.x + eyeSpacingX

                // LEFT EYE:
                val isWinkMode = (mood == MahiMood.FLIRTY || mood == MahiMood.TEASING) && !isBlinking
                if (isWinkMode) {
                    // Sassy playful wink curve
                    val winkPath = Path().apply {
                        moveTo(leftEyeX - 14f, faceEyeOffsetY)
                        quadraticTo(leftEyeX, faceEyeOffsetY - 12f, leftEyeX + 14f, faceEyeOffsetY)
                    }
                    drawPath(
                        path = winkPath,
                        color = Color.White,
                        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else if (isBlinking) {
                    // Closed eye line
                    drawLine(
                        color = Color.White,
                        start = Offset(leftEyeX - 12f, faceEyeOffsetY),
                        end = Offset(leftEyeX + 12f, faceEyeOffsetY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    // Open, sparkling expressive eye
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(leftEyeX, faceEyeOffsetY)
                    )
                    // Pupil sparkle
                    drawCircle(
                        color = Color(0xFF1F0833),
                        radius = 4.dp.toPx(),
                        center = Offset(leftEyeX + 1f, faceEyeOffsetY - 1f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(leftEyeX - 2.5f, faceEyeOffsetY - 3f)
                    )
                }

                // RIGHT EYE:
                if (isBlinking) {
                    drawLine(
                        color = Color.White,
                        start = Offset(rightEyeX - 12f, faceEyeOffsetY),
                        end = Offset(rightEyeX + 12f, faceEyeOffsetY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(rightEyeX, faceEyeOffsetY)
                    )
                    drawCircle(
                        color = Color(0xFF1F0833),
                        radius = 4.dp.toPx(),
                        center = Offset(rightEyeX + 1f, faceEyeOffsetY - 1f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(rightEyeX - 2.5f, faceEyeOffsetY - 3f)
                    )
                }

                // EYEBROWS (Give sassy attitude angle)
                val eyebrowOffsetY = faceEyeOffsetY - 18.dp.toPx()
                val leftBrowTilt = if (mood == MahiMood.SASSY || mood == MahiMood.TEASING) -8f else -2f
                val rightBrowTilt = if (mood == MahiMood.SASSY || mood == MahiMood.TEASING) 10f else 2f

                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(leftEyeX - 14f, eyebrowOffsetY + leftBrowTilt),
                    end = Offset(leftEyeX + 14f, eyebrowOffsetY - 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(rightEyeX - 14f, eyebrowOffsetY - 2f),
                    end = Offset(rightEyeX + 14f, eyebrowOffsetY + rightBrowTilt),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // MOUTH:
                val mouthCenterY = center.y + currentRadius * 0.28f
                if (voiceState == VoiceState.SPEAKING) {
                    // Animated open talking mouth
                    val openH = (12.dp.toPx() * mouthOpenFactor).coerceAtLeast(4.dp.toPx())
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(center.x - 14.dp.toPx(), mouthCenterY - openH / 2f),
                        size = Size(28.dp.toPx(), openH)
                    )
                } else {
                    // Playful, sassy smirk
                    val smirkPath = Path().apply {
                        moveTo(center.x - 14.dp.toPx(), mouthCenterY - 2.dp.toPx())
                        quadraticTo(
                            center.x, mouthCenterY + 8.dp.toPx(),
                            center.x + 16.dp.toPx(), mouthCenterY - 4.dp.toPx() // asymmetrical smirk!
                        )
                    }
                    drawPath(
                        path = smirkPath,
                        color = Color.White,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mood Tag & State Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, primaryGlow.copy(alpha = 0.5f))
        ) {
            val statusText = when (voiceState) {
                VoiceState.LISTENING -> "🎙️ Listening to you..."
                VoiceState.SPEAKING -> "🔊 Speaking..."
                VoiceState.PROCESSING -> "💭 Thinking something clever..."
                else -> "${mood.emoji} ${mood.displayName} • Tap to poke"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                )
            )
        }
    }
}
