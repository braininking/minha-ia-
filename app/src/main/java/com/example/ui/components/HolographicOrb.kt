package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicOrb(
    state: AssistantState,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    // Slow continuous rotation for outer rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation
    val reverseRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Breathing pulse for core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 600
                    AssistantState.INTERPRETING, AssistantState.PLANNING -> 1000
                    AssistantState.EXECUTING -> 800
                    AssistantState.VERIFYING -> 500
                    AssistantState.ERROR -> 400
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Radar expansion wave
    val waveExpansion by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.LISTENING || state == AssistantState.PLANNING) 1400 else 2800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val (primaryColor, glowColor) = when (state) {
        AssistantState.ERROR -> Pair(ErrorRed, ErrorRed.copy(alpha = 0.35f))
        AssistantState.PAUSED -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.35f))
        AssistantState.COMPLETED -> Pair(EmeraldGreen, EmeraldGreen.copy(alpha = 0.45f))
        else -> Pair(NeonGreen, NeonGreen.copy(alpha = 0.40f))
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("holographic_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.72f

            // 1. Expanding radar pulse wave
            val waveRadius = baseRadius * waveExpansion
            val waveAlpha = ((1.35f - waveExpansion) / 0.75f).coerceIn(0f, 0.6f)
            drawCircle(
                color = primaryColor.copy(alpha = waveAlpha),
                radius = waveRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Extra audio reaction ring when listening
            if (state == AssistantState.LISTENING && audioLevel > 0.5f) {
                val audioBoost = (audioLevel / 10f) * 24.dp.toPx()
                drawCircle(
                    color = primaryColor.copy(alpha = 0.5f),
                    radius = baseRadius + audioBoost,
                    center = center,
                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f)))
                )
            }

            // 3. Ambient outer glow gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        glowColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.3f,
                center = center
            )

            // 4. Outer Rotating Geometric Ring with dashes
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.65f),
                    radius = baseRadius * 1.05f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 18f, 8f, 18f))
                    )
                )

                // Small orbital cyber nodes
                for (i in 0 until 4) {
                    val angleRad = Math.toRadians((i * 90.0))
                    val nodeX = center.x + (baseRadius * 1.05f * cos(angleRad)).toFloat()
                    val nodeY = center.y + (baseRadius * 1.05f * sin(angleRad)).toFloat()
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // 5. Inner Counter-Rotating Concentric Ring
            rotate(reverseRotationAngle, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = baseRadius * 0.85f,
                    center = center,
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                    )
                )
            }

            // 6. Glowing Sci-Fi Core
            val coreRadius = baseRadius * 0.65f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        glowColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 7. Center Quantum Neural Dot
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx() * pulseScale,
                center = center
            )
        }
    }
}
